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


AJUSTES_REALES = {"HOST": "ep-ejemplo.us-east-1.aws.neon.tech", "USER": "alex", "PASSWORD": "AbC123", "NAME": "neondb"}


def _conexion_postgres(cursor, ajustes=None):
    conexion = mock.MagicMock()
    conexion.vendor = "postgresql"
    conexion.settings_dict = ajustes or AJUSTES_REALES
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
        conexion.settings_dict = AJUSTES_REALES
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

    def test_los_marcadores_de_env_example_se_reconocen_sin_intentar_conectar(self):
        # Olvidar reemplazar la plantilla es el error más probable al configurar; sin esto el usuario vería un genérico
        # "no se pudo conectar" tras esperar a que falle el DNS.
        for campo, marcador in (("HOST", "HOST_DE_NEON"), ("PASSWORD", "CONTRASENA"), ("NAME", "NOMBRE_DE_BASE")):
            with self.subTest(campo=campo):
                conexion = _conexion_postgres(_CursorFalso(), {**AJUSTES_REALES, campo: marcador})
                with mock.patch.object(servicios, "connection", conexion):
                    r = servicios.comprobar_base_de_datos()
                self.assertFalse(r["ok"])
                self.assertIn("marcadores de .env.example", r["detalle"])
                conexion.cursor.assert_not_called()

    def test_ajustes_reales_no_se_confunden_con_una_plantilla(self):
        with mock.patch.object(servicios, "connection", _conexion_postgres(_CursorFalso())):
            self.assertTrue(servicios.comprobar_base_de_datos()["ok"])


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

    def test_la_plantilla_de_la_consola_de_cloudinary_se_reconoce_sin_llamar_a_la_red(self):
        # La consola muestra cloudinary://<your_api_key>:<your_api_secret>@nube; el SDK lee esos textos LITERALMENTE.
        casos = {
            "consola de Cloudinary": {"api_key": "<your_api_key>", "api_secret": "<your_api_secret>"},
            "solo el secreto sin rellenar": {"api_secret": "<your_api_secret>"},
            "sin los símbolos": {"api_key": "your_api_key"},
            "plantilla de .env.example": {"api_key": "CLAVE_API", "api_secret": "SECRETO_API", "cloud_name": "NOMBRE_DE_NUBE"},
        }
        for nombre, campos in casos.items():
            with self.subTest(nombre), mock.patch("cloudinary.config", return_value=self._configuracion(**campos)), mock.patch(
                "cloudinary.api.ping"
            ) as ping:
                r = servicios.comprobar_cloudinary()
            self.assertFalse(r["ok"])
            self.assertIn("plantilla", r["detalle"])
            ping.assert_not_called()

    # Valores FALSOS con la forma de un par real de Cloudinary (clave de 15 dígitos, secreto de 27 caracteres).
    FORMA_REAL = {"api_key": "123456789012345", "api_secret": "abcdefghijklmnopqrstuvwxyz0"}

    def _diagnosticar_401(self, mensaje, **campos):
        from cloudinary.exceptions import AuthorizationRequired

        configuracion = self._configuracion(**{**self.FORMA_REAL, **campos})
        with mock.patch("cloudinary.config", return_value=configuracion), mock.patch(
            "cloudinary.api.ping", side_effect=AuthorizationRequired(mensaje)
        ):
            return servicios.comprobar_cloudinary()

    def test_cada_causa_de_401_tiene_su_explicacion_y_no_copia_el_mensaje(self):
        # Los tres mensajes se MIDIERON contra Cloudinary con credenciales falsas (ver servicios.py).
        casos = {
            "Error 401 - unknown api_key": "no conoce esa clave",
            "Error 401 - disabled api_key": "existe pero está desactivada",
            "Error 401 - api_secret mismatch": "el SECRETO no le corresponde",
        }
        for mensaje, esperado in casos.items():
            with self.subTest(mensaje=mensaje):
                r = self._diagnosticar_401(mensaje)
                self.assertFalse(r["ok"])
                self.assertIn(esperado, r["detalle"])
                self.assertNotIn(mensaje.split(" - ")[1], r["detalle"])  # el texto del SDK no se copia
                self.assertNotIn("Pistas", r["detalle"])  # la forma del par es correcta: no hay nada que señalar

    def test_una_causa_de_401_desconocida_no_afirma_nada_especifico(self):
        r = self._diagnosticar_401("Error 401 - Invalid Signature")
        self.assertFalse(r["ok"])
        self.assertIn("no coinciden entre sí", r["detalle"])
        for afirmacion in ("desactivada", "no conoce", "SECRETO no le corresponde"):
            self.assertNotIn(afirmacion, r["detalle"])

    def test_las_pistas_de_forma_no_incluyen_los_valores(self):
        r = self._diagnosticar_401("Error 401 - api_secret mismatch", api_key="12345", api_secret="corto-secreto")
        self.assertIn("15 dígitos", r["detalle"])
        self.assertIn("27 caracteres", r["detalle"])
        self.assertNotIn("12345", r["detalle"])
        self.assertNotIn("corto-secreto", r["detalle"])

    def test_pistas_de_forma_distinguen_simbolos_inesperados_y_claves_no_numericas(self):
        r = self._diagnosticar_401("Error 401 - disabled api_key", api_key="12345678901234x", api_secret="a" * 26 + " ")
        self.assertIn("no todos son dígitos", r["detalle"])
        self.assertIn("símbolos o espacios inesperados", r["detalle"])

    def test_secreto_oculto_con_asteriscos_falla_sin_llamar_a_la_red(self):
        # Copiar el texto de la pantalla en vez de usar el botón de copiar da "*****": no puede ser válido.
        for oculto in ("*" * 27, "•" * 27):
            with self.subTest(oculto=oculto[:1]), mock.patch(
                "cloudinary.config", return_value=self._configuracion(**{**self.FORMA_REAL, "api_secret": oculto})
            ), mock.patch("cloudinary.api.ping") as ping:
                r = servicios.comprobar_cloudinary()
            self.assertFalse(r["ok"])
            self.assertIn("OCULTO", r["detalle"])
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
