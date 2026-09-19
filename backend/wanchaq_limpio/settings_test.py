"""Ajustes SOLO para `python manage.py test` (manage.py los elige solo).

Motivo: el ejecutor de pruebas de Django crea y borra una base de datos de prueba. Si usara los ajustes normales y tu
`.env` apuntara a Neon, las pruebas intentarían tocar la base real. Aquí el entorno se aísla por completo: no se lee
`.env`, no se usa DATABASE_URL y la base es SQLite en memoria.
"""

import os

os.environ["NO_CARGAR_DOTENV"] = "1"
os.environ["DEBUG"] = "1"
os.environ["DATABASE_URL"] = ""
os.environ["HEALTH_TOKEN"] = ""  # cada prueba fija el suyo con override_settings

from wanchaq_limpio.settings import *  # noqa: E402,F401,F403

DATABASES = {"default": {"ENGINE": "django.db.backends.sqlite3", "NAME": ":memory:"}}

# El servidor HTTP de LiveServerTestCase envuelve la aplicación en un manejador de archivos estáticos que exige STATIC_URL
# (su valor por defecto es None y falla). La aplicación real no sirve estáticos, por eso solo se define aquí.
STATIC_URL = "/static/"
