#!/usr/bin/env python
"""Verifica que las cuentas de Neon (base de datos) y Cloudinary (fotos) están bien configuradas.

Uso, desde la carpeta `backend` y con el entorno virtual activado (ver docs/despliegue.md):

    python verificar_servicios.py
        Comprueba las variables de backend/.env, la conexión a Neon (con PostGIS operativo) y las credenciales de
        Cloudinary.

    python verificar_servicios.py --subida
        Además sube una imagen de 1x1 px como recurso PRIVADO, comprueba que su URL pública NO la sirve y que la URL
        firmada SÍ, y la borra. Es la prueba de que las fotos de los reportes no quedarán expuestas.

    python verificar_servicios.py --remoto https://wanchaq-limpio-api.onrender.com
        Verifica el servicio YA desplegado en Render (usa el HEALTH_TOKEN de backend/.env). La primera llamada puede
        tardar ~1 minuto: el plan gratuito de Render duerme el servicio tras 15 minutos sin tráfico.

SEGURIDAD: imprime únicamente "OK"/"FALLO"/"AVISO" y textos fijos. Nunca imprime el valor de una variable ni el
mensaje de error de un servicio (ver api/servicios.py), así que su salida completa se puede pegar en el chat.
Devuelve código de salida 0 si todo está bien y 1 si algo falla.
"""

import argparse
import base64
import json
import os
import secrets
import struct
import sys
import time
import urllib.error
import urllib.request
import zlib
from pathlib import Path
from urllib.parse import urlparse

BACKEND = Path(__file__).resolve().parent
TIMEOUT_ARRANQUE_EN_FRIO_S = 120  # Render tarda ~1 min en despertar un servicio dormido; Neon unos segundos más.


def _linea(nivel, texto):
    print(f"{nivel:<6}{texto}")
    return nivel == "OK"


def ok(texto):
    return _linea("OK", texto)


def fallo(texto):
    return _linea("FALLO", texto)


def aviso(texto):
    _linea("AVISO", texto)


def png_minimo():
    """Un PNG válido de 1x1 píxel, construido a mano para no depender de ninguna librería de imágenes."""

    def trozo(tipo, datos):
        return struct.pack(">I", len(datos)) + tipo + datos + struct.pack(">I", zlib.crc32(tipo + datos) & 0xFFFFFFFF)

    cabecera = struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0)  # 1x1, 8 bits por canal, RGB
    fila = b"\x00" + bytes([0x1F, 0x9D, 0x6B])  # byte de filtro 0 + un píxel verde
    return b"\x89PNG\r\n\x1a\n" + trozo(b"IHDR", cabecera) + trozo(b"IDAT", zlib.compress(fila)) + trozo(b"IEND", b"")


# --- Verificación local ---------------------------------------------------------------------------------------------
def _iniciar_django():
    """Carga los ajustes (y con ellos backend/.env). Devuelve True si arrancaron."""
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "wanchaq_limpio.settings")
    sys.path.insert(0, str(BACKEND))
    try:
        import django

        django.setup()
    except Exception as exc:  # noqa: BLE001
        # Los mensajes de ImproperlyConfigured los escribimos nosotros en settings.py (texto fijo, sin secretos); el de
        # cualquier otra excepción podría contener parte de una cadena de conexión mal formada, así que solo su nombre.
        from django.core.exceptions import ImproperlyConfigured

        detalle = str(exc) if isinstance(exc, ImproperlyConfigured) else f"{type(exc).__name__} (revisa backend/.env)"
        return fallo(f"los ajustes no arrancan: {detalle}")
    return True


def _variables():
    existe = (BACKEND / ".env").exists()
    resultado = ok("backend/.env encontrado") if existe else fallo("no existe backend/.env (cópialo de .env.example)")
    for nombre in ("DATABASE_URL", "CLOUDINARY_URL", "HEALTH_TOKEN"):
        presente = bool(os.environ.get(nombre))
        resultado &= ok(f"{nombre} definida") if presente else fallo(f"{nombre} está vacía o no existe")
    from django.conf import settings

    host = urlparse(os.environ.get("DATABASE_URL", "")).hostname or ""
    if "-pooler" in host:
        aviso("DATABASE_URL usa la cadena 'pooled' de Neon; se recomienda la DIRECTA (sin '-pooler') para migraciones")
    if settings.DEBUG:
        aviso("DEBUG=1 (correcto en tu máquina; en Render NO debe definirse)")
    return resultado


def _subida_privada():
    """Sube 1x1 px como 'authenticated', comprueba que no es pública, que la firmada sí sirve, y la borra."""
    import cloudinary
    import cloudinary.uploader
    import cloudinary.utils

    public_id = f"wanchaq_limpio/_verificacion/prueba_{secrets.token_hex(4)}"
    datos = "data:image/png;base64," + base64.b64encode(png_minimo()).decode()
    try:
        r = cloudinary.uploader.upload(datos, public_id=public_id, type="authenticated", overwrite=True)
    except Exception as exc:  # noqa: BLE001
        return fallo(f"Cloudinary rechazó la subida de prueba ({type(exc).__name__})")
    resultado = True
    try:
        resultado &= (
            ok("subida aceptada y guardada como privada (type=authenticated)")
            if r.get("type") == "authenticated"
            else fallo("el recurso NO quedó como 'authenticated': las fotos serían públicas")
        )
        nube = cloudinary.config().cloud_name
        url_publica = f"https://res.cloudinary.com/{nube}/image/upload/v{r['version']}/{public_id}.png"
        estado = _estado_http(url_publica)
        resultado &= (
            ok(f"la URL pública NO sirve la foto (HTTP {estado})")
            if estado != 200
            else fallo("la URL pública SÍ sirve la foto: las fotos de los reportes quedarían expuestas")
        )
        url_firmada, _ = cloudinary.utils.cloudinary_url(
            public_id, type="authenticated", sign_url=True, secure=True, format="png", version=r["version"]
        )
        estado = _estado_http(url_firmada)
        resultado &= (
            ok("la URL firmada SÍ sirve la foto (así la verá el servidor/administrador)")
            if estado == 200
            else fallo(f"la URL firmada no sirvió la foto (HTTP {estado})")
        )
    finally:  # aunque una comprobación falle, no se deja basura en la cuenta
        try:
            borrado = cloudinary.uploader.destroy(public_id, type="authenticated", invalidate=True)
            resultado &= ok("recurso de prueba borrado") if borrado.get("result") == "ok" else fallo(
                "no se pudo borrar el recurso de prueba (bórralo a mano en la consola de Cloudinary)"
            )
        except Exception as exc:  # noqa: BLE001
            resultado &= fallo(f"no se pudo borrar el recurso de prueba ({type(exc).__name__})")
    return resultado


def _estado_http(url):
    """Código HTTP de un GET (sin descargar más de lo necesario); 0 si no hubo respuesta."""
    try:
        with urllib.request.urlopen(url, timeout=30) as respuesta:  # noqa: S310 - URL https construida aquí
            return respuesta.status
    except urllib.error.HTTPError as exc:
        return exc.code
    except OSError:
        return 0


def verificar_local(con_subida):
    if not _iniciar_django():
        return 1
    from api import servicios

    todo_ok = _variables()
    bd = servicios.comprobar_base_de_datos()
    todo_ok &= ok(f"Neon: {bd['detalle']}") if bd["ok"] else fallo(f"Neon: {bd['detalle']}")
    cl = servicios.comprobar_cloudinary()
    todo_ok &= ok(f"Cloudinary: {cl['detalle']}") if cl["ok"] else fallo(f"Cloudinary: {cl['detalle']}")
    if con_subida:
        if cl["ok"]:
            todo_ok &= _subida_privada()
        else:
            aviso("se omite --subida porque las credenciales de Cloudinary no son válidas")
            todo_ok = False
    return _resumen(todo_ok)


# --- Verificación del servicio desplegado ---------------------------------------------------------------------------
def _es_direccion_segura(base):
    partes = urlparse(base)
    return partes.scheme == "https" or (partes.scheme == "http" and partes.hostname in {"localhost", "127.0.0.1"})


def _pedir(url, cabeceras=None):
    """GET que devuelve (codigo_http, cuerpo_json_o_None, segundos). El código es 0 si no hubo respuesta."""
    inicio = time.monotonic()
    peticion = urllib.request.Request(url, headers=cabeceras or {})
    try:
        with urllib.request.urlopen(peticion, timeout=TIMEOUT_ARRANQUE_EN_FRIO_S) as respuesta:  # noqa: S310
            codigo, cuerpo = respuesta.status, respuesta.read()
    except urllib.error.HTTPError as exc:
        codigo, cuerpo = exc.code, exc.read()
    except OSError:
        return 0, None, time.monotonic() - inicio
    try:
        datos = json.loads(cuerpo)
    except ValueError:
        datos = None
    return codigo, datos, time.monotonic() - inicio


def verificar_remoto(base):
    if not _es_direccion_segura(base):
        return _resumen(fallo("la dirección debe empezar por https:// (el token no debe viajar sin cifrar)"))
    base = base.rstrip("/")
    sys.path.insert(0, str(BACKEND))
    from django.core.exceptions import ImproperlyConfigured
    from wanchaq_limpio.entorno import cargar_dotenv

    try:
        cargar_dotenv(BACKEND / ".env")
    except ImproperlyConfigured as exc:  # mensaje propio, sin secretos
        return _resumen(fallo(str(exc)))
    codigo, _, segundos = _pedir(f"{base}/health")
    if codigo != 200:
        return _resumen(fallo(f"/health no respondió 200 (código {codigo}); ¿el despliegue terminó? Revisa los logs en Render"))
    ok(f"/health responde ({segundos:.0f} s)")
    if segundos > 15:
        aviso("tardó porque el servicio estaba dormido (arranque en frío del plan gratuito); es lo esperado")
    token = os.environ.get("HEALTH_TOKEN", "")
    if not token:
        return _resumen(fallo("falta HEALTH_TOKEN en backend/.env para consultar /health/servicios"))
    codigo, datos, _ = _pedir(f"{base}/health/servicios", {"X-Health-Token": token})
    if codigo == 401:
        return _resumen(fallo("token rechazado: el HEALTH_TOKEN de backend/.env no coincide con el del panel de Render"))
    if codigo == 404:
        return _resumen(fallo("/health/servicios no existe: falta la variable HEALTH_TOKEN en el panel de Render"))
    if not isinstance(datos, dict) or "servicios" not in datos:
        return _resumen(fallo(f"respuesta inesperada de /health/servicios (código {codigo})"))
    todo_ok = True
    for nombre, estado in datos["servicios"].items():
        texto = f"{nombre} (en Render): {estado.get('detalle', '')}"
        todo_ok &= ok(texto) if estado.get("ok") else fallo(texto)
    return _resumen(todo_ok)


def _resumen(todo_ok):
    print()
    print("RESULTADO: TODO OK" if todo_ok else "RESULTADO: HAY FALLOS (revisa las líneas FALLO de arriba)")
    return 0 if todo_ok else 1


def main(argv=None):
    if hasattr(sys.stdout, "reconfigure"):
        # UTF-8 explícito: con la salida redirigida, Windows usaría cp1252 y las tildes se verían mal al pegarlas.
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Verifica las cuentas de Neon y Cloudinary (y el servicio en Render).")
    parser.add_argument("--subida", action="store_true", help="prueba una subida PRIVADA de 1x1 px a Cloudinary")
    parser.add_argument("--remoto", metavar="URL", help="verifica el servicio desplegado en Render, p. ej. https://...")
    args = parser.parse_args(argv)
    return verificar_remoto(args.remoto) if args.remoto else verificar_local(args.subida)


if __name__ == "__main__":
    sys.exit(main())
