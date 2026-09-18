"""Prueba el modelo `.tflite` con fotos REALES de calle y ayuda a elegir el umbral de confianza.

Por qué existe: las métricas del cuaderno se miden con el mismo tipo de fotos con que se entrenó (TrashNet +
Kaggle) y sobreestiman el desempeño en la calle. Este script mide el modelo con TUS fotos, sin Colab, y con
ellas calcula qué umbral de confianza conviene, en vez de suponerlo.

Estructura de la carpeta de fotos (una subcarpeta por clase, con los nombres exactos de labels.txt):

    prueba_calle/
        plastico/  carton/  vidrio/  organico/   <- fotos de esa clase
        fuera_de_clase/                          <- escombros, muros, veredas, suelo... (opcional)

Uso (desde esta carpeta):

    python probar_modelo.py ../prueba_calle --modelo ../salida_colab/clasificador_residuos.tflite
    python probar_modelo.py ../prueba_calle --umbral 0.70 --tope-fuera 0.10

Reglas: estas fotos son SOLO para probar. Si se usan para entrenar, la prueba deja de valer. Y NO se suben a git
(pueden traer rostros o placas; `.gitignore` excluye `ml/prueba_calle/`). Con pocas fotos por clase los
resultados son inciertos: por eso se muestran intervalos de confianza y no solo porcentajes.

Requiere TensorFlow (se usa su misma decodificación y redimensionado que en el entrenamiento, para que la prueba
sea fiel), numpy y, opcionalmente, ai-edge-litert.
"""

import argparse
import collections
import datetime
import json
import math
import os
import sys
from pathlib import Path

import numpy as np

EXTENSIONES = {".jpg", ".jpeg", ".png", ".bmp"}
IMG_SIZE = 224
FUERA = "fuera_de_clase"
UMBRALES = [round(0.50 + 0.05 * i, 2) for i in range(10)]  # 0.50 ... 0.95
AQUI = Path(__file__).parent


def intervalo_wilson(exitos, n, z=1.96):
    """Intervalo de confianza del 95 % para una proporción. Con pocas fotos es MUY ancho: eso es lo honesto."""
    if n == 0:
        return None
    p = exitos / n
    denominador = 1 + z * z / n
    centro = (p + z * z / (2 * n)) / denominador
    margen = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denominador
    return [round(max(0.0, centro - margen), 3), round(min(1.0, centro + margen), 3)]


def pct(valor):
    return "—" if valor is None else f"{valor:.0%}"


def leer_etiquetas(ruta_labels):
    return [linea.strip() for linea in Path(ruta_labels).read_text(encoding="utf-8").splitlines() if linea.strip()]


def listar_fotos(carpeta):
    return sorted(p for p in carpeta.rglob("*") if p.is_file() and p.suffix.lower() in EXTENSIONES)


def main():
    parser = argparse.ArgumentParser(description="Prueba el modelo con fotos reales de calle.")
    parser.add_argument("carpeta", type=Path, help="carpeta con una subcarpeta por clase (y opcionalmente fuera_de_clase)")
    parser.add_argument("--modelo", type=Path, default=AQUI.parent / "salida_colab" / "clasificador_residuos.tflite")
    parser.add_argument("--labels", type=Path, default=None, help="por defecto, labels.txt junto al modelo")
    parser.add_argument("--umbral", type=float, default=0.70, help="umbral que se evalúa en el informe (0.70 por defecto)")
    parser.add_argument("--precision-objetivo", type=float, default=0.85)
    parser.add_argument("--tope-fuera", type=float, default=None, help="tope aceptable de fotos fuera de clase sobre el umbral (fíjalo ANTES de ver el resultado)")
    parser.add_argument("--salida", type=Path, default=None, help="JSON de resultados (por defecto ml/resultados/prueba_calle_<fecha>.json)")
    args = parser.parse_args()

    if not args.carpeta.is_dir():
        sys.exit(f"No existe la carpeta de fotos: {args.carpeta}")
    if not args.modelo.is_file():
        sys.exit(f"No existe el modelo: {args.modelo}")
    clases = leer_etiquetas(args.labels or args.modelo.parent / "labels.txt")

    os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")
    import tensorflow as tf

    try:
        from ai_edge_litert.interpreter import Interpreter
    except ImportError:
        Interpreter = getattr(tf.lite, "Interpreter", None)
        if Interpreter is None:
            sys.exit("Falta un intérprete TFLite: pip install ai-edge-litert")
    interprete = Interpreter(model_path=str(args.modelo))
    interprete.allocate_tensors()
    entrada, salida = interprete.get_input_details()[0], interprete.get_output_details()[0]
    if list(salida["shape"]) != [1, len(clases)]:
        sys.exit(f"El modelo devuelve {list(salida['shape'])} pero labels.txt tiene {len(clases)} clases: no corresponden.")

    def predecir(ruta):
        datos = tf.io.read_file(str(ruta))
        imagen = tf.image.decode_image(datos, channels=3, expand_animations=False)
        imagen = tf.image.resize(imagen, (IMG_SIZE, IMG_SIZE))  # igual que el cuaderno: float32, píxeles 0..255
        interprete.set_tensor(entrada["index"], imagen.numpy().astype(np.float32)[None])
        interprete.invoke()
        probabilidades = interprete.get_tensor(salida["index"])[0]
        return int(probabilidades.argmax()), float(probabilidades.max())

    registros, ilegibles, sin_fotos = [], [], []
    for real in clases + [FUERA]:
        carpeta = args.carpeta / real
        fotos = listar_fotos(carpeta) if carpeta.is_dir() else []
        if not fotos and real != FUERA:
            sin_fotos.append(real)
        for foto in fotos:
            try:
                indice, confianza = predecir(foto)
            except tf.errors.InvalidArgumentError:
                ilegibles.append(f"{real}/{foto.name}")
                continue
            registros.append({"real": real, "pred": clases[indice], "conf": confianza, "archivo": f"{real}/{foto.name}"})

    de_clase = [r for r in registros if r["real"] != FUERA]
    fuera = [r for r in registros if r["real"] == FUERA]
    if not de_clase:
        sys.exit(f"No hay fotos de ninguna clase en {args.carpeta} (esperaba subcarpetas {clases}).")

    # ---- Por clase, con el umbral elegido ------------------------------------------------------
    umbral = args.umbral
    por_clase = {}
    for clase in clases:
        fotos = [r for r in de_clase if r["real"] == clase]
        n = len(fotos)
        correctas = sum(r["pred"] == clase for r in fotos)
        por_clase[clase] = {
            "n": n,
            "aciertos": correctas,
            "acierto": None if n == 0 else round(correctas / n, 3),
            "acierto_ic95": intervalo_wilson(correctas, n),
            "sugerencia_correcta": sum(r["pred"] == clase and r["conf"] >= umbral for r in fotos),
            "sugerencia_equivocada": sum(r["pred"] != clase and r["conf"] >= umbral for r in fotos),
            "sin_sugerencia_mixto": sum(r["conf"] < umbral for r in fotos),
        }

    matriz = {real: dict(collections.Counter(r["pred"] for r in de_clase if r["real"] == real)) for real in clases}
    total_correctas = sum(r["pred"] == r["real"] for r in de_clase)

    # ---- Barrido de umbrales ---------------------------------------------------------------------
    barrido = []
    for u in UMBRALES:
        cubiertas = [r for r in de_clase if r["conf"] >= u]
        aciertos = sum(r["pred"] == r["real"] for r in cubiertas)
        fila = {
            "umbral": u,
            "cobertura": round(len(cubiertas) / len(de_clase), 3),
            "precision": None if not cubiertas else round(aciertos / len(cubiertas), 3),
            "fuera_de_clase_sobre_umbral": None if not fuera else round(sum(r["conf"] >= u for r in fuera) / len(fuera), 3),
        }
        barrido.append(fila)

    def cumple(fila):
        if fila["precision"] is None or fila["precision"] < args.precision_objetivo:
            return False
        if args.tope_fuera is not None and fuera:
            return fila["fuera_de_clase_sobre_umbral"] <= args.tope_fuera
        return True

    recomendado = next((f["umbral"] for f in barrido if cumple(f)), None)

    fuera_res = None
    if fuera:
        sobre = [r for r in fuera if r["conf"] >= umbral]
        fuera_res = {
            "n": len(fuera),
            "sobre_umbral": len(sobre),
            "tasa": round(len(sobre) / len(fuera), 3),
            "tasa_ic95": intervalo_wilson(len(sobre), len(fuera)),
            "por_clase_predicha": dict(collections.Counter(r["pred"] for r in sobre)),
            "peores_casos": [
                {"archivo": r["archivo"], "clase": r["pred"], "confianza": round(r["conf"], 3)}
                for r in sorted(fuera, key=lambda r: -r["conf"])[:10]
            ],
        }

    resultado = {
        "fecha": datetime.datetime.now().isoformat(timespec="seconds"),
        "modelo": str(args.modelo.name),
        "umbral_evaluado": umbral,
        "n_fotos_clases": len(de_clase),
        "n_fuera_de_clase": len(fuera),
        "ilegibles": ilegibles,
        "clases_sin_fotos": sin_fotos,
        "acierto_global": round(total_correctas / len(de_clase), 3),
        "acierto_global_ic95": intervalo_wilson(total_correctas, len(de_clase)),
        "por_clase": por_clase,
        "matriz_confusion": matriz,
        "fuera_de_clase": fuera_res,
        "barrido_umbral": barrido,
        "precision_objetivo": args.precision_objetivo,
        "tope_fuera": args.tope_fuera,
        "umbral_recomendado": recomendado,
        "errores_con_seguridad": [
            {"archivo": r["archivo"], "real": r["real"], "pred": r["pred"], "confianza": round(r["conf"], 3)}
            for r in sorted(de_clase, key=lambda r: -r["conf"])
            if r["pred"] != r["real"] and r["conf"] >= umbral
        ][:15],
        "advertencia": "Prueba con fotos del equipo; no es el indicador de la tesis (coincidencia IA-observador, medida en campo).",
    }

    # ---- Informe legible --------------------------------------------------------------------------
    print(f"\nModelo: {args.modelo.name} | fotos de clase: {len(de_clase)} | fuera de clase: {len(fuera)}")
    if sin_fotos:
        print(f"AVISO: sin fotos para {sin_fotos}: no se puede medir esas clases.")
    if ilegibles:
        print(f"AVISO: {len(ilegibles)} archivo(s) ilegible(s), se omiten: {ilegibles[:5]}")
    ic = resultado["acierto_global_ic95"]
    print(f"\nAcierto global (sin umbral): {pct(resultado['acierto_global'])}  [95 %: {pct(ic[0])} a {pct(ic[1])}]")
    print(f"\nPor clase, con umbral {umbral}:")
    print(f"  {'clase':<10}{'n':>4}{'acierta':>9}{'IC 95 %':>16}{'sug. ok':>9}{'sug. mal':>10}{'mixto':>7}")
    for clase, r in por_clase.items():
        ic = r["acierto_ic95"]
        rango = "—" if ic is None else f"{pct(ic[0])}–{pct(ic[1])}"
        print(f"  {clase:<10}{r['n']:>4}{pct(r['acierto']):>9}{rango:>16}{r['sugerencia_correcta']:>9}{r['sugerencia_equivocada']:>10}{r['sin_sugerencia_mixto']:>7}")
    print("\nMatriz de confusión (fila = real, columna = predicho):")
    print("  " + " " * 10 + "".join(f"{c:>10}" for c in clases))
    for real, fila in matriz.items():
        print(f"  {real:<10}" + "".join(f"{fila.get(c, 0):>10}" for c in clases))
    if fuera_res:
        ic = fuera_res["tasa_ic95"]
        print(f"\nFuera de clase: {fuera_res['sobre_umbral']} de {fuera_res['n']} ({pct(fuera_res['tasa'])}, 95 %: {pct(ic[0])} a {pct(ic[1])}) "
              f"superan el umbral {umbral}: la app sugeriría una categoría equivocada con seguridad. Por clase: {fuera_res['por_clase_predicha']}")
    else:
        print("\nAVISO: no hay fotos en fuera_de_clase: NO se midió el riesgo con escombros, muros o suelo.")
    print(f"\n  {'umbral':>7}{'cobertura':>11}{'precisión':>11}{'fuera>umbral':>14}")
    for f in barrido:
        marca = "  <-- evaluado" if f["umbral"] == round(umbral, 2) else ""
        print(f"  {f['umbral']:>7.2f}{pct(f['cobertura']):>11}{pct(f['precision']):>11}{pct(f['fuera_de_clase_sobre_umbral']):>14}{marca}")
    criterio = f"precisión ≥ {args.precision_objetivo:.0%}" + (f" y fuera de clase ≤ {args.tope_fuera:.0%}" if args.tope_fuera is not None and fuera else "")
    print(f"\nUmbral mínimo que cumple ({criterio}): {recomendado if recomendado is not None else 'NINGUNO: el modelo no es confiable con estas fotos'}")
    if resultado["errores_con_seguridad"]:
        print("\nErrores con seguridad (los más graves):")
        for e in resultado["errores_con_seguridad"][:5]:
            print(f"  {e['archivo']}: real {e['real']}, dijo {e['pred']} con {e['confianza']:.0%}")
    print(f"\nRecuerda: {resultado['advertencia']}")

    destino = args.salida or AQUI.parent / "resultados" / f"prueba_calle_{datetime.datetime.now():%Y%m%d-%H%M%S}.json"
    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_text(json.dumps(resultado, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Resultados guardados en {destino}")


if __name__ == "__main__":
    main()
