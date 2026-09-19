"""Pruebas del filtro que oculta los identificadores de dispositivo en los registros (wanchaq_limpio/logs.py)."""

import io
import logging
import sys
from unittest import mock

from django.test import SimpleTestCase, TestCase

from api import fotos
from wanchaq_limpio.logs import OcultarIdentificadores

ID = "3f2b8c1e-5a7d-4e0b-9c64-1d2e3f4a5b6c"


def _registro(mensaje, *args, exc_info=None):
    return logging.LogRecord("prueba", logging.WARNING, __file__, 1, mensaje, args, exc_info)


class FiltroTest(SimpleTestCase):
    def test_reemplaza_el_uuid_en_el_mensaje(self):
        registro = _registro("Not Found: /reportes/%s/5", ID)
        self.assertTrue(OcultarIdentificadores().filter(registro))
        self.assertEqual(registro.getMessage(), "Not Found: /reportes/[id]/5")

    def test_reemplaza_uuid_en_mayusculas_y_varios_a_la_vez(self):
        registro = _registro(f"{ID.upper()} y {ID}")
        OcultarIdentificadores().filter(registro)
        self.assertEqual(registro.getMessage(), "[id] y [id]")

    def test_no_toca_lo_que_no_es_un_uuid(self):
        registro = _registro("Cloudinary falló (%s) en /health", "RuntimeError")
        OcultarIdentificadores().filter(registro)
        self.assertEqual(registro.getMessage(), "Cloudinary falló (RuntimeError) en /health")

    def test_reemplaza_el_uuid_dentro_de_un_traceback(self):
        try:
            raise ValueError(f"no existe el reporte {ID}")
        except ValueError:
            registro = _registro("fallo", exc_info=sys.exc_info())
        OcultarIdentificadores().filter(registro)
        texto = logging.Formatter().format(registro)
        self.assertIn("ValueError", texto)
        self.assertNotIn(ID, texto)
        self.assertIn("[id]", texto)


class ConfiguracionTest(TestCase):
    def _manejador(self):
        return next(
            h for h in logging.getLogger().handlers if any(isinstance(f, OcultarIdentificadores) for f in h.filters)
        )

    def test_el_filtro_esta_instalado_en_el_handler_de_la_raiz(self):
        # Django encamina sus registros ("Not Found: /reportes/…") a la raíz por propagación
        self.assertIsNotNone(self._manejador())

    def test_una_baja_que_da_404_no_deja_el_uuid_en_los_registros_reales(self):
        manejador = self._manejador()
        salida = io.StringIO()
        anterior = manejador.setStream(salida)
        try:
            with mock.patch.object(fotos, "borrar"):
                respuesta = self.client.delete(f"/reportes/{ID}/5")
        finally:
            manejador.setStream(anterior)
        self.assertEqual(respuesta.status_code, 404)
        self.assertIn("Not Found", salida.getvalue(), "Django debe seguir registrando el 404")
        self.assertNotIn(ID, salida.getvalue())
