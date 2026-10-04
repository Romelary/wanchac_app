from django.urls import path

from api import puntos_criticos, reportes, views

urlpatterns = [
    path("health", views.health),
    path("health/servicios", views.health_servicios),
    path("puntos-criticos", puntos_criticos.puntos_criticos),
    path("mapa-calor", puntos_criticos.mapa_calor),
    path("reportes", reportes.reportes),
    # uuid e int: una ruta mal formada ni siquiera llega a la vista (404), y la app trata el 404 de la baja como cumplida.
    path("reportes/<uuid:id_anonimo>/<int:id_local>", reportes.reporte),
]
