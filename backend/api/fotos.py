"""Fotos de los reportes en Cloudinary, siempre PRIVADAS (`type=authenticated`, ADR-0004 #11).

El `public_id` es un hash determinista de la clave natural del reporte y NO contiene el seudónimo del dispositivo: los
identificadores de Cloudinary aparecen en las URL firmadas que verá quien administre el estudio, y `idAnonimo` funciona
como credencial de capacidad para la baja (ADR-0002, docs/diseno.md §4.2). Determinista => subir dos veces la misma foto
la SOBRESCRIBE en vez de duplicarla (idempotencia del reenvío).

Como en `servicios.py`, de una excepción solo se registra su NOMBRE, nunca su mensaje.
"""

import hashlib
import io
import logging

logger = logging.getLogger("api")

CARPETA = "wanchaq_limpio/reportes"


class ServicioNoDisponible(Exception):
    """Cloudinary no respondió o rechazó la operación. El cliente debe reintentar (la app trata todo no-2xx así)."""


def public_id_de(id_anonimo, id_local):
    huella = hashlib.sha256(f"{id_anonimo}:{id_local}".encode()).hexdigest()[:32]
    return f"{CARPETA}/{huella}"


def subir(contenido, public_id):
    """Sube los bytes de la foto como recurso privado. Sobrescribe si ya existía."""
    import cloudinary.uploader

    try:
        cloudinary.uploader.upload(
            io.BytesIO(contenido),
            public_id=public_id,
            type="authenticated",
            resource_type="image",
            overwrite=True,
            allowed_formats=["jpg"],
        )
    except Exception as exc:  # noqa: BLE001 - solo el NOMBRE de la excepción
        logger.warning("Cloudinary no aceptó la subida (%s)", type(exc).__name__)
        raise ServicioNoDisponible from None


def borrar(public_id):
    """Borra la foto. Que ya no existiera cuenta como éxito (la baja es idempotente)."""
    import cloudinary.uploader

    try:
        resultado = cloudinary.uploader.destroy(public_id, type="authenticated", invalidate=True)
    except Exception as exc:  # noqa: BLE001
        logger.warning("Cloudinary no pudo borrar (%s)", type(exc).__name__)
        raise ServicioNoDisponible from None
    if resultado.get("result") not in ("ok", "not found"):
        logger.warning("Cloudinary respondió de forma inesperada al borrar")
        raise ServicioNoDisponible
