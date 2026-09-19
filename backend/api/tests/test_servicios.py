"""Pruebas de las comprobaciones de servicios, con las dependencias externas simuladas (sin red ni credenciales)."""

from unittest import mock

from django.test import SimpleTestCase, TestCase

from api import servicios

CLAVE_SECRETA = "s3cr3t0-que-no-debe-salir-en-ningun-mensaje"


class _CursorFalso:
    """Cursor de PostgreSQL simulado: responde a `postgis_version()` y `ST_DWithin`; se puede hacer fallar uno."""

    def __init__(self, postgis="3.5 USE_GEOS=1", dwithin=True, falla_postgis=False):
        self._respuestas = {"postgis_version": postgis, "ST_DWithin": dwithin}
        self._falla_postgis = falla_postgis
        self._ultima = None

    def __enter__(self):
        return self

    def __exit__(self, *args):
        return False

    def execute(self, sql):
        if "postgis_version" in sql and self._falla_postgis:
            raise RuntimeError(f"function postgis_version() does not exist ({CLAVE_SECRETA})")
        self._ultima = next((v for k, v in self._respuestas.items() if k in sql), 1)

    def fetchone(self):
        return (self._ultima,)


def _conexion_postgres(cursor):
    conexion = mock.MagicMock()
    conexion.vendor = "postgresql"
    conexion.cursor.return_value = cursor
    return conexion


class BaseDeDatosTest(SimpleTestCase):
    def test_postgis_operativo_es_ok_y_muestra_la_version(self):
        with mock.patch.object(servicios, "connection", _conexion_postgres(_CursorFalso())):
            r = servicios.comprobar_base_de_datos()
        self.assertTrue(r["ok"])
        self.assertIn("PostGIS 3.5", r["detalle"])

    def test_sin_postgis_falla_y_explica_el_remedio(self):
        with mock.patch.object(servicios, "connection", _conexion_postgres(_CursorFalso(falla_postgis=True))):
            r = servicios.comprobar_base_de_datos()
        self.assertFalse(r["ok"])
        self.assertIn("CREATE EXTENSION postgis", r["detalle"])

    def test_geography_que_no_mide_en_metros_falla(self):
        with mock.patch.object(servicios, "connection", _conexion_postgres(_CursorFalso(dwithin=False))):
            r = servicios.comprobar_base_de_datos()
        self.assertFalse(r["ok"])
        self.assertIn("ST_DWithin", r["detalle"])

    def test_error_de_conexion_no_filtra_el_mensaje_de_la_excepcion(self):
        conexion = mock.MagicMock()
        conexion.cursor.side_effect = RuntimeError(f"connection to host db.ejemplo.neon.tech user=x password={CLAVE_SECRETA}")
        with mock.patch.object(servicios, "connection", conexion):
            r = servicios.comprobar_base_de_datos()
        self.assertFalse(r["ok"])
        self.assertIn("RuntimeError", r["detalle"])
        self.assertNotIn(CLAVE_SECRETA, str(r))
        self.assertNotIn("neon.tech", str(r))

    def test_fallo_de_postgis_tampoco_filtra_el_mensaje(self):
        with mock.patch.object(servicios, "connection", _conexion_postgres(_CursorFalso(falla_postgis=True))):
            r = servicios.comprobar_base_de_datos()
        self.assertNotIn(CLAVE_SECRETA, str(r))


class BaseDeDatosSqliteTest(TestCase):
    def test_un_motor_que_no_es_postgresql_se_rechaza(self):
        # En pruebas la base es SQLite: si en producción faltara DATABASE_URL, esto es lo que se vería.
        r = servicios.comprobar_base_de_datos()
        self.assertFalse(r["ok"])
        self.assertIn("PostgreSQL", r["detalle"])


class CloudinaryTest(SimpleTestCase):
    def _configuracion(self, cloud_name="nube", api_key="clave", api_secret="secreto"):
        return mock.Mock(cloud_name=cloud_name, api_key=api_key, api_secret=api_secret)

    def test_sin_credenciales_falla_sin_llamar_a_la_red(self):
        with mock.patch("cloudinary.config", return_value=self._configuracion(cloud_name=None)), mock.patch(
            "cloudinary.api.ping"
        ) as ping:
            r = servicios.comprobar_cloudinary()
        self.assertFalse(r["ok"])
        self.assertIn("CLOUDINARY_URL", r["detalle"])
        ping.assert_not_called()

    def test_credenciales_aceptadas(self):
        with mock.patch("cloudinary.config", return_value=self._configuracion()), mock.patch(
            "cloudinary.api.ping", return_value={"status": "OK"}
        ):
            self.assertTrue(servicios.comprobar_cloudinary()["ok"])

    def test_credenciales_rechazadas_no_filtran_el_mensaje(self):
        with mock.patch("cloudinary.config", return_value=self._configuracion()), mock.patch(
            "cloudinary.api.ping", side_effect=RuntimeError(f"Invalid api_key {CLAVE_SECRETA}")
        ):
            r = servicios.comprobar_cloudinary()
        self.assertFalse(r["ok"])
        self.assertNotIn(CLAVE_SECRETA, str(r))


class ComprobarTodoTest(SimpleTestCase):
    def test_el_global_es_ok_solo_si_todos_lo_son(self):
        ok, mal = {"ok": True, "detalle": ""}, {"ok": False, "detalle": ""}
        for bd, cl, esperado in [(ok, ok, True), (ok, mal, False), (mal, ok, False), (mal, mal, False)]:
            with mock.patch.object(servicios, "comprobar_base_de_datos", return_value=bd), mock.patch.object(
                servicios, "comprobar_cloudinary", return_value=cl
            ):
                self.assertEqual(servicios.comprobar_todo()["ok"], esperado)
