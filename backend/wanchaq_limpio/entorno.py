"""Carga del archivo `.env` local (solo desarrollo; en Render las variables las define el panel)."""

from django.core.exceptions import ImproperlyConfigured
from dotenv import load_dotenv


def cargar_dotenv(ruta):
    """Vuelca `ruta` en `os.environ` SIN pisar las variables que ya existan. Devuelve True si el archivo existía.

    Un `.env` que no esté en UTF-8 da un error claro. En Windows es fácil que ocurra: `echo X=1 > .env` en PowerShell
    5.1 y la opción "Unicode" del Bloc de notas lo guardan en UTF-16, y sin este aviso el fallo sería un
    `UnicodeDecodeError` sin ninguna pista de qué archivo ni qué hacer. (Un BOM de UTF-8 sí se tolera: python-dotenv
    1.x lo descarta por sí mismo; una prueba lo vigila por si una versión futura dejara de hacerlo.)
    """
    try:
        return load_dotenv(ruta, encoding="utf-8")
    except UnicodeDecodeError as exc:
        raise ImproperlyConfigured(
            "backend/.env no está guardado como UTF-8 (¿lo creaste con '>' de PowerShell o lo guardaste como "
            "'Unicode'?). Ábrelo con el Bloc de notas y usa Guardar como... -> Codificación: UTF-8."
        ) from exc
