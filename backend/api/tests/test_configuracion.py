"""Pruebas de la CONFIGURACIÓN: que un despliegue mal configurado falle fuerte y que nada sensible quede versionado.

Los ajustes se evalúan en un proceso hijo con un entorno controlado (no se lee `.env`, ver NO_CARGAR_DOTENV), porque
`settings.py` se ejecuta una sola vez al importarse y aquí hay que probar varias combinaciones de variables.
"""

import io
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

from django.core.management import call_command
from django.test import SimpleTestCase, TestCase

BACKEND = Path(__file__).resolve().parents[2]
RAIZ = BACKEND.parent
VARIABLES_DEL_PROYECTO = (
    "SECRET_KEY", "DEBUG", "DATABASE_URL", "CLOUDINARY_URL", "HEALTH_TOKEN", "ALLOWED_HOSTS", "RENDER_EXTERNAL_HOSTNAME",
)
# Con la misma forma que la cadena que genera la consola de Neon (documentación oficial, 2026-09-18).
POSTGRES = "postgresql://usuario:contrasena@ep-ejemplo.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require"


def _evaluar_ajustes(expresion, **entorno):
    """Importa los ajustes REALES (no los de prueba) en un proceso limpio y devuelve (codigo, stdout, stderr)."""
    env = {k: v for k, v in os.environ.items() if k not in VARIABLES_DEL_PROYECTO}
    env.update(NO_CARGAR_DOTENV="1", DJANGO_SETTINGS_MODULE="wanchaq_limpio.settings", **entorno)
    codigo = f"import django; django.setup(); from django.conf import settings as s; print({expresion})"
    p = subprocess.run([sys.executable, "-c", codigo], cwd=BACKEND, env=env, capture_output=True, text=True, timeout=60)
    return p.returncode, p.stdout.strip(), p.stderr


class FalloRapidoTest(SimpleTestCase):
    def test_sin_secret_key_en_produccion_no_arranca(self):
        codigo, _, err = _evaluar_ajustes("1", DATABASE_URL=POSTGRES)
        self.assertNotEqual(codigo, 0)
        self.assertIn("SECRET_KEY", err)

    def test_sin_database_url_en_produccion_no_arranca_en_vez_de_usar_sqlite(self):
        # Con SQLite en Render los reportes se perderían en cada reinicio, sin ningún error visible.
        codigo, _, err = _evaluar_ajustes("1", SECRET_KEY="una-clave")
        self.assertNotEqual(codigo, 0)
        self.assertIn("DATABASE_URL", err)

    def test_produccion_completa_arranca_con_debug_apagado(self):
        codigo, salida, _ = _evaluar_ajustes(
            "(s.DEBUG, s.DATABASES['default']['ENGINE'])", SECRET_KEY="una-clave", DATABASE_URL=POSTGRES
        )
        self.assertEqual(codigo, 0)
        self.assertEqual(salida, "(False, 'django.db.backends.postgresql')")

    def test_tls_es_obligatorio_aunque_la_url_no_lo_pida(self):
        # La URL de prueba NO trae ?sslmode=require: si el TLS se exige es por la configuración, no por la URL.
        sin_tls = POSTGRES.split("?")[0]
        self.assertNotIn("sslmode", sin_tls)
        _, salida, _ = _evaluar_ajustes(
            "s.DATABASES['default']['OPTIONS'].get('sslmode')", SECRET_KEY="una-clave", DATABASE_URL=sin_tls
        )
        self.assertEqual(salida, "require")

    def test_la_cadena_de_neon_conserva_sus_parametros_de_seguridad(self):
        # La consola de Neon añade channel_binding=require: no debe perderse ni romper la conexión.
        _, salida, _ = _evaluar_ajustes(
            "sorted(s.DATABASES['default']['OPTIONS'].items())", SECRET_KEY="una-clave", DATABASE_URL=POSTGRES
        )
        self.assertEqual(
            salida, "[('channel_binding', 'require'), ('connect_timeout', 15), ('sslmode', 'require')]"
        )

    def test_la_conexion_a_la_base_tiene_limite_de_tiempo(self):
        # Sin límite, una base que no contesta cuelga la petición hasta que gunicorn mata el proceso (ver settings.py).
        _, salida, _ = _evaluar_ajustes(
            "s.DATABASES['default']['OPTIONS']['connect_timeout']", SECRET_KEY="una-clave", DATABASE_URL=POSTGRES
        )
        self.assertEqual(salida, "15")

    def test_un_limite_de_tiempo_puesto_en_la_url_se_respeta(self):
        _, salida, _ = _evaluar_ajustes(
            "s.DATABASES['default']['OPTIONS']['connect_timeout']", SECRET_KEY="una-clave", DATABASE_URL=POSTGRES + "&connect_timeout=5"
        )
        self.assertEqual(salida, "5")

    def test_debug_por_defecto_esta_apagado(self):
        _, salida, _ = _evaluar_ajustes("s.DEBUG", SECRET_KEY="una-clave", DATABASE_URL=POSTGRES)
        self.assertEqual(salida, "False")

    def test_en_desarrollo_sin_nada_configurado_usa_sqlite(self):
        codigo, salida, _ = _evaluar_ajustes("s.DATABASES['default']['ENGINE']", DEBUG="1")
        self.assertEqual(codigo, 0)
        self.assertEqual(salida, "django.db.backends.sqlite3")

    def test_base_local_no_exige_tls(self):
        _, salida, _ = _evaluar_ajustes(
            "'sslmode' in s.DATABASES['default'].get('OPTIONS', {})",
            SECRET_KEY="una-clave", DATABASE_URL="postgresql://u:p@localhost:5432/prueba",
        )
        self.assertEqual(salida, "False")

    def test_el_host_de_render_se_permite_solo(self):
        _, salida, _ = _evaluar_ajustes(
            "s.ALLOWED_HOSTS", SECRET_KEY="una-clave", DATABASE_URL=POSTGRES,
            RENDER_EXTERNAL_HOSTNAME="wanchaq-limpio-api.onrender.com",
        )
        self.assertEqual(salida, "['wanchaq-limpio-api.onrender.com']")


class CloudinaryUrlMalFormadaTest(SimpleTestCase):
    """Con el SDK REAL de Cloudinary (que valida la URL al importarse) y en un proceso limpio, para que ningún mock
    oculte que `import cloudinary` lanza ValueError. Valores FALSOS: nunca se usan credenciales reales en las pruebas."""

    LLAMADA = "__import__('api.servicios', fromlist=['x']).comprobar_cloudinary()"

    def _diagnosticar(self, valor):
        return _evaluar_ajustes(self.LLAMADA, SECRET_KEY="una-clave", DATABASE_URL=POSTGRES, CLOUDINARY_URL=valor)

    def test_la_linea_repetida_da_una_explicacion_y_no_un_traceback(self):
        # El error real de un usuario: pegó `CLOUDINARY_URL=cloudinary://…` DENTRO del valor de CLOUDINARY_URL.
        codigo, salida, err = self._diagnosticar("CLOUDINARY_URL=cloudinary://111111:falsosecreto@nube-de-ejemplo")
        self.assertEqual(codigo, 0, f"no debe lanzar excepción: {err[-300:]}")
        self.assertIn("'ok': False", salida)
        self.assertIn("CLOUDINARY_URL mal formada", salida)
        self.assertIn("repetiste", salida)

    def test_otras_urls_que_el_sdk_rechaza_tambien_se_explican(self):
        for valor in ("https://111111:falsosecreto@nube", '"cloudinary://111111:falsosecreto@nube"'):
            with self.subTest(valor=valor.split("falsosecreto")[0]):
                codigo, salida, _ = self._diagnosticar(valor)
                self.assertEqual(codigo, 0)
                self.assertIn("CLOUDINARY_URL mal formada", salida)

    def test_el_diagnostico_no_copia_el_valor_ni_el_mensaje_del_sdk(self):
        _, salida, _ = self._diagnosticar("CLOUDINARY_URL=cloudinary://111111:falsosecreto@nube-de-ejemplo")
        for resto in ("falsosecreto", "111111", "nube-de-ejemplo", "Invalid CLOUDINARY_URL scheme"):
            self.assertNotIn(resto, salida)

    def test_una_url_incompleta_sin_excepcion_sigue_dando_su_propio_mensaje(self):
        # cloudinary://111111:falsosecreto (sin @nube) no hace lanzar al SDK: la atrapa la comprobación de "incompleta".
        _, salida, _ = self._diagnosticar("cloudinary://111111:falsosecreto")
        self.assertIn("ausente o mal formada", salida)


class CoherenciaDeArchivosTest(SimpleTestCase):
    """Los tres sitios donde se nombran las variables (código, plantilla local y Blueprint) no pueden desincronizarse."""

    ENTORNO_QUE_NO_SE_DOCUMENTA = {"RENDER_EXTERNAL_HOSTNAME", "NO_CARGAR_DOTENV"}  # la define Render / solo pruebas

    def _variables_que_lee_settings(self):
        fuente = (BACKEND / "wanchaq_limpio" / "settings.py").read_text(encoding="utf-8")
        return set(re.findall(r'(?:environ\.get|environ\[|_booleano)\(?"([A-Z_]+)"', fuente))

    def _variables_de_env_example(self):
        texto = (BACKEND / ".env.example").read_text(encoding="utf-8")
        return dict(re.findall(r"^([A-Z_]+)=(.*)$", texto, re.MULTILINE))

    def _bloques_de_render_yaml(self):
        texto = (RAIZ / "render.yaml").read_text(encoding="utf-8")
        return {b.splitlines()[0].strip(): b for b in re.split(r"\n\s*- key: ", texto)[1:]}

    def test_toda_variable_que_lee_settings_esta_en_env_example(self):
        leidas = self._variables_que_lee_settings() - self.ENTORNO_QUE_NO_SE_DOCUMENTA
        # Guardia contra un falso verde: si la expresión regular dejara de encontrar variables, la comparación de abajo
        # pasaría sin comprobar nada. (CLOUDINARY_URL no aparece aquí: la lee el SDK de Cloudinary, no settings.py.)
        self.assertTrue({"SECRET_KEY", "DEBUG", "DATABASE_URL", "HEALTH_TOKEN", "ALLOWED_HOSTS"} <= leidas)
        self.assertEqual(set(), leidas - set(self._variables_de_env_example()))

    def test_env_example_no_contiene_secretos_reales(self):
        v = self._variables_de_env_example()
        self.assertEqual("", v["SECRET_KEY"])
        self.assertEqual("", v["HEALTH_TOKEN"])
        self.assertIn("CONTRASENA", v["DATABASE_URL"])
        self.assertIn("SECRETO_API", v["CLOUDINARY_URL"])

    def test_el_diagnostico_reconoce_las_plantillas_de_env_example(self):
        # Si alguien cambia los marcadores de .env.example y no los del diagnóstico, esta prueba lo avisa: el usuario
        # que olvide reemplazarlos dejaría de recibir el mensaje preciso.
        from api import servicios

        v = self._variables_de_env_example()
        self.assertTrue(any(m in v["DATABASE_URL"] for m in servicios._PLANTILLA_BD))
        self.assertTrue(any(m in v["CLOUDINARY_URL"] for m in servicios._PLANTILLA_CLOUDINARY))

    def test_render_yaml_declara_los_secretos_sin_valor(self):
        bloques = self._bloques_de_render_yaml()
        for nombre in ("DATABASE_URL", "CLOUDINARY_URL", "HEALTH_TOKEN"):
            self.assertIn(nombre, bloques, f"render.yaml no declara {nombre}")
            self.assertIn("sync: false", bloques[nombre], f"{nombre} debe pedirse en el panel (sync: false)")
            self.assertIsNone(re.search(r"^\s+value:", bloques[nombre], re.MULTILINE), f"{nombre} NO puede llevar valor")

    def test_render_yaml_arranca_gunicorn_donde_render_lo_espera(self):
        import importlib

        texto = (RAIZ / "render.yaml").read_text(encoding="utf-8")
        comando = re.search(r"^\s*startCommand:\s*(.+)$", texto, re.MULTILINE).group(1)
        self.assertIn("wanchaq_limpio.wsgi:application", comando)
        # Render documenta PORT como opcional (10000 por defecto): sin el valor por defecto, si faltara, gunicorn
        # recibiría "0.0.0.0:" y no arrancaría.
        self.assertIn("0.0.0.0:${PORT:-10000}", comando)
        self.assertRegex(texto, r"(?m)^\s*rootDir:\s*backend\s*$")
        self.assertRegex(texto, r"(?m)^\s*healthCheckPath:\s*/health\s*$")
        # El destino del comando de arranque existe y es invocable (lo que gunicorn importará en Render).
        self.assertTrue(callable(importlib.import_module("wanchaq_limpio.wsgi").application))

    def test_render_yaml_instala_dependencias_y_aplica_las_migraciones_al_construir(self):
        texto = (RAIZ / "render.yaml").read_text(encoding="utf-8")
        comando = re.search(r"^\s*buildCommand:\s*(.+)$", texto, re.MULTILINE).group(1)
        self.assertIn("pip install -r requirements.txt", comando)
        self.assertIn("python manage.py migrate --noinput", comando)  # sin migrate, POST /reportes daría 500 (no hay tablas)
        # `&&`: si la instalación falla no se intenta migrar, y si migrar falla la construcción falla (no queda a medias)
        self.assertLess(comando.index("pip install"), comando.index("manage.py migrate"))
        self.assertIn("&&", comando)

    def test_render_yaml_fija_python_completo_y_compatible_con_django(self):
        texto = (RAIZ / "render.yaml").read_text(encoding="utf-8")
        version = re.search(r"key: PYTHON_VERSION\s+value:\s*(\d+)\.(\d+)\.(\d+)\s*$", texto, re.MULTILINE)
        self.assertIsNotNone(version, "PYTHON_VERSION debe ser x.y.z completa: así lo exige Render en esa variable")
        self.assertGreaterEqual((int(version[1]), int(version[2])), (3, 12), "Django 6.1 exige Python >= 3.12")

    def test_todas_las_dependencias_estan_fijadas_a_una_version_exacta(self):
        lineas = [
            l.strip() for l in (BACKEND / "requirements.txt").read_text(encoding="utf-8").splitlines()
            if l.strip() and not l.strip().startswith("#")
        ]
        self.assertTrue(lineas)
        for linea in lineas:
            self.assertRegex(linea, r"^[A-Za-z0-9_.\[\]-]+==\d[\w.]*$", f"{linea!r}: lo que se probó es lo que se despliega")

    def test_render_yaml_genera_la_secret_key_y_no_activa_debug(self):
        bloques = self._bloques_de_render_yaml()
        self.assertIn("generateValue: true", bloques["SECRET_KEY"])
        self.assertNotIn("DEBUG", bloques)

    def test_env_esta_ignorado_por_git(self):
        if shutil.which("git") is None:
            self.skipTest("git no está instalado")
        p = subprocess.run(["git", "check-ignore", "-q", "backend/.env"], cwd=RAIZ)
        self.assertEqual(p.returncode, 0, "backend/.env debe estar en .gitignore")


class MigracionesTest(TestCase):
    def test_las_migraciones_estan_al_dia_con_los_modelos(self):
        # Si alguien cambia models.py y olvida `makemigrations`, el despliegue migraría un esquema distinto del código
        # (y Render aplica `migrate` en cada construcción).
        salida = io.StringIO()
        try:
            call_command("makemigrations", "api", check=True, dry_run=True, stdout=salida, stderr=salida)
        except SystemExit:
            self.fail("models.py cambió sin generar migración. Ejecuta `python manage.py makemigrations api`:\n" + salida.getvalue())

    def test_la_migracion_inicial_crea_las_dos_tablas_del_contrato(self):
        from django.db import connection

        nombres = set(connection.introspection.table_names())
        self.assertLessEqual({"reporte", "reporte_eliminado"}, nombres)

    def test_la_clave_natural_es_unica_en_la_base_de_datos_no_solo_en_el_codigo(self):
        from django.db import IntegrityError, transaction

        from api.models import Reporte

        datos = dict(
            id_anonimo="3f2b8c1e-5a7d-4e0b-9c64-1d2e3f4a5b6c", id_local=1, categoria="MIXTO",
            fecha_hora="2026-09-18T15:00:00Z", latitud=-13.53, longitud=-71.9475, foto_public_id="x",
        )
        Reporte.objects.create(**datos)
        with self.assertRaises(IntegrityError), transaction.atomic():
            Reporte.objects.create(**datos)

    def test_la_base_rechaza_valores_fuera_del_contrato_aunque_el_codigo_los_dejara_pasar(self):
        # Defensa en profundidad: las CHECK constraints protegen los datos si algún día se guarda por otra vía.
        from django.db import IntegrityError, transaction

        from api.models import Reporte

        base = dict(
            id_anonimo="3f2b8c1e-5a7d-4e0b-9c64-1d2e3f4a5b6c", id_local=1, categoria="MIXTO",
            fecha_hora="2026-09-18T15:00:00Z", latitud=-13.53, longitud=-71.9475, foto_public_id="x",
        )
        invalidos = [
            {"categoria": "OTRO"}, {"latitud": 95.0}, {"longitud": -200.0},
            {"categoria_ia": "ESCOMBROS", "confianza_ia": 0.5}, {"categoria_ia": "VIDRIO", "confianza_ia": 1.5},
        ]
        for cambio in invalidos:
            with self.subTest(cambio=cambio), self.assertRaises(IntegrityError), transaction.atomic():
                Reporte.objects.create(**{**base, **cambio})
