"""Endpoints de salud.

`/health` es el que "pincha" un monitor externo cada ~10 minutos para que Render no duerma el servicio (plan gratuito:
se apaga a los 15 min sin tráfico y tarda ~1 min en despertar). Por eso NO toca la base de datos: Neon también se
duerme (a los 5 min) y cada consulta la despertaría, gastando las 100 horas-cómputo mensuales sin ningún beneficio.

`/health/servicios` sí toca la base y Cloudinary, por eso exige un token y NO lo debe llamar ningún monitor automático.
"""

import hmac

from django.conf import settings
from django.http import Http404, JsonResponse
from django.views.decorators.http import require_GET

from api import servicios


@require_GET
def health(request):
    return JsonResponse({"estado": "ok"})


@require_GET
def health_servicios(request):
    token_esperado = settings.HEALTH_TOKEN
    if not token_esperado:
        # Sin token configurado el endpoint "no existe": no queda una puerta abierta por olvido.
        raise Http404("health/servicios deshabilitado")
    token_recibido = request.headers.get("X-Health-Token", "")
    # compare_digest: comparación en tiempo constante, para no filtrar el token por diferencias de tiempo de respuesta.
    if not hmac.compare_digest(token_recibido.encode(), token_esperado.encode()):
        return JsonResponse({"error": "token invalido"}, status=401)
    resultado = servicios.comprobar_todo()
    return JsonResponse(resultado, status=200 if resultado["ok"] else 503)
