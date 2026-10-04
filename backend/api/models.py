"""Modelo de datos de la Fase 1 del backend: recibir y guardar reportes (docs/diseno.md §4.2).

Solo se guarda lo que el contrato trae. La columna `geography(Point,4326)` con su índice GiST (ADR-0004 #2) NO se crea
todavía: la Fase 1 no hace ninguna consulta espacial (la validación "dentro de Wanchaq" es un rectángulo, igual que en la
app), y añadirla ahora sería DDL específico de PostGIS sin uso y sin poder probarlo en la suite (SQLite). Se agrega con la
primera consulta espacial real (Fase 2/3).
"""

from django.db import models
from django.db.models import Q

from api.constantes import CATEGORIAS, CATEGORIAS_IA, ERROR_POSICION_MAXIMO_M


class Reporte(models.Model):
    """Un reporte ciudadano. La clave natural es (id_anonimo, id_local): permite que el reenvío del mismo reporte no lo
    duplique y que la baja lo identifique sin login (ADR-0001, ADR-0002)."""

    id_anonimo = models.UUIDField(help_text="Seudónimo del dispositivo. NUNCA se devuelve en respuestas ni se registra en logs.")
    id_local = models.BigIntegerField(help_text="Id del reporte en la base local del teléfono.")
    categoria = models.CharField(max_length=16, help_text="La que el ciudadano confirmó.")
    fecha_hora = models.DateTimeField(help_text="Momento de la captura, según el reloj del teléfono (UTC).")
    latitud = models.FloatField()
    longitud = models.FloatField()
    foto_public_id = models.CharField(max_length=128, help_text="Identificador de la foto PRIVADA en Cloudinary.")
    categoria_ia = models.CharField(
        max_length=16, null=True, blank=True, help_text="Respuesta cruda del modelo de IA (CORR-010); nula si no estuvo disponible."
    )
    confianza_ia = models.FloatField(null=True, blank=True)
    error_posicion_m = models.FloatField(
        null=True,
        blank=True,
        help_text="Precisión que declara el teléfono (radio del 68 %); NO es el error real. Nula en reportes antiguos.",
    )
    es_prueba = models.BooleanField(
        default=False, help_text="Reporte del APK de prueba: puede estar fuera de Wanchaq y se excluye de focos y estadísticas."
    )
    recibido_en = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "reporte"
        constraints = [
            models.UniqueConstraint(fields=["id_anonimo", "id_local"], name="uq_reporte_clave_natural"),
            models.CheckConstraint(
                condition=Q(latitud__gte=-90, latitud__lte=90, longitud__gte=-180, longitud__lte=180),
                name="ck_reporte_coordenadas",
            ),
            models.CheckConstraint(condition=Q(categoria__in=CATEGORIAS), name="ck_reporte_categoria"),
            models.CheckConstraint(
                condition=Q(categoria_ia__isnull=True) | Q(categoria_ia__in=CATEGORIAS_IA), name="ck_reporte_categoria_ia"
            ),
            models.CheckConstraint(
                condition=Q(confianza_ia__isnull=True) | Q(confianza_ia__gte=0, confianza_ia__lte=1),
                name="ck_reporte_confianza_ia",
            ),
            models.CheckConstraint(
                condition=Q(error_posicion_m__isnull=True)
                | Q(error_posicion_m__gte=0, error_posicion_m__lte=ERROR_POSICION_MAXIMO_M),
                name="ck_reporte_error_posicion",
            ),
        ]
        indexes = [models.Index(fields=["fecha_hora"], name="ix_reporte_fecha_hora")]  # ventana de observación (Fase 2)


class ReporteEliminado(models.Model):
    """Lápida de una baja (docs/diseno.md §4.2): cierra las carreras "respuesta perdida" y "subida en vuelo mientras el
    usuario borra". Un POST posterior de la misma clave se rechaza con 410 Gone. Se registra aunque el reporte no
    existiera (la baja pudo llegar antes que la subida)."""

    id_anonimo = models.UUIDField()
    id_local = models.BigIntegerField()
    eliminado_en = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "reporte_eliminado"
        constraints = [models.UniqueConstraint(fields=["id_anonimo", "id_local"], name="uq_lapida_clave_natural")]

class FichaObservacion(models.Model):
    """Registro de la validación en campo de un foco detectado (Paso 2.2)."""

    foco_id = models.BigIntegerField(
        primary_key=True, 
        help_text="ID del foco (que corresponde al menor id de reporte del clúster)."
    )
    confirmado = models.BooleanField(help_text="¿Se confirmó que el basural existe?")
    distancia_m = models.FloatField(null=True, blank=True, help_text="Distancia en metros desde el centro del foco.")
    tipo_residuo = models.CharField(max_length=64, null=True, blank=True)
    volumen = models.CharField(max_length=64, null=True, blank=True)
    entorno = models.CharField(max_length=64, null=True, blank=True)
    categoria_observador = models.CharField(max_length=16, null=True, blank=True, help_text="Categoría real según el investigador.")
    foto_valida = models.BooleanField(default=True)
    registro_completo = models.BooleanField(default=True)
    fecha_verificacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "ficha_observacion"
