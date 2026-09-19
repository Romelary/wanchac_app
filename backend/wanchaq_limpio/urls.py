from django.urls import path

from api import reportes, views

urlpatterns = [
    path("health", views.health),
    path("health/servicios", views.health_servicios),
    path("reportes", reportes.reportes),
    # uuid e int: una ruta mal formada ni siquiera llega a la vista (404), y la app trata el 404 de la baja como cumplida.
    path("reportes/<uuid:id_anonimo>/<int:id_local>", reportes.reporte),
]
