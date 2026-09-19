"""Pruebas de la carga del `.env` (`wanchaq_limpio/entorno.py`)."""

import os
import tempfile
from pathlib import Path
from unittest import mock

from django.core.exceptions import ImproperlyConfigured
from django.test import SimpleTestCase

from wanchaq_limpio.entorno import cargar_dotenv


class CargarDotenvTest(SimpleTestCase):
    def _cargar(self, contenido_bytes, entorno_previo=None):
        with tempfile.TemporaryDirectory() as carpeta, mock.patch.dict(os.environ, entorno_previo or {}):
            ruta = Path(carpeta) / ".env"
            ruta.write_bytes(contenido_bytes)
            resultado = cargar_dotenv(ruta)
            return resultado, {k: os.environ.get(k) for k in ("PRUEBA_A", "PRUEBA_B")}

    def test_lee_un_archivo_normal(self):
        _, valores = self._cargar(b"PRUEBA_A=hola\nPRUEBA_B=mundo\n")
        self.assertEqual(valores, {"PRUEBA_A": "hola", "PRUEBA_B": "mundo"})

    def test_tolera_el_bom_de_utf8(self):
        # Sin BOM-tolerancia la primera variable se llamaría "﻿PRUEBA_A" y no se encontraría, sin ningún aviso.
        # Hoy lo garantiza python-dotenv; esta prueba avisa si una versión futura lo dejara de hacer.
        _, valores = self._cargar("﻿PRUEBA_A=hola\nPRUEBA_B=mundo\n".encode("utf-8"))
        self.assertEqual(valores, {"PRUEBA_A": "hola", "PRUEBA_B": "mundo"})

    def test_un_archivo_utf16_da_un_error_claro(self):
        # Es lo que produce `echo X=1 > .env` en PowerShell 5.1.
        with self.assertRaises(ImproperlyConfigured) as contexto:
            self._cargar("PRUEBA_A=hola\n".encode("utf-16"))
        self.assertIn("UTF-8", str(contexto.exception))
        self.assertIn(".env", str(contexto.exception))

    def test_no_pisa_las_variables_que_ya_existen(self):
        # En Render el panel manda: un .env olvidado no debe poder cambiar lo que allí se configuró.
        _, valores = self._cargar(b"PRUEBA_A=del-archivo\n", entorno_previo={"PRUEBA_A": "del-entorno"})
        self.assertEqual(valores["PRUEBA_A"], "del-entorno")

    def test_un_archivo_inexistente_no_da_error(self):
        with tempfile.TemporaryDirectory() as carpeta:
            self.assertFalse(cargar_dotenv(Path(carpeta) / "no-existe.env"))

    def test_acepta_valores_con_comillas_y_sin_ellas(self):
        _, valores = self._cargar(b'PRUEBA_A="con comillas"\nPRUEBA_B=sin_comillas\n')
        self.assertEqual(valores, {"PRUEBA_A": "con comillas", "PRUEBA_B": "sin_comillas"})
