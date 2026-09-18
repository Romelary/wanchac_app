"""Prueba de humo del cuaderno de entrenamiento (sin GPU, sin internet, sin Google Drive).

Por qué existe: el cuaderno se ejecuta una sola vez en Colab, pero un error a mitad de camino
(p. ej. en la exportación a TFLite, al final) costaría minutos u horas de entrenamiento. Esta
prueba ejecuta el MISMO archivo `clasificador_residuos.py` con `SMOKE_TEST=1`, que sustituye
los datos reales por imágenes sintéticas y reduce el entrenamiento a una época, y comprueba:

  1. que el cuaderno corre de principio a fin y produce todos los archivos;
  2. que `labels.txt` y `metadata.json` respetan el contrato con la app, incluido el ORDEN de las
     clases, que se compara contra el enum `CategoriaResiduo` de Kotlin;
  3. que el `.tflite` tiene la entrada/salida que la app espera y produce probabilidades válidas;
  4. que el `.ipynb` generado es válido y está al día respecto al `.py`;
  5. que `probar_modelo.py` (prueba del modelo con fotos reales de calle) funciona con el `.tflite` recién generado.

Lo que NO comprueba: la calidad del modelo (los datos son de ruido). Eso solo se ve en Colab.

Uso (desde esta carpeta, con TensorFlow y nbformat instalados):  python probar_notebook.py
"""

import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

AQUI = Path(__file__).parent
FUENTE = AQUI / "clasificador_residuos.py"
ENUM_KOTLIN = AQUI.parent.parent / "app/src/main/java/com/wanchaqlimpio/app/domain/model/CategoriaResiduo.kt"

ARCHIVOS_ESPERADOS = [
    "clasificador_residuos.tflite",
    "labels.txt",
    "metadata.json",
    "metricas.json",
    "curvas_entrenamiento.png",
    "matriz_confusion.png",
]


class PruebaDeHumoDelCuaderno(unittest.TestCase):
    """Se ejecuta el cuaderno una sola vez (tarda un par de minutos) y varias pruebas revisan su salida."""

    @classmethod
    def setUpClass(cls):
        cls._tmp = tempfile.TemporaryDirectory()
        cls.raiz = Path(cls._tmp.name)
        cls.entorno = {
            **os.environ,
            "SMOKE_TEST": "1",
            "SMOKE_DIR": str(cls.raiz),
            "PYTHONUTF8": "1",  # Windows: evita UnicodeEncodeError con tildes en la consola
            "TF_CPP_MIN_LOG_LEVEL": "2",
        }
        cls.proceso = subprocess.run(
            [sys.executable, str(FUENTE)], env=cls.entorno, capture_output=True, text=True, encoding="utf-8"
        )
        cls.salida = cls.raiz / "salida"

    @classmethod
    def tearDownClass(cls):
        cls._tmp.cleanup()

    def test_el_cuaderno_corre_de_principio_a_fin(self):
        self.assertEqual(
            self.proceso.returncode,
            0,
            f"El cuaderno falló.\n--- stdout (final) ---\n{self.proceso.stdout[-2500:]}"
            f"\n--- stderr (final) ---\n{self.proceso.stderr[-2500:]}",
        )

    def test_genera_todos_los_archivos(self):
        for nombre in ARCHIVOS_ESPERADOS:
            archivo = self.salida / nombre
            self.assertTrue(archivo.is_file() and archivo.stat().st_size > 0, f"Falta o está vacío: {nombre}")

    def test_labels_coincide_con_el_enum_de_kotlin(self):
        """El contrato más frágil: si el orden difiere, la app etiquetaría mal TODAS las predicciones."""
        cuerpo = ENUM_KOTLIN.read_text(encoding="utf-8").split("enum class CategoriaResiduo {")[1].split("}")[0]
        valores = re.findall(r"^\s*([A-Z_]+),?\s*$", cuerpo, flags=re.MULTILINE)
        # El modelo NO predice ESCOMBROS (decisión 2026-09-18) ni MIXTO (el "no sé" al que la app recurre con poca
        # confianza): ambas son de elección manual. El resto, en el mismo orden que el enum, es la salida del modelo.
        manuales = {"ESCOMBROS", "MIXTO"}
        clases_del_modelo = [v.lower() for v in valores if v not in manuales]
        labels = (self.salida / "labels.txt").read_text(encoding="utf-8").split()
        self.assertEqual(labels, clases_del_modelo)
        meta = json.loads((self.salida / "metadata.json").read_text(encoding="utf-8"))
        self.assertEqual(sorted(meta["categorias_manuales"]), sorted(v.lower() for v in manuales))
        self.assertTrue(set(labels).isdisjoint(meta["categorias_manuales"]), "una categoría no puede ser a la vez del modelo y manual")

    def test_la_siembra_de_trashnet_mapea_las_carpetas_y_descarta_la_basura(self):
        """Zip falso: plastic 12, cardboard 8, paper 8, glass 12, metal 5, trash 5 (+ basura de __MACOSX).
        Esperado: metal/trash y __MACOSX no entran; cardboard+paper se funden en `carton` con tope 15."""
        conteo = json.loads((self.salida / "metadata.json").read_text(encoding="utf-8"))["dataset"]["conteo_por_clase"]
        de_trashnet = {clase: datos["de_trashnet"] for clase, datos in conteo.items()}
        total = {clase: datos["total"] for clase, datos in conteo.items()}
        self.assertEqual(de_trashnet, {"plastico": 12, "carton": 15, "vidrio": 12, "organico": 0})
        self.assertEqual(total, {"plastico": 12, "carton": 15, "vidrio": 12, "organico": 15})

    def test_un_archivo_corrupto_se_descarta_y_se_informa_y_un_formato_no_soportado_se_avisa(self):
        """El dataset trae `corrupta.jpg` (bytes que no son imagen) y `foto.heic` (formato que TensorFlow no lee).
        Ninguno debe contar ni romper el entrenamiento, y ninguno debe ignorarse EN SILENCIO."""
        meta = json.loads((self.salida / "metadata.json").read_text(encoding="utf-8"))
        self.assertEqual(meta["dataset"]["archivos_descartados"], ["organico/corrupta.jpg"])
        self.assertIn("foto.heic", self.proceso.stdout, "el formato no soportado debe avisarse por consola")

    def test_la_evaluacion_fuera_de_clase_se_ejecuta_y_se_registra(self):
        """Sin escombros como clase, lo que protege a la app es que la confianza quede bajo el umbral. La celda 7b
        lo mide con fotos que no son de ninguna clase. En la prueba hay 6 (ruido); debe quedar registrado en
        metricas.json con una tasa válida y con los peores casos listados."""
        fuera = json.loads((self.salida / "metricas.json").read_text(encoding="utf-8"))["fuera_de_clase"]
        self.assertIsNotNone(fuera, "la evaluación fuera de clase no se ejecutó")
        self.assertEqual(fuera["n"], 6)
        self.assertTrue(0.0 <= fuera["tasa"] <= 1.0)
        self.assertEqual(fuera["con_confianza_sobre_umbral"], round(fuera["tasa"] * fuera["n"]))
        self.assertEqual(len(fuera["peores_casos"]), 6)
        confianzas = [caso["confianza"] for caso in fuera["peores_casos"]]
        self.assertEqual(confianzas, sorted(confianzas, reverse=True), "los peores casos deben ir del más al menos seguro")

    def test_probar_modelo_mide_fotos_reales_con_el_tflite_generado(self):
        """`probar_modelo.py` es lo que permitirá decidir el umbral con fotos de calle. Se ejecuta contra el
        `.tflite` recién generado con fotos de ruido (3 por clase + 3 fuera de clase + 1 corrupta) y debe
        producir un informe coherente: conteos, un archivo ilegible informado y no silenciado, y el barrido de
        umbrales completo. No evalúa la calidad (son ruido), solo que la herramienta funciona."""
        import numpy as np
        from PIL import Image

        carpeta = self.raiz / "prueba_calle_falsa"
        rng = np.random.default_rng(7)
        for clase in ["plastico", "carton", "vidrio", "organico", "fuera_de_clase"]:
            (carpeta / clase).mkdir(parents=True, exist_ok=True)
            for n in range(3):
                Image.fromarray(rng.integers(0, 255, (64, 64, 3), dtype="uint8")).save(carpeta / clase / f"{n}.jpg")
        (carpeta / "organico" / "corrupta.jpg").write_bytes(b"esto no es una imagen")

        resultado_json = self.raiz / "prueba_calle.json"
        proceso = subprocess.run(
            [
                sys.executable,
                str(AQUI / "probar_modelo.py"),
                str(carpeta),
                "--modelo",
                str(self.salida / "clasificador_residuos.tflite"),
                "--umbral",
                "0.70",
                "--tope-fuera",
                "0.10",
                "--salida",
                str(resultado_json),
            ],
            env=self.entorno,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
        self.assertEqual(proceso.returncode, 0, f"probar_modelo.py falló:\n{proceso.stdout[-1500:]}\n{proceso.stderr[-1500:]}")
        r = json.loads(resultado_json.read_text(encoding="utf-8"))
        self.assertEqual(r["n_fotos_clases"], 12)
        self.assertEqual(r["n_fuera_de_clase"], 3)
        self.assertEqual(r["ilegibles"], ["organico/corrupta.jpg"], "el archivo corrupto debe informarse, no ignorarse")
        self.assertEqual(sum(c["n"] for c in r["por_clase"].values()), 12)
        self.assertEqual([f["umbral"] for f in r["barrido_umbral"]], [0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95])
        # cobertura y fuera-de-clase sobre umbral nunca aumentan al subir el umbral
        cobertura = [f["cobertura"] for f in r["barrido_umbral"]]
        self.assertEqual(cobertura, sorted(cobertura, reverse=True))
        self.assertTrue(0.0 <= r["acierto_global"] <= 1.0)
        self.assertIsNotNone(r["fuera_de_clase"])
        self.assertIn("advertencia", r)

    def test_probar_modelo_falla_con_claridad_si_la_carpeta_no_existe(self):
        proceso = subprocess.run(
            [sys.executable, str(AQUI / "probar_modelo.py"), str(self.raiz / "no_existe")],
            env=self.entorno,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
        self.assertNotEqual(proceso.returncode, 0)
        self.assertIn("No existe la carpeta de fotos", proceso.stderr)

    def test_los_json_son_json_estandar(self):
        """Python escribe NaN/Infinity sin quejarse, pero no son JSON válido: un lector estricto (p. ej. el
        de la app en Kotlin) fallaría. La tabla de umbrales puede tener precisiones sin definir."""

        def rechazar(constante):
            raise ValueError(f"JSON no estándar: {constante}")

        for nombre in ("metadata.json", "metricas.json"):
            json.loads((self.salida / nombre).read_text(encoding="utf-8"), parse_constant=rechazar)

    def test_metadata_es_coherente(self):
        meta = json.loads((self.salida / "metadata.json").read_text(encoding="utf-8"))
        labels = (self.salida / "labels.txt").read_text(encoding="utf-8").split()
        self.assertEqual(meta["clases"], labels)
        self.assertEqual(meta["entrada"]["forma"], [1, 224, 224, 3])
        self.assertEqual(meta["salida"]["forma"], [1, len(labels)])
        self.assertTrue(0.5 <= meta["umbral_confianza_sugerido"] < 1.0, "el umbral no debe bajar del piso de 0.50")
        self.assertIsInstance(meta["umbral_alcanza_precision_objetivo"], bool)
        sha = hashlib.sha256((self.salida / "clasificador_residuos.tflite").read_bytes()).hexdigest()
        self.assertEqual(meta["sha256_tflite"], sha, "metadata.json no describe el .tflite guardado")

    def test_el_tflite_tiene_la_entrada_y_salida_que_la_app_espera(self):
        import numpy as np

        try:
            from ai_edge_litert.interpreter import Interpreter  # el intérprete vigente (LiteRT)
        except ImportError:
            import tensorflow as tf

            Interpreter = tf.lite.Interpreter  # en desuso, pero es lo que hay si LiteRT no está instalado

        interprete = Interpreter(model_path=str(self.salida / "clasificador_residuos.tflite"))
        interprete.allocate_tensors()
        entrada = interprete.get_input_details()[0]
        salida = interprete.get_output_details()[0]
        self.assertEqual(list(entrada["shape"]), [1, 224, 224, 3])
        self.assertEqual(entrada["dtype"], np.float32)
        self.assertEqual(list(salida["shape"]), [1, 4])  # plastico, carton, vidrio, organico

        # Un gradiente de 0..255: el rango de píxeles que la app entrega (la normalización va dentro).
        imagen = np.linspace(0, 255, 224 * 224 * 3, dtype=np.float32).reshape(1, 224, 224, 3)
        interprete.set_tensor(entrada["index"], imagen)
        interprete.invoke()
        probabilidades = interprete.get_tensor(salida["index"])[0]
        self.assertAlmostEqual(float(probabilidades.sum()), 1.0, places=3)
        self.assertTrue(((probabilidades >= 0) & (probabilidades <= 1)).all())


class PruebaDelNotebook(unittest.TestCase):
    """El .ipynb que se sube a Colab debe ser válido y no divergir del .py que aquí se prueba."""

    def test_el_notebook_generado_es_valido_y_compila(self):
        import nbformat

        sys.path.insert(0, str(AQUI))
        import construir_notebook

        celdas = construir_notebook.leer_celdas(FUENTE.read_text(encoding="utf-8"))
        self.assertGreater(len(celdas), 10)
        cuaderno = nbformat.v4.new_notebook(cells=celdas)
        nbformat.validate(cuaderno)
        for i, celda in enumerate(celdas):
            if celda.cell_type == "code":
                compile(celda.source, f"celda {i}", "exec")  # SyntaxError si una celda quedó mal cortada

    def test_el_ipynb_versionado_esta_al_dia(self):
        import nbformat

        sys.path.insert(0, str(AQUI))
        import construir_notebook

        ipynb = AQUI / "clasificador_residuos.ipynb"
        if not ipynb.exists():
            self.skipTest("Aún no se generó el .ipynb: ejecuta construir_notebook.py")
        esperado = [c.source for c in construir_notebook.leer_celdas(FUENTE.read_text(encoding="utf-8"))]
        real = [c.source for c in nbformat.read(ipynb, as_version=4).cells]
        self.assertEqual(real, esperado, "El .ipynb está desactualizado: ejecuta construir_notebook.py")


if __name__ == "__main__":
    unittest.main(verbosity=2)
