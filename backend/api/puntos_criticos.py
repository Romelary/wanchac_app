"""`GET /puntos-criticos`: el mapa público de focos confirmados (docs/diseno.md §4.2; Módulo 5 de la app).

PROVISIONAL. Todavía no existe la tabla `punto_critico` (agrupamiento DBSCAN, Fase 2) ni una forma de CONFIRMAR un foco en
campo (docs/avance.md, tarea A18). Como el contrato solo permite devolver focos confirmados, la única respuesta veraz hoy es
una colección VACÍA, que la app muestra como «Aún no hay focos confirmados». Existe para que la app distinga «todavía no hay
focos» de «el servidor no tiene el endpoint» (un 404, que se vería como un error del servidor y sin mapa).

No toca la base de datos: no despierta a Neon ni gasta cómputo. Los focos son públicos: la petición no lleva ni necesita
ningún identificador de dispositivo, y la respuesta jamás incluirá `idAnonimo`, `idLocal`, fotos ni reportes individuales.
"""

from django.http import JsonResponse
from django.views.decorators.http import require_GET

MEDIA_GEOJSON = "application/geo+json"  # RFC 7946, §12
CACHE_SEGUNDOS = 60  # la app pide la lista cada vez que se abre la pestaña; un foco nuevo puede tardar hasta 1 min


@require_GET
def puntos_criticos(request):
    # plan.md decía `?estado=confirmado`: se admite ese único valor por compatibilidad y cualquier otro es un error. El
    # servidor solo devuelve focos confirmados aunque se pidiera otra cosa (CORR-003).
    if request.GET.get("estado", "confirmado") != "confirmado":
        return JsonResponse(
            {"error": "parametro_invalido", "detalle": "El único valor admitido de «estado» es «confirmado»."}, status=400
        )
    respuesta = JsonResponse({"type": "FeatureCollection", "features": []}, content_type=MEDIA_GEOJSON)
    respuesta["Cache-Control"] = f"public, max-age={CACHE_SEGUNDOS}"
    return respuesta
