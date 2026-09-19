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

SEGURIDAD: imprime únicamente "OK"/"FALLO"/"AVISO"/"INFO" y textos fijos. Nunca imprime el valor de una variable ni
el mensaje de error de un servicio (ver api/servicios.py), así que su salida completa se puede pegar en el chat. La
única cifra parcial es, cuando Cloudinary falla, los 4 ÚLTIMOS dígitos de la clave de API (un identificador que sin el
secreto no autentica nada) para reconocerla en la consola; el secreto jamás se imprime, ni siquiera en parte.
Devuelve código de salida 0 si todo está bien y 1 si algo falla.
"""

import argparse
import base64
import io
import json
import os
import re
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
VARIABLES_DEL_PROYECTO = ("DEBUG", "SECRET_KEY", "DATABASE_URL", "CLOUDINARY_URL", "HEALTH_TOKEN", "ALLOWED_HOSTS")
# Zonas geográficas para comparar la región de Neon con la de Render SIN suponer a qué región de AWS equivale cada región
# de Render (Render no lo documenta: render.com/docs/regions). Regiones de Neon: neon.com/docs/introduction/regions.
ZONA_DE_NEON = {
    "us-east-1": "costa este de EE. UU.", "us-east-2": "costa este de EE. UU.", "us-west-2": "costa oeste de EE. UU.",
    "eu-central-1": "Europa central", "eu-west-2": "Reino Unido", "ap-southeast-1": "Singapur",
    "ap-southeast-2": "Sídney", "sa-east-1": "Sudamérica",
}
ZONA_DE_RENDER = {
    "virginia": "costa este de EE. UU.", "ohio": "costa este de EE. UU.", "oregon": "costa oeste de EE. UU.",
    "frankfurt": "Europa central", "singapore": "Singapur",
}


def _linea(nivel, texto):
    print(f"{nivel:<6}{texto}")
    return nivel == "OK"


def ok(texto):
    return _linea("OK", texto)


def fallo(texto):
    return _linea("FALLO", texto)


def aviso(texto):
    _linea("AVISO", texto)


def info(texto):
    _linea("INFO", texto)


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


def _leer_env():
    """Texto de backend/.env, o None si no existe o no está en UTF-8 (de eso ya se queja la carga de ajustes)."""
    try:
        return (BACKEND / ".env").read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return None


def _conflictos_con_el_entorno():
    """Variables definidas en backend/.env Y en el entorno de Windows, con valores DISTINTOS.

    La carga del .env no pisa las variables que ya existen (en Render manda el panel). En tu máquina eso significa que
    una variable puesta antes en PowerShell o en las variables de entorno de Windows GANA sobre el .env: corriges el
    archivo y "sigue el mismo error". Debe llamarse ANTES de cargar los ajustes, que mezclan el .env en os.environ.
    """
    texto = _leer_env()
    if texto is None:
        return []
    from dotenv import dotenv_values

    valores = dotenv_values(stream=io.StringIO(texto))
    return [n for n in VARIABLES_DEL_PROYECTO if valores.get(n) and os.environ.get(n) and valores[n] != os.environ[n]]


def _repetidas_en_env():
    """Variables que aparecen más de una vez en backend/.env: gana la ÚLTIMA, que suele ser una vieja olvidada."""
    nombres = re.findall(r"^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=", _leer_env() or "", re.MULTILINE)
    return [n for n in VARIABLES_DEL_PROYECTO if nombres.count(n) > 1]


def _avisos_de_origen():
    for nombre in _conflictos_con_el_entorno():
        aviso(
            f"{nombre} existe TAMBIÉN en el entorno de Windows con otro valor: se usa ESA, no la de backend/.env "
            "(ver docs/despliegue.md §10 para quitarla)"
        )
    for nombre in _repetidas_en_env():
        aviso(f"{nombre} aparece varias veces en backend/.env: se usa la ÚLTIMA (borra las viejas)")


def _region_de_neon():
    """Región de AWS del proyecto de Neon, leída del nombre del servidor (p. ej. 'us-east-2'); None si no se reconoce."""
    host = urlparse(os.environ.get("DATABASE_URL", "")).hostname or ""
    coincidencia = re.search(r"\.([a-z]{2}-[a-z]+-\d)\.aws\.neon\.tech$", host)
    return coincidencia.group(1) if coincidencia else None


def _region_de_render_yaml():
    try:
        texto = (BACKEND.parent / "render.yaml").read_text(encoding="utf-8")
    except OSError:
        return None
    coincidencia = re.search(r"^\s*region:\s*([a-z]+)\s*$", texto, re.MULTILINE)
    return coincidencia.group(1) if coincidencia else None


def _aviso_de_region():
    """Compara la zona de Neon con la de render.yaml: si son lejanas, cada consulta a la base paga esa distancia.

    Solo se imprime la REGIÓN (p. ej. us-east-2), que no es un secreto; nunca el nombre completo del servidor.
    """
    neon, render = _region_de_neon(), _region_de_render_yaml()
    zona_neon, zona_render = ZONA_DE_NEON.get(neon), ZONA_DE_RENDER.get(render)
    if zona_neon is None or zona_render is None:
        return  # región desconocida: no se opina
    if zona_neon == zona_render:
        info(f"Neon está en {neon} y render.yaml usa '{render}': misma zona ({zona_neon})")
        return
    opciones = [nombre for nombre, zona in ZONA_DE_RENDER.items() if zona == zona_neon]
    sugerencia = (
        f"cambia region: en render.yaml a una de estas: {', '.join(opciones)}"
        if opciones
        else "Render no tiene región en esa zona: elige la más cercana"
    )
    aviso(
        f"Neon está en {neon} ({zona_neon}) pero render.yaml usa '{render}' ({zona_render}): cada consulta a la base "
        f"cruzaría esa distancia. Antes de desplegar, {sugerencia} (una vez creado el servicio, su región no se puede cambiar)"
    )


def _final_de_la_clave_de_api():
    """Muestra SOLO los 4 últimos dígitos de la clave de API en uso, para reconocerla en la lista de la consola."""
    try:
        import cloudinary

        clave = str(cloudinary.config().api_key or "")
    except Exception:  # noqa: BLE001 - una URL mal formada ya se explicó en la comprobación anterior
        return
    if clave.isdigit() and len(clave) >= 8:
        info(
            f"la clave de API de Cloudinary en uso termina en ...{clave[-4:]}: debe ser la de la clave ACTIVA "
            "que ves en Settings → API Keys"
        )


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
    _aviso_de_region()
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
    _avisos_de_origen()  # ANTES de cargar los ajustes: estos mezclan el .env en os.environ
    if not _iniciar_django():
        return 1
    from api import servicios

    todo_ok = _variables()
    bd = servicios.comprobar_base_de_datos()
    todo_ok &= ok(f"Neon: {bd['detalle']}") if bd["ok"] else fallo(f"Neon: {bd['detalle']}")
    cl = servicios.comprobar_cloudinary()
    todo_ok &= ok(f"Cloudinary: {cl['detalle']}") if cl["ok"] else fallo(f"Cloudinary: {cl['detalle']}")
    if not cl["ok"]:
        _final_de_la_clave_de_api()
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
