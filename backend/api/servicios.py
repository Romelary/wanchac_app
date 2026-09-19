"""Comprobaciones de los servicios externos (base de datos en Neon y fotos en Cloudinary).

Las usan dos consumidores:
  * `GET /health/servicios` (protegido con token), para verificar el servicio YA desplegado en Render, donde el plan
    gratuito no ofrece consola ni SSH;
  * `backend/verificar_servicios.py`, para verificar TU máquina antes de subir nada.

Regla de oro: el resultado es un texto corto y de una lista cerrada. NUNCA se copia el mensaje de una excepción, porque
los mensajes de los clientes de base de datos y de red pueden incluir el host, el usuario o parte de la cadena de
conexión, y esta salida se pega en chats y en documentos de tesis. De la excepción solo se informa su NOMBRE de clase.
(Se puede INSPECCIONAR su mensaje para elegir entre textos fijos —p. ej. "disabled api_key"—, pero jamás se copia.)
Tampoco se imprimen valores de claves: solo pistas sobre su FORMA (longitud, símbolos), que ayudan a encontrar un error
de copiado sin exponer nada.
"""

import re

from django.db import connection


# Restos de las PLANTILLAS que el usuario copia y olvida reemplazar: la de `.env.example` y la que muestra la consola de
# Cloudinary (`cloudinary://<your_api_key>:<your_api_secret>@nube`, que lleva los símbolos < > de "rellena aquí").
# Reconocerlas da un mensaje preciso en vez de un genérico "no se pudo conectar / no aceptó las credenciales", y evita
# una llamada de red que se sabe que fallará. Una prueba vigila que sigan coincidiendo con `.env.example`.
_PLANTILLA_BD = ("HOST_DE_NEON", "CONTRASENA", "NOMBRE_DE_BASE")
_PLANTILLA_CLOUDINARY = ("<", ">", "your_api", "CLAVE_API", "SECRETO_API", "NOMBRE_DE_NUBE")


def _resultado(ok, detalle):
    return {"ok": ok, "detalle": detalle}


def _contiene_alguna(valores, marcas):
    return any(marca in str(valor) for valor in valores for marca in marcas)


_SECRETO_CLOUDINARY = re.compile(r"[A-Za-z0-9_-]+")


def _secreto_enmascarado(secreto):
    return "*" in str(secreto) or "•" in str(secreto)  # asteriscos o punto medio "•" que usa la consola


def _observaciones_de_forma(configuracion):
    """Pistas sobre la FORMA de la clave y del secreto (longitud, símbolos). Nunca se incluyen sus valores.

    Las claves de Cloudinary tienen 15 dígitos y sus secretos 27 caracteres alfanuméricos (con - y _): así eran las del
    par que tenía el usuario al configurar. Son pistas, no reglas: por eso dicen "suele", por si Cloudinary cambiara el
    formato.
    """
    clave, secreto = str(configuracion.api_key), str(configuracion.api_secret)
    pistas = []
    if not (clave.isascii() and clave.isdigit() and len(clave) == 15):
        extra = "" if clave.isdigit() else " y no todos son dígitos"
        pistas.append(f"la clave de API suele ser de 15 dígitos (la tuya tiene {len(clave)} caracteres{extra})")
    if not (len(secreto) == 27 and _SECRETO_CLOUDINARY.fullmatch(secreto)):
        extra = "" if _SECRETO_CLOUDINARY.fullmatch(secreto) else ", con símbolos o espacios inesperados"
        pistas.append(f"el secreto suele tener 27 caracteres alfanuméricos (el tuyo tiene {len(secreto)}{extra})")
    return pistas


def comprobar_base_de_datos():
    """Conecta, y si el motor es PostgreSQL comprueba además que PostGIS está habilitado y que `geography` funciona."""
    ajustes = connection.settings_dict
    if _contiene_alguna((ajustes.get(campo) or "" for campo in ("HOST", "USER", "PASSWORD", "NAME")), _PLANTILLA_BD):
        return _resultado(
            False, "DATABASE_URL todavía tiene los marcadores de .env.example (HOST_DE_NEON, CONTRASENA…): pega la cadena real de Neon"
        )
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
    try:
        import cloudinary
        import cloudinary.api

        configuracion = cloudinary.config()
    except ValueError:
        # El SDK VALIDA la URL al importarse y lanza ValueError si no empieza por "cloudinary://". Es lo que ocurre al
        # pegar la línea entera dentro del valor (CLOUDINARY_URL=CLOUDINARY_URL=cloudinary://…), con una URL https:// o
        # con comillas incluidas en el valor. Sin este `except` el usuario vería un traceback (y /health/servicios un
        # error 500) en vez de la explicación. No se copia el mensaje de la excepción (regla de oro de este módulo).
        return _resultado(
            False,
            "CLOUDINARY_URL mal formada: debe empezar por cloudinary:// (¿repetiste 'CLOUDINARY_URL=' dentro del valor, "
            "o lleva comillas?)",
        )
    if not (configuracion.cloud_name and configuracion.api_key and configuracion.api_secret):
        return _resultado(False, "CLOUDINARY_URL ausente o mal formada (debe ser cloudinary://clave:secreto@nombre)")
    if _contiene_alguna((configuracion.cloud_name, configuracion.api_key, configuracion.api_secret), _PLANTILLA_CLOUDINARY):
        return _resultado(
            False, "CLOUDINARY_URL todavía tiene una plantilla (p. ej. <your_api_key>): reemplaza clave y secreto por los reales, sin los símbolos < >"
        )
    if _secreto_enmascarado(configuracion.api_secret):
        return _resultado(
            False,
            "CLOUDINARY_URL lleva el secreto OCULTO (asteriscos): se copió el texto de la pantalla, no el secreto real; "
            "usa el botón de copiar de la consola (o revélalo) y vuelve a pegarlo",
        )
    try:
        cloudinary.api.ping()
    except Exception as exc:  # noqa: BLE001 - se informa el NOMBRE de la excepción, nunca su mensaje
        nombre = type(exc).__name__
        if nombre != "AuthorizationRequired":
            return _resultado(False, f"Cloudinary no aceptó las credenciales ({nombre})")
        # 401. Cloudinary distingue tres causas en el mensaje (medido el 2026-09-18 con credenciales FALSAS contra su nube
        # pública de demostración, sin tocar ninguna cuenta): "unknown api_key" (la clave no existe), "disabled api_key"
        # (existe pero está desactivada) y "api_secret mismatch" (la clave existe pero el secreto no le corresponde).
        # Se inspecciona el mensaje SOLO para elegir uno de estos textos fijos; nunca se copia.
        mensaje = str(exc)
        if "unknown api_key" in mensaje:
            causa = "Cloudinary no conoce esa clave de API (¿mal copiada, de otra cuenta, o del par que se borró?)"
        elif "disabled api_key" in mensaje:
            causa = "esa clave de API existe pero está desactivada (¿el par que desactivaste?)"
        elif "api_secret mismatch" in mensaje:
            causa = "la clave existe, pero el SECRETO no le corresponde (¿copiado incompleto, o de otro par de claves?)"
        else:
            causa = "la clave, el secreto o el nombre de la nube no coinciden entre sí"
        detalle = f"Cloudinary rechazó las credenciales (401 {nombre}): {causa}"
        pistas = _observaciones_de_forma(configuracion)
        if pistas:
            detalle += ". Pistas sobre la forma: " + "; ".join(pistas)
        return _resultado(False, detalle)
    return _resultado(True, "credenciales aceptadas")


def comprobar_todo():
    """Ejecuta todas las comprobaciones. `ok` global = todas OK."""
    servicios = {
        "base_de_datos": comprobar_base_de_datos(),
        "cloudinary": comprobar_cloudinary(),
    }
    return {"ok": all(s["ok"] for s in servicios.values()), "servicios": servicios}
