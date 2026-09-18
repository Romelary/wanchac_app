"""Prueba de humo del cuaderno de entrenamiento (sin GPU, sin internet, sin Google Drive).

Por qué existe: el cuaderno se ejecuta una sola vez en Colab, pero un error a mitad de camino
(p. ej. en la exportación a TFLite, al final) costaría minutos u horas de entrenamiento. Esta
prueba ejecuta el MISMO archivo `clasificador_residuos.py` con `SMOKE_TEST=1`, que sustituye
los datos reales por imágenes sintéticas y reduce el entrenamiento a una época, y comprueba:

  1. que el cuaderno corre de principio a fin y produce todos los archivos;
  2. que `labels.txt` y `metadata.json` respetan el contrato con la app, incluido el ORDEN de las
     clases, que se compara contra el enum `CategoriaResiduo` de Kotlin;
  3. que el `.tflite` tiene la entrada/salida que la app espera y produce probabilidades válidas;
  4. que el `.ipynb` generado es válido y está al día respecto al `.py`.

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
        entorno = {
            **os.environ,
            "SMOKE_TEST": "1",
            "SMOKE_DIR": str(cls.raiz),
            "PYTHONUTF8": "1",  # Windows: evita UnicodeEncodeError con tildes en la consola
            "TF_CPP_MIN_LOG_LEVEL": "2",
        }
        cls.proceso = subprocess.run(
            [sys.executable, str(FUENTE)], env=entorno, capture_output=True, text=True, encoding="utf-8"
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
        # MIXTO no es una clase del modelo: es el "no sé" al que la app recurre con poca confianza.
        clases_del_modelo = [v.lower() for v in valores if v != "MIXTO"]
        labels = (self.salida / "labels.txt").read_text(encoding="utf-8").split()
        self.assertEqual(labels, clases_del_modelo)

    def test_la_siembra_de_trashnet_mapea_las_carpetas_y_descarta_la_basura(self):
        """Zip falso: plastic 12, cardboard 8, paper 8, glass 12, metal 5, trash 5 (+ basura de __MACOSX).
        Esperado: metal/trash y __MACOSX no entran; cardboard+paper se funden en `carton` con tope 15."""
        conteo = json.loads((self.salida / "metadata.json").read_text(encoding="utf-8"))["dataset"]["conteo_por_clase"]
        de_trashnet = {clase: datos["de_trashnet"] for clase, datos in conteo.items()}
        total = {clase: datos["total"] for clase, datos in conteo.items()}
        self.assertEqual(de_trashnet, {"plastico": 12, "carton": 15, "vidrio": 12, "organico": 0, "escombros": 0})
        self.assertEqual(total, {"plastico": 12, "carton": 15, "vidrio": 12, "organico": 15, "escombros": 15})

    def test_un_archivo_corrupto_se_descarta_y_se_informa_y_un_formato_no_soportado_se_avisa(self):
        """El dataset trae `corrupta.jpg` (bytes que no son imagen) y `foto.heic` (formato que TensorFlow no lee).
        Ninguno debe contar ni romper el entrenamiento, y ninguno debe ignorarse EN SILENCIO."""
        meta = json.loads((self.salida / "metadata.json").read_text(encoding="utf-8"))
        self.assertEqual(meta["dataset"]["archivos_descartados"], ["organico/corrupta.jpg"])
        self.assertIn("foto.heic", self.proceso.stdout, "el formato no soportado debe avisarse por consola")

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
        self.assertEqual(list(salida["shape"]), [1, 5])

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
