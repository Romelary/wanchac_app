"""`GET /puntos-criticos`: el mapa público de focos detectados (docs/diseno.md §4.2; Módulo 5 de la app; ADR-0009).

Los focos se calculan AL PEDIR, con un DBSCAN sencillo sobre los reportes (api/focos.py): no hay tabla `punto_critico` ni
tarea programada. Los reportes del APK de prueba (`es_prueba`) no cuentan. Aún no existe la confirmación en campo
(docs/avance.md, tarea A18): el nombre del parámetro `estado=confirmado` se conserva solo por compatibilidad con la app.

Los focos son públicos: la petición no lleva ni necesita ningún identificador de dispositivo, y la respuesta jamás incluye
`idAnonimo`, `idLocal`, fotos ni reportes individuales (solo centroide, cuenta, categoría predominante y fecha).
"""

import logging

from django.db import Error as ErrorDeBaseDeDatos
from django.http import JsonResponse
from django.views.decorators.http import require_GET

from api.focos import PuntoDeReporte, detectar_focos
from api.models import Reporte

logger = logging.getLogger("api")

MEDIA_GEOJSON = "application/geo+json"  # RFC 7946, §12
CACHE_SEGUNDOS = 60  # la app pide la lista cada vez que se abre la pestaña; un foco nuevo puede tardar hasta 1 min


def _feature(foco):
    return {
        "type": "Feature",
        "geometry": {"type": "Point", "coordinates": [foco.longitud, foco.latitud]},  # GeoJSON: [longitud, latitud]
        "properties": {
            "id": foco.id,
            "numReportes": foco.num_reportes,
            "categoriaPredominante": foco.categoria_predominante,
            "fechaDeteccionEpochMillis": int(foco.ultimo_reporte.timestamp() * 1000),
        },
    }


@require_GET
def puntos_criticos(request):
    # plan.md decía `?estado=confirmado`: se admite ese único valor por compatibilidad y cualquier otro es un error.
    if request.GET.get("estado", "confirmado") != "confirmado":
        return JsonResponse(
            {"error": "parametro_invalido", "detalle": "El único valor admitido de «estado» es «confirmado»."}, status=400
        )
    try:
        filas = Reporte.objects.filter(es_prueba=False).values_list("id", "latitud", "longitud", "categoria", "fecha_hora")
        focos = detectar_focos([PuntoDeReporte(*fila) for fila in filas])
    except ErrorDeBaseDeDatos as exc:
        logger.warning("Base de datos no disponible para los focos (%s)", type(exc).__name__)
        return JsonResponse(
            {"error": "servicio_no_disponible", "detalle": "Fallo transitorio: reintenta más tarde."}, status=503
        )
    respuesta = JsonResponse(
        {"type": "FeatureCollection", "features": [_feature(f) for f in focos]}, content_type=MEDIA_GEOJSON
    )
    respuesta["Cache-Control"] = f"public, max-age={CACHE_SEGUNDOS}"
    return respuesta
