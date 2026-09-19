"""Pruebas de `verificar_servicios.py --remoto … --reporte-de-prueba`: el contrato de la API ejecutado contra el servicio.

La prueba más importante es `ContratoEnVivoTest`: lanza la aplicación Django REAL en un servidor HTTP y le pasa la MISMA
secuencia que se ejecutará contra Render, con Cloudinary simulado. Si el script y la API discrepan, falla aquí y no en el
primer despliegue.
"""

import base64
import hashlib
import io
import uuid
from contextlib import redirect_stdout
from unittest import mock

from django.test import LiveServerTestCase, TestCase

import verificar_servicios as v
from api import fotos
from api.models import Reporte, ReporteEliminado

ID_FIJO = uuid.UUID("3f2b8c1e-5a7d-4e0b-9c64-1d2e3f4a5b6c")


def _capturar(funcion, *args):
    salida = io.StringIO()
    with redirect_stdout(salida):
        resultado = funcion(*args)
    return resultado, salida.getvalue()


class JpegMinimoTest(TestCase):
    def test_es_el_jpeg_de_286_bytes_que_se_valido_con_un_decodificador(self):
        datos = base64.b64decode(v.JPEG_MINIMO_B64)
        self.assertEqual(len(datos), 286)
        self.assertEqual(datos[:3], b"\xff\xd8\xff")
        self.assertEqual(datos[-2:], b"\xff\xd9")
        # Se decodificó con Pillow al generarlo (8x8 px). El hash detecta cualquier byte alterado al copiarlo.
        self.assertEqual(hashlib.sha256(datos).hexdigest(), "6986825318099dc2a2a2c7082f80ee4d64416ad04fac2eb650feda2ec7ee750c")


class MultipartTest(TestCase):
    """Los bytes que arma `_multipart` deben ser aceptados por la API real, igual que los de Retrofit."""

    def test_la_api_real_acepta_el_cuerpo_y_recibe_la_foto_intacta(self):
        jpeg = base64.b64decode(v.JPEG_MINIMO_B64)
        documento = (
            '{"type":"Feature","geometry":{"type":"Point","coordinates":[-71.9475,-13.53]},"properties":'
            f'{{"idAnonimo":"{ID_FIJO}","idLocal":9000000001,"categoria":"MIXTO","fechaHoraEpochMillis":'
            f'{__import__("time").time_ns() // 1_000_000}}}}}'
        )
        cuerpo, tipo = v._multipart(("geojson", documento), ("foto", "prueba.jpg", jpeg))
        with mock.patch.object(fotos, "subir") as subir:
            respuesta = self.client.post("/reportes", data=cuerpo, content_type=tipo)
        self.assertEqual(respuesta.status_code, 201, respuesta.content)
        subir.assert_called_once()
        self.assertEqual(subir.call_args.args[0], jpeg)  # llegó byte a byte
        reporte = Reporte.objects.get()
        self.assertEqual((reporte.id_local, reporte.categoria), (9000000001, "MIXTO"))

    def test_las_partes_llevan_los_tipos_que_envia_retrofit(self):
        # El servidor valida la foto por su firma (FF D8 FF), no por el tipo declarado, así que solo esta prueba impide que
        # el script se aleje de lo que manda la app: geojson como JSON con charset, foto como image/jpeg con nombre de archivo.
        cuerpo, _ = v._multipart(("geojson", "{}"), ("foto", "prueba.jpg", b"\xff\xd8\xff"))
        self.assertIn(b'name="geojson"\r\nContent-Type: application/json; charset=utf-8\r\n\r\n{}', cuerpo)
        self.assertIn(b'name="foto"; filename="prueba.jpg"\r\nContent-Type: image/jpeg\r\n\r\n', cuerpo)

    def test_cada_cuerpo_usa_una_frontera_distinta(self):
        a = v._multipart(("geojson", "{}"), ("foto", "f.jpg", b"x"))[1]
        b = v._multipart(("geojson", "{}"), ("foto", "f.jpg", b"x"))[1]
        self.assertNotEqual(a, b)


class ContratoEnVivoTest(LiveServerTestCase):
    def test_la_secuencia_completa_contra_la_aplicacion_real_por_http(self):
        with mock.patch.object(fotos, "subir") as subir, mock.patch.object(fotos, "borrar") as borrar:
            resultado, salida = _capturar(v._prueba_de_contrato, self.live_server_url)
        self.assertTrue(resultado, salida)
        self.assertEqual(salida.count("OK"), 6, salida)
        self.assertNotIn("FALLO", salida)
        self.assertEqual(subir.call_count, 1)  # solo la primera subida llega a Cloudinary: el resto lo cortan la lápida y la validación
        self.assertEqual(borrar.call_count, 1)
        self.assertEqual(Reporte.objects.count(), 0)  # el reporte de prueba se borró
        self.assertEqual(ReporteEliminado.objects.count(), 1)  # y quedó su lápida

    def test_no_imprime_el_identificador_de_la_prueba(self):
        with mock.patch.object(v.uuid, "uuid4", return_value=ID_FIJO), mock.patch.object(fotos, "subir"), mock.patch.object(fotos, "borrar"):
            _, salida = _capturar(v._prueba_de_contrato, self.live_server_url)
        self.assertNotIn(str(ID_FIJO), salida)
        self.assertNotIn("9000", salida)


class RamasDeFalloTest(TestCase):
    CORRECTA = [(201, None), (200, None), (204, None), (404, None), (410, None), (422, None)]

    def _correr(self, respuestas):
        with mock.patch.object(v, "_enviar", side_effect=respuestas) as enviar:
            resultado, salida = _capturar(v._prueba_de_contrato, "https://servicio.ejemplo")
        return resultado, salida, enviar

    def test_todo_correcto(self):
        resultado, salida, enviar = self._correr(self.CORRECTA)
        self.assertTrue(resultado)
        self.assertEqual(enviar.call_count, 6)  # sin limpieza: la baja se completó

    def test_las_seis_peticiones_usan_el_metodo_y_la_ruta_esperados(self):
        _, _, enviar = self._correr(self.CORRECTA)
        metodos = [llamada.args[0] for llamada in enviar.call_args_list]
        rutas = [llamada.args[1] for llamada in enviar.call_args_list]
        self.assertEqual(metodos, ["POST", "POST", "DELETE", "DELETE", "POST", "POST"])
        self.assertEqual(rutas[0], "https://servicio.ejemplo/reportes")
        self.assertRegex(rutas[2], r"^https://servicio\.ejemplo/reportes/[0-9a-f-]{36}/9\d{9}$")
        self.assertEqual(rutas[2], rutas[3])  # la misma clave en las dos bajas

    def test_si_falla_la_creacion_se_detiene_y_orienta(self):
        for codigo, pista in ((404, "Fase 1"), (500, "migraciones"), (503, "Cloudinary"), (0, "no hubo respuesta"), (400, "rechazó")):
            with self.subTest(codigo=codigo):
                resultado, salida, enviar = self._correr([(codigo, None)])
                self.assertFalse(resultado)
                self.assertIn(f"dio {codigo}", salida)
                self.assertIn(pista, salida)
                self.assertEqual(enviar.call_count, 1)  # no sigue: nada que limpiar, nada que probar

    def test_cada_paso_con_un_codigo_inesperado_es_fallo_y_dice_cual(self):
        for indice, esperado in ((1, 200), (2, 204), (3, 404), (4, 410), (5, 422)):
            respuestas = list(self.CORRECTA)
            respuestas[indice] = (503, None)
            with self.subTest(paso=indice):
                resultado, salida, _ = self._correr(respuestas + [(204, None)])
                self.assertFalse(resultado)
                self.assertIn(f"se esperaba {esperado} y dio 503", salida)

    def test_si_la_baja_no_se_completa_se_intenta_limpiar_el_reporte_de_prueba(self):
        respuestas = [(201, None), (200, None), (503, None), (404, None), (410, None), (422, None), (204, None)]
        resultado, salida, enviar = self._correr(respuestas)
        self.assertFalse(resultado)
        self.assertEqual(enviar.call_count, 7)
        self.assertEqual(enviar.call_args_list[-1].args[0], "DELETE")  # la limpieza
        self.assertIn("se intentó borrar el reporte de prueba", salida)

    def test_si_la_baja_se_completa_no_hay_limpieza_extra(self):
        _, salida, enviar = self._correr(self.CORRECTA)
        self.assertNotIn("se intentó borrar", salida)
        self.assertEqual(enviar.call_count, 6)


class EnlaceConVerificarRemotoTest(TestCase):
    URL = "https://servicio.ejemplo"
    SERVICIOS_OK = {"servicios": {"base_de_datos": {"ok": True, "detalle": "x"}, "cloudinary": {"ok": True, "detalle": "y"}}}
    SERVICIOS_MAL = {"servicios": {"base_de_datos": {"ok": False, "detalle": "no se pudo conectar (OperationalError)"}}}

    def _correr(self, servicios, con_reporte, contrato=True):
        respuestas = [(200, {"estado": "ok"}, 1.0), (200 if servicios is self.SERVICIOS_OK else 503, servicios, 1.0)]
        with mock.patch("wanchaq_limpio.entorno.cargar_dotenv"), mock.patch.dict("os.environ", {"HEALTH_TOKEN": "t"}), \
                mock.patch.object(v, "_pedir", side_effect=respuestas), \
                mock.patch.object(v, "_prueba_de_contrato", return_value=contrato) as prueba:
            codigo, salida = _capturar(v.verificar_remoto, self.URL, con_reporte)
        return codigo, salida, prueba

    def test_sin_la_opcion_no_se_ejecuta_el_contrato(self):
        codigo, _, prueba = self._correr(self.SERVICIOS_OK, con_reporte=False)
        self.assertEqual(codigo, 0)
        prueba.assert_not_called()

    def test_con_la_opcion_y_los_servicios_bien_se_ejecuta(self):
        codigo, _, prueba = self._correr(self.SERVICIOS_OK, con_reporte=True)
        self.assertEqual(codigo, 0)
        prueba.assert_called_once_with(self.URL)

    def test_si_el_contrato_falla_el_resultado_global_falla(self):
        codigo, salida, _ = self._correr(self.SERVICIOS_OK, con_reporte=True, contrato=False)
        self.assertEqual(codigo, 1)
        self.assertIn("HAY FALLOS", salida)

    def test_si_un_servicio_falla_no_se_ejecuta_el_contrato_y_se_avisa(self):
        codigo, salida, prueba = self._correr(self.SERVICIOS_MAL, con_reporte=True)
        self.assertEqual(codigo, 1)
        prueba.assert_not_called()
        self.assertIn("se omite --reporte-de-prueba", salida)

    def test_la_opcion_sin_remoto_es_un_error_de_uso(self):
        with self.assertRaises(SystemExit) as contexto, redirect_stdout(io.StringIO()), mock.patch("sys.stderr", io.StringIO()):
            v.main(["--reporte-de-prueba"])
        self.assertEqual(contexto.exception.code, 2)
