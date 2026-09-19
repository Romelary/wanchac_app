"""Pruebas de `verificar_servicios.py`, con la red y los servicios simulados.

El script es lo que TÚ ejecutas con tus credenciales reales y lo que se pega en el chat, así que se prueba que:
  * su lógica de decisión es correcta (una URL pública que sirva la foto es un FALLO, no un OK),
  * nunca imprime un secreto,
  * no deja basura en Cloudinary aunque una comprobación falle.
"""

import io
import os
import struct
import tempfile
import urllib.error
import zlib
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

from django.test import SimpleTestCase

import verificar_servicios as v

TOKEN = "tok-secreto-que-no-debe-imprimirse"
SERVICIOS_OK = {"ok": True, "servicios": {"base_de_datos": {"ok": True, "detalle": "conectado"}, "cloudinary": {"ok": True, "detalle": "ok"}}}
SERVICIOS_MAL = {"ok": False, "servicios": {"base_de_datos": {"ok": False, "detalle": "no se pudo conectar (OperationalError)"}}}


def _capturar(funcion, *args):
    salida = io.StringIO()
    with redirect_stdout(salida):
        codigo = funcion(*args)
    return codigo, salida.getvalue()


class PngMinimoTest(SimpleTestCase):
    def test_es_un_png_valido_de_1x1(self):
        datos = v.png_minimo()
        self.assertEqual(datos[:8], b"\x89PNG\r\n\x1a\n")
        posicion, trozos = 8, {}
        while posicion < len(datos):
            largo, tipo = struct.unpack(">I4s", datos[posicion:posicion + 8])
            cuerpo = datos[posicion + 8:posicion + 8 + largo]
            (crc,) = struct.unpack(">I", datos[posicion + 8 + largo:posicion + 12 + largo])
            self.assertEqual(crc, zlib.crc32(tipo + cuerpo) & 0xFFFFFFFF, f"CRC inválido en {tipo}")
            trozos[tipo] = cuerpo
            posicion += 12 + largo
        self.assertEqual(list(trozos), [b"IHDR", b"IDAT", b"IEND"])
        self.assertEqual(struct.unpack(">II", trozos[b"IHDR"][:8]), (1, 1))
        self.assertEqual(len(zlib.decompress(trozos[b"IDAT"])), 4)  # 1 byte de filtro + 3 de color


@mock.patch("wanchaq_limpio.entorno.cargar_dotenv")
class RemotoTest(SimpleTestCase):
    def _verificar(self, respuestas, url="https://api.ejemplo.onrender.com", token=TOKEN):
        with mock.patch.dict("os.environ", {"HEALTH_TOKEN": token}), mock.patch.object(
            v, "_pedir", side_effect=respuestas
        ) as pedir:
            codigo, salida = _capturar(v.verificar_remoto, url)
        return codigo, salida, pedir

    def test_todo_bien_devuelve_0(self, _):
        codigo, salida, pedir = self._verificar([(200, {"estado": "ok"}, 2.0), (200, SERVICIOS_OK, 0.5)])
        self.assertEqual(codigo, 0)
        self.assertIn("RESULTADO: TODO OK", salida)
        # El token viaja SOLO en la cabecera de la segunda llamada
        self.assertEqual(pedir.call_args_list[1].args[1], {"X-Health-Token": TOKEN})

    def test_nunca_imprime_el_token(self, _):
        for respuestas in (
            [(200, {"estado": "ok"}, 1.0), (200, SERVICIOS_OK, 1.0)],
            [(200, {"estado": "ok"}, 1.0), (401, {"error": "token invalido"}, 1.0)],
            [(200, {"estado": "ok"}, 1.0), (503, SERVICIOS_MAL, 1.0)],
        ):
            _, salida, _ = self._verificar(respuestas)
            self.assertNotIn(TOKEN, salida)

    def test_un_servicio_caido_devuelve_1_y_lo_nombra(self, _):
        codigo, salida, _ = self._verificar([(200, {"estado": "ok"}, 1.0), (503, SERVICIOS_MAL, 1.0)])
        self.assertEqual(codigo, 1)
        self.assertIn("FALLO base_de_datos", salida)

    def test_token_rechazado_se_explica(self, _):
        codigo, salida, _ = self._verificar([(200, {"estado": "ok"}, 1.0), (401, {"error": "x"}, 1.0)])
        self.assertEqual(codigo, 1)
        self.assertIn("no coincide", salida)

    def test_endpoint_inexistente_se_explica(self, _):
        codigo, salida, _ = self._verificar([(200, {"estado": "ok"}, 1.0), (404, None, 1.0)])
        self.assertEqual(codigo, 1)
        self.assertIn("HEALTH_TOKEN en el panel de Render", salida)

    def test_si_health_no_responde_no_se_envia_el_token(self, _):
        codigo, _, pedir = self._verificar([(0, None, 120.0)])
        self.assertEqual(codigo, 1)
        self.assertEqual(pedir.call_count, 1)

    def test_avisa_del_arranque_en_frio(self, _):
        _, salida, _ = self._verificar([(200, {"estado": "ok"}, 58.0), (200, SERVICIOS_OK, 1.0)])
        self.assertIn("AVISO", salida)

    def test_un_env_mal_codificado_se_explica_sin_traceback(self, cargar):
        from django.core.exceptions import ImproperlyConfigured

        cargar.side_effect = ImproperlyConfigured("backend/.env no está guardado como UTF-8")
        codigo, salida, pedir = self._verificar([])
        self.assertEqual(codigo, 1)
        self.assertIn("UTF-8", salida)
        self.assertEqual(pedir.call_count, 0)

    def test_sin_https_no_envia_nada(self, _):
        codigo, salida, pedir = self._verificar([], url="http://api.ejemplo.com")
        self.assertEqual(codigo, 1)
        self.assertEqual(pedir.call_count, 0)
        self.assertIn("https://", salida)

    def test_sin_token_local_no_consulta_servicios(self, _):
        codigo, salida, pedir = self._verificar([(200, {"estado": "ok"}, 1.0)], token="")
        self.assertEqual(codigo, 1)
        self.assertEqual(pedir.call_count, 1)
        self.assertIn("HEALTH_TOKEN", salida)


class PedirTest(SimpleTestCase):
    def test_un_error_http_devuelve_su_codigo_y_su_cuerpo(self):
        error = urllib.error.HTTPError("u", 503, "x", {}, io.BytesIO(b'{"ok": false}'))
        with mock.patch("urllib.request.urlopen", side_effect=error):
            codigo, datos, _ = v._pedir("https://x")
        self.assertEqual((codigo, datos), (503, {"ok": False}))

    def test_sin_conexion_devuelve_0(self):
        with mock.patch("urllib.request.urlopen", side_effect=OSError("sin red")):
            self.assertEqual(v._pedir("https://x")[:2], (0, None))

    def test_cuerpo_que_no_es_json_no_rompe(self):
        respuesta = mock.MagicMock(status=200)
        respuesta.__enter__.return_value = respuesta
        respuesta.read.return_value = b"<html>"
        with mock.patch("urllib.request.urlopen", return_value=respuesta):
            self.assertEqual(v._pedir("https://x")[:2], (200, None))


class SubidaPrivadaTest(SimpleTestCase):
    """`_subida_privada` con Cloudinary simulado: se decide bien y siempre se limpia."""

    def _correr(self, tipo="authenticated", estado_publico=404, estado_firmado=200, resultado_borrado="ok"):
        with mock.patch("cloudinary.uploader.upload", return_value={"type": tipo, "version": 1}) as subir, mock.patch(
            "cloudinary.uploader.destroy", return_value={"result": resultado_borrado}
        ) as borrar, mock.patch("cloudinary.config", return_value=mock.Mock(cloud_name="nube")), mock.patch(
            "cloudinary.utils.cloudinary_url", return_value=("https://res.cloudinary.com/firmada", {})
        ), mock.patch.object(v, "_estado_http", side_effect=[estado_publico, estado_firmado]):
            codigo, salida = _capturar(v._subida_privada)
        return codigo, salida, subir, borrar

    def test_todo_correcto(self):
        ok, salida, subir, borrar = self._correr()
        self.assertTrue(ok)
        self.assertNotIn("FALLO", salida)
        self.assertEqual(subir.call_args.kwargs["type"], "authenticated")
        borrar.assert_called_once()
        self.assertEqual(borrar.call_args.kwargs["type"], "authenticated")

    def test_url_publica_que_sirve_la_foto_es_fallo(self):
        ok, salida, _, borrar = self._correr(estado_publico=200)
        self.assertFalse(ok)
        self.assertIn("expuestas", salida)
        borrar.assert_called_once()  # aunque falle, se borra el recurso de prueba

    def test_recurso_que_no_quedo_privado_es_fallo(self):
        ok, salida, _, borrar = self._correr(tipo="upload")
        self.assertFalse(ok)
        self.assertIn("públicas", salida)
        borrar.assert_called_once()

    def test_url_firmada_que_no_sirve_es_fallo(self):
        ok, _, _, _ = self._correr(estado_firmado=403)
        self.assertFalse(ok)

    def test_no_poder_borrar_es_fallo_y_pide_borrarlo_a_mano(self):
        ok, salida, _, _ = self._correr(resultado_borrado="not found")
        self.assertFalse(ok)
        self.assertIn("a mano", salida)

    def test_subida_rechazada_no_filtra_el_mensaje(self):
        with mock.patch("cloudinary.uploader.upload", side_effect=RuntimeError(f"api_secret={TOKEN}")):
            ok, salida = _capturar(v._subida_privada)
        self.assertFalse(ok)
        self.assertNotIn(TOKEN, salida)
        self.assertIn("RuntimeError", salida)


class LocalTest(SimpleTestCase):
    def _correr(self, entorno, bd_ok=True, cloudinary_ok=True, con_env=True, subida=False, subida_ok=True,
                contenido_env="", clave_api="123456789012345"):
        bd = {"ok": bd_ok, "detalle": "conectado; PostGIS 3.5 operativo" if bd_ok else "no se pudo conectar (OperationalError)"}
        cl = {"ok": cloudinary_ok, "detalle": "credenciales aceptadas"}
        with tempfile.TemporaryDirectory() as carpeta:
            if con_env:
                archivo = Path(carpeta) / ".env"
                if isinstance(contenido_env, bytes):
                    archivo.write_bytes(contenido_env)
                else:
                    archivo.write_text(contenido_env, encoding="utf-8")
            with mock.patch.object(v, "BACKEND", Path(carpeta)), mock.patch.object(v, "_iniciar_django", return_value=True), \
                    mock.patch.dict("os.environ", entorno, clear=False), \
                    mock.patch("api.servicios.comprobar_base_de_datos", return_value=bd), \
                    mock.patch("api.servicios.comprobar_cloudinary", return_value=cl), \
                    mock.patch("cloudinary.config", return_value=mock.Mock(api_key=clave_api)), \
                    mock.patch.object(v, "_subida_privada", return_value=subida_ok) as subida_mock:
                codigo, salida = _capturar(v.verificar_local, subida)
        return codigo, salida, subida_mock

    ENTORNO = {"DATABASE_URL": "postgresql://u:clave-secreta@host.neon.tech/db", "CLOUDINARY_URL": "cloudinary://1:sec@n", "HEALTH_TOKEN": TOKEN}

    def test_todo_correcto_devuelve_0_y_no_imprime_valores(self):
        codigo, salida, _ = self._correr(self.ENTORNO)
        self.assertEqual(codigo, 0)
        self.assertIn("RESULTADO: TODO OK", salida)
        for secreto in ("clave-secreta", TOKEN, "cloudinary://", "neon.tech"):
            self.assertNotIn(secreto, salida)

    def test_variable_vacia_es_fallo(self):
        codigo, salida, _ = self._correr({**self.ENTORNO, "HEALTH_TOKEN": ""})
        self.assertEqual(codigo, 1)
        self.assertIn("FALLO HEALTH_TOKEN", salida)

    def test_sin_archivo_env_es_fallo(self):
        codigo, salida, _ = self._correr(self.ENTORNO, con_env=False)
        self.assertEqual(codigo, 1)
        self.assertIn("no existe backend/.env", salida)

    def test_base_caida_es_fallo(self):
        codigo, salida, _ = self._correr(self.ENTORNO, bd_ok=False)
        self.assertEqual(codigo, 1)
        self.assertIn("FALLO Neon", salida)

    def test_cadena_pooled_solo_avisa(self):
        entorno = {**self.ENTORNO, "DATABASE_URL": "postgresql://u:p@ep-x-pooler.neon.tech/db"}
        codigo, salida, _ = self._correr(entorno)
        self.assertEqual(codigo, 0)
        self.assertIn("AVISO", salida)

    def test_si_la_subida_privada_falla_el_resultado_global_falla(self):
        codigo, salida, _ = self._correr(self.ENTORNO, subida=True, subida_ok=False)
        self.assertEqual(codigo, 1)
        self.assertIn("HAY FALLOS", salida)

    def test_subida_solo_se_prueba_si_se_pide_y_cloudinary_responde(self):
        _, _, sin_pedir = self._correr(self.ENTORNO, subida=False)
        sin_pedir.assert_not_called()
        _, _, pedida = self._correr(self.ENTORNO, subida=True)
        pedida.assert_called_once()
        codigo, salida, no_valida = self._correr(self.ENTORNO, cloudinary_ok=False, subida=True)
        no_valida.assert_not_called()
        self.assertEqual(codigo, 1)

    # --- Diagnóstico de ORIGEN de las variables: la trampa de "corrijo el .env y sigue el mismo error" ---------------
    def test_una_variable_del_entorno_de_windows_que_contradice_al_env_avisa_sin_mostrar_valores(self):
        # La carga del .env no pisa lo que ya existe en el entorno: una CLOUDINARY_URL puesta antes en PowerShell GANA.
        entorno = {**self.ENTORNO, "CLOUDINARY_URL": "cloudinary://1:secretoviejo@nube"}
        _, salida, _ = self._correr(entorno, contenido_env="CLOUDINARY_URL=cloudinary://2:secretonuevo@nube\n")
        self.assertIn("AVISO CLOUDINARY_URL existe TAMBIÉN en el entorno de Windows", salida)
        for valor in ("secretoviejo", "secretonuevo", "cloudinary://"):
            self.assertNotIn(valor, salida)

    def test_si_el_entorno_y_el_env_coinciden_no_hay_aviso(self):
        entorno = {**self.ENTORNO, "CLOUDINARY_URL": "cloudinary://1:igual@nube"}
        _, salida, _ = self._correr(entorno, contenido_env="CLOUDINARY_URL=cloudinary://1:igual@nube\n")
        self.assertNotIn("TAMBIÉN", salida)

    def test_una_variable_del_env_que_no_existe_en_el_entorno_no_es_conflicto(self):
        with mock.patch.dict("os.environ"):
            os.environ.pop("HEALTH_TOKEN", None)
            entorno = {k: val for k, val in self.ENTORNO.items() if k != "HEALTH_TOKEN"}
            _, salida, _ = self._correr(entorno, contenido_env="HEALTH_TOKEN=abc\n")
        self.assertNotIn("TAMBIÉN", salida)

    def test_una_variable_repetida_en_el_env_avisa_y_las_comentadas_no_cuentan(self):
        _, salida, _ = self._correr(self.ENTORNO, contenido_env="HEALTH_TOKEN=a\nHEALTH_TOKEN=b\nDEBUG=1\n")
        self.assertIn("HEALTH_TOKEN aparece varias veces", salida)
        self.assertNotIn("DEBUG aparece", salida)
        _, salida, _ = self._correr(self.ENTORNO, contenido_env="# HEALTH_TOKEN=vieja\nHEALTH_TOKEN=nueva\n")
        self.assertNotIn("varias veces", salida)

    def test_un_env_ilegible_no_rompe_el_diagnostico_de_origen(self):
        # UTF-16 (creado con `>` de PowerShell): de eso se queja la carga de ajustes; aquí no debe haber traceback.
        codigo, salida, _ = self._correr(self.ENTORNO, contenido_env="CLOUDINARY_URL=x\n".encode("utf-16"))
        self.assertNotIn("Traceback", salida)
        self.assertIn(codigo, (0, 1))

    # --- Reconocer la clave en la consola sin exponerla ---------------------------------------------------------------
    def test_si_cloudinary_falla_muestra_donde_termina_la_clave_sin_mostrarla_entera(self):
        _, salida, _ = self._correr(self.ENTORNO, cloudinary_ok=False)
        self.assertIn("INFO", salida)
        self.assertIn("la clave de API de Cloudinary en uso termina en ...2345", salida)
        self.assertNotIn("123456789012345", salida)
        self.assertNotIn("6789012345", salida)

    def test_si_cloudinary_funciona_no_menciona_la_clave(self):
        _, salida, _ = self._correr(self.ENTORNO)
        self.assertNotIn("termina en", salida)

    def test_una_clave_que_no_es_numerica_no_se_muestra_ni_en_parte(self):
        _, salida, _ = self._correr(self.ENTORNO, cloudinary_ok=False, clave_api="<your_api_key>")
        self.assertNotIn("termina en", salida)
        self.assertNotIn("your_api", salida)
