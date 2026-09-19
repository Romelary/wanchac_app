"""Configuración del backend de Wanchaq Limpio.

TODO lo sensible (claves, contraseñas, cadenas de conexión) viene de variables de entorno; nada de eso se escribe
en el código ni se sube a git. En desarrollo se leen de `backend/.env` (archivo NO versionado, ver `.env.example`);
en Render no existe ese archivo: allí las define el panel (o `render.yaml`, que solo declara sus NOMBRES).

Las decisiones que no son obvias están explicadas en `docs/adr/0004-backend-django-sin-geodjango.md`.
"""

import os
from pathlib import Path
from urllib.parse import urlparse

import dj_database_url
from django.core.exceptions import ImproperlyConfigured

from wanchaq_limpio.entorno import cargar_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent

# Las pruebas de configuración necesitan un entorno "limpio" (sin lo que haya en tu .env): NO_CARGAR_DOTENV=1.
if not os.environ.get("NO_CARGAR_DOTENV"):
    cargar_dotenv(BASE_DIR / ".env")


def _booleano(nombre, defecto=False):
    return os.environ.get(nombre, str(defecto)).strip().lower() in {"1", "true", "yes", "si", "sí"}


DEBUG = _booleano("DEBUG", False)

# --- Clave secreta -------------------------------------------------------------------------------------------------
SECRET_KEY = os.environ.get("SECRET_KEY", "")
if not SECRET_KEY:
    if DEBUG:
        SECRET_KEY = "solo-para-desarrollo-no-usar-jamas-en-produccion"
    else:
        raise ImproperlyConfigured("Falta la variable de entorno SECRET_KEY.")

# --- Hosts permitidos ----------------------------------------------------------------------------------------------
ALLOWED_HOSTS = [h.strip() for h in os.environ.get("ALLOWED_HOSTS", "").split(",") if h.strip()]
if os.environ.get("RENDER_EXTERNAL_HOSTNAME"):  # Render la define sola: p. ej. wanchaq-limpio-api.onrender.com
    ALLOWED_HOSTS.append(os.environ["RENDER_EXTERNAL_HOSTNAME"])
if DEBUG:
    ALLOWED_HOSTS += ["localhost", "127.0.0.1"]

# --- Base de datos (Neon: PostgreSQL + PostGIS) ----------------------------------------------------------------------
DATABASE_URL = os.environ.get("DATABASE_URL", "")
if DATABASE_URL:
    _es_local = urlparse(DATABASE_URL).hostname in {"localhost", "127.0.0.1"}
    _base = dj_database_url.parse(
        DATABASE_URL,
        conn_max_age=60,
        conn_health_checks=True,  # Neon se duerme a los 5 min: sin esto, una conexión reutilizada fallaría al despertar
        disable_server_side_cursors=True,  # recomendado por la guía de Django de Neon
        ssl_require=not _es_local,  # Neon exige TLS
    )
    # Sin límite, libpq espera INDEFINIDAMENTE si el servidor no contesta (comprobado: en Windows ni siquiera detecta un
    # puerto cerrado; en Render bastaría una red que descarte paquetes) y gunicorn acabaría matando el proceso (502).
    # 15 s cubren de sobra el despertar de Neon (unos segundos) y quedan muy por debajo del --timeout 60 de gunicorn.
    _base.setdefault("OPTIONS", {}).setdefault("connect_timeout", 15)
    DATABASES = {"default": _base}
elif DEBUG:
    DATABASES = {"default": {"ENGINE": "django.db.backends.sqlite3", "NAME": BASE_DIR / "db.sqlite3"}}
else:
    # El disco de Render (plan gratuito) se borra en cada reinicio: SQLite "funcionaría" y perdería los datos EN SILENCIO.
    raise ImproperlyConfigured("Falta DATABASE_URL: sin ella no hay dónde guardar los reportes.")

# --- Cloudinary ----------------------------------------------------------------------------------------------------
# El SDK lee CLOUDINARY_URL (cloudinary://clave:secreto@nombre_de_nube) por sí solo; aquí no se toca esa variable.

# --- Diagnóstico de infraestructura ---------------------------------------------------------------------------------
# Token para /health/servicios (comprueba base de datos y Cloudinary). Si está vacío, ese endpoint no existe.
HEALTH_TOKEN = os.environ.get("HEALTH_TOKEN", "")

# --- Django --------------------------------------------------------------------------------------------------------
# Solo "api": aún no hay modelos, y no se instalan contrib.auth/admin/sessions porque la app no tiene login (ADR-0001)
# y así `migrate` no crea en Neon tablas que nadie usa (0,5 GB de cuota).
INSTALLED_APPS = [
    "api",
]
MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.middleware.common.CommonMiddleware",
]
ROOT_URLCONF = "wanchaq_limpio.urls"
WSGI_APPLICATION = "wanchaq_limpio.wsgi.application"
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# Render termina el TLS en su proxy y reenvía por HTTP: sin esto Django creería que la petición no es segura.
SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")

LANGUAGE_CODE = "es"
TIME_ZONE = "America/Lima"
USE_TZ = True
