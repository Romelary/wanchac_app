"""Comprobaciones de los servicios externos (base de datos en Neon y fotos en Cloudinary).

Las usan dos consumidores:
  * `GET /health/servicios` (protegido con token), para verificar el servicio YA desplegado en Render, donde el plan
    gratuito no ofrece consola ni SSH;
  * `backend/verificar_servicios.py`, para verificar TU máquina antes de subir nada.

Regla de oro: el resultado es un texto corto y de una lista cerrada. NUNCA se copia el mensaje de una excepción, porque
los mensajes de los clientes de base de datos y de red pueden incluir el host, el usuario o parte de la cadena de
conexión, y esta salida se pega en chats y en documentos de tesis. De la excepción solo se informa su NOMBRE de clase.
"""

from django.db import connection


def _resultado(ok, detalle):
    return {"ok": ok, "detalle": detalle}


def comprobar_base_de_datos():
    """Conecta, y si el motor es PostgreSQL comprueba además que PostGIS está habilitado y que `geography` funciona."""
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
            if connection.vendor != "postgresql":
                return _resultado(False, "el motor no es PostgreSQL (¿falta DATABASE_URL?)")
            try:
                cursor.execute("SELECT postgis_version()")
            except Exception:  # noqa: BLE001 - la extensión no existe: es el fallo más probable en un Neon recién creado
                return _resultado(False, "PostgreSQL responde, pero PostGIS no está habilitado: ejecutar CREATE EXTENSION postgis;")
            version = str(cursor.fetchone()[0]).split()[0]
            # Dos puntos a ~11 m entre sí (0,0 y 0,0.0001 grados): comprueba que el tipo `geography` mide en metros.
            cursor.execute("SELECT ST_DWithin(ST_MakePoint(0, 0)::geography, ST_MakePoint(0, 0.0001)::geography, 50)")
            if cursor.fetchone()[0] is not True:
                return _resultado(False, f"PostGIS {version} responde, pero ST_DWithin sobre geography falló")
    except Exception as exc:  # noqa: BLE001 - se informa solo el NOMBRE de la excepción (ver la regla de oro arriba)
        return _resultado(False, f"no se pudo conectar ({type(exc).__name__})")
    return _resultado(True, f"conectado; PostGIS {version} operativo")


def comprobar_cloudinary():
    """Comprueba que CLOUDINARY_URL está bien formada y que Cloudinary acepta las credenciales (`api.ping`)."""
    # Importación diferida: el SDK lee CLOUDINARY_URL al importarse y `settings` debe haber cargado `.env` antes.
    import cloudinary
    import cloudinary.api

    configuracion = cloudinary.config()
    if not (configuracion.cloud_name and configuracion.api_key and configuracion.api_secret):
        return _resultado(False, "CLOUDINARY_URL ausente o mal formada (debe ser cloudinary://clave:secreto@nombre)")
    try:
        cloudinary.api.ping()
    except Exception as exc:  # noqa: BLE001 - solo el NOMBRE de la excepción
        return _resultado(False, f"Cloudinary no aceptó las credenciales ({type(exc).__name__})")
    return _resultado(True, "credenciales aceptadas")


def comprobar_todo():
    """Ejecuta todas las comprobaciones. `ok` global = todas OK."""
    servicios = {
        "base_de_datos": comprobar_base_de_datos(),
        "cloudinary": comprobar_cloudinary(),
    }
    return {"ok": all(s["ok"] for s in servicios.values()), "servicios": servicios}
