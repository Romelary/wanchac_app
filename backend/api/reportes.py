"""Endpoints de reportes: `POST /reportes` y `DELETE /reportes/{idAnonimo}/{idLocal}` (docs/diseno.md §4.2).

Códigos de respuesta (la app trata TODO no-2xx como "reintentar", salvo 404 en la baja, que cuenta como cumplida):
  POST    201 creado · 200 ya recibido (idempotente) · 400 mal formado · 410 el reporte fue dado de baja ·
          413 foto demasiado grande · 415 la foto no es JPEG · 422 incumple una regla del estudio · 503 fallo transitorio
  DELETE  204 borrado · 404 no existía (la lápida se registra igual) · 503 fallo transitorio

Reglas de privacidad: `idAnonimo` nunca se devuelve en una respuesta ni se registra (ver `wanchaq_limpio/logs.py`), y los
mensajes de error son fijos: no repiten lo que envió el cliente.
"""

import logging

from django.conf import settings
from django.db import connection, transaction
from django.db import Error as ErrorDeBaseDeDatos
from django.http import HttpResponse, JsonResponse
from django.views.decorators.http import require_http_methods

from api import fotos
from api.models import Reporte, ReporteEliminado
from api.validacion import ErrorDeValidacion, validar_geojson

logger = logging.getLogger("api")

JPEG = b"\xff\xd8\xff"
MARGEN_MULTIPART_BYTES = 64 * 1024  # cabeceras y el campo geojson, además de la foto


def _error(codigo, http, detalle):
    return JsonResponse({"error": codigo, "detalle": detalle}, status=http)


def _servicio_no_disponible():
    return _error("servicio_no_disponible", 503, "Fallo transitorio: reintenta más tarde.")


def _bloquear(id_anonimo, id_local):
    """Serializa las operaciones sobre UNA MISMA clave (solo PostgreSQL; dura hasta el fin de la transacción).

    Sin esto, un POST y un DELETE simultáneos de la misma clave podrían cruzarse: el DELETE no vería el reporte que el POST
    aún no confirmó, registraría la lápida, y el POST dejaría el reporte "resucitado". Con el candado, uno espera al otro.
    """
    if connection.vendor == "postgresql":
        with connection.cursor() as cursor:
            cursor.execute("SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))", [f"{id_anonimo}:{id_local}"])


def _leer_peticion(request):
    """Devuelve (DatosReporte, bytes_de_la_foto) o lanza ErrorDeValidacion. No toca la base de datos ni la red."""
    maximo = settings.MAX_FOTO_BYTES
    try:
        largo = int(request.META.get("CONTENT_LENGTH") or 0)
    except ValueError:
        largo = 0
    if largo > maximo + MARGEN_MULTIPART_BYTES:  # se rechaza ANTES de leer el cuerpo
        raise ErrorDeValidacion("foto_demasiado_grande", 413, "La foto supera el tamaño máximo.")

    geojson = request.POST.get("geojson")
    if geojson is None:
        raise ErrorDeValidacion("parte_faltante", 400, "Falta la parte geojson.")
    foto = request.FILES.get("foto")
    if foto is None:
        raise ErrorDeValidacion("parte_faltante", 400, "Falta la parte foto.")
    if foto.size > maximo:
        raise ErrorDeValidacion("foto_demasiado_grande", 413, "La foto supera el tamaño máximo.")
    contenido = foto.read()
    if not contenido.startswith(JPEG):
        raise ErrorDeValidacion("foto_no_jpeg", 415, "La foto debe ser un JPEG.")
    return validar_geojson(geojson), contenido


@require_http_methods(["POST"])
def reportes(request):
    try:
        datos, contenido = _leer_peticion(request)
    except ErrorDeValidacion as error:
        return _error(error.codigo, error.http, error.detalle)

    clave = {"id_anonimo": datos.id_anonimo, "id_local": datos.id_local}
    try:
        # Respuesta rápida sin gastar la red: un reenvío (respuesta perdida) o un POST tardío no vuelven a subir la foto.
        if ReporteEliminado.objects.filter(**clave).exists():
            return _error("reporte_eliminado", 410, "El reporte fue dado de baja.")
        if Reporte.objects.filter(**clave).exists():
            return JsonResponse({"estado": "ya_recibido"}, status=200)
    except ErrorDeBaseDeDatos as exc:
        logger.warning("Base de datos no disponible (%s)", type(exc).__name__)
        return _servicio_no_disponible()

    public_id = fotos.public_id_de(datos.id_anonimo, datos.id_local)
    try:
        fotos.subir(contenido, public_id)
    except fotos.ServicioNoDisponible:
        return _servicio_no_disponible()

    try:
        with transaction.atomic():
            _bloquear(datos.id_anonimo, datos.id_local)
            if ReporteEliminado.objects.filter(**clave).exists():
                eliminado = True
            else:
                eliminado = False
                _, creado = Reporte.objects.get_or_create(
                    **clave,
                    defaults={
                        "categoria": datos.categoria,
                        "fecha_hora": datos.fecha_hora,
                        "latitud": datos.latitud,
                        "longitud": datos.longitud,
                        "foto_public_id": public_id,
                        "categoria_ia": datos.categoria_ia,
                        "confianza_ia": datos.confianza_ia,
                        "error_posicion_m": datos.error_posicion_m,
                        "es_prueba": datos.es_prueba,
                    },
                )
    except ErrorDeBaseDeDatos as exc:
        logger.warning("No se pudo guardar el reporte (%s)", type(exc).__name__)
        return _servicio_no_disponible()

    if eliminado:  # la baja llegó mientras se subía la foto: se deshace lo subido (mejor esfuerzo)
        try:
            fotos.borrar(public_id)
        except fotos.ServicioNoDisponible:
            pass
        return _error("reporte_eliminado", 410, "El reporte fue dado de baja.")
    return JsonResponse({"estado": "creado" if creado else "ya_recibido"}, status=201 if creado else 200)


@require_http_methods(["DELETE"])
def reporte(request, id_anonimo, id_local):
    try:
        with transaction.atomic():
            _bloquear(id_anonimo, id_local)
            existente = Reporte.objects.filter(id_anonimo=id_anonimo, id_local=id_local).first()
            if existente is not None:
                # Primero la foto, DENTRO de la transacción: si Cloudinary falla se revierte todo y el cliente reintenta;
                # así no queda ninguna foto huérfana ni una baja a medias.
                fotos.borrar(existente.foto_public_id)
                existente.delete()
            ReporteEliminado.objects.get_or_create(id_anonimo=id_anonimo, id_local=id_local)
    except fotos.ServicioNoDisponible:
        return _servicio_no_disponible()
    except ErrorDeBaseDeDatos as exc:
        logger.warning("No se pudo procesar la baja (%s)", type(exc).__name__)
        return _servicio_no_disponible()
    if existente is None:
        return JsonResponse({"estado": "no_existia"}, status=404)
    return HttpResponse(status=204)
