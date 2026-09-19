"""Constantes del contrato con la app (docs/diseno.md §4.2), compartidas por los modelos y la validación."""

from datetime import datetime, timedelta, timezone

CATEGORIAS = ("PLASTICO", "CARTON", "VIDRIO", "ORGANICO", "ESCOMBROS", "MIXTO")
# El clasificador de IA on-device solo predice estas 4; ESCOMBROS y MIXTO son de elección manual (ADR-0003, ml/README.md).
CATEGORIAS_IA = ("PLASTICO", "CARTON", "VIDRIO", "ORGANICO")

# Rectángulo aproximado del distrito de Wanchaq: el MISMO que usa la app (LimiteDistritalWanchaq.kt), para que cliente y
# servidor coincidan. Es provisional hasta tener el polígono oficial (pendiente en docs/progreso.md); entonces la
# validación definitiva pasará a ST_Within en PostGIS (CORR-007).
WANCHAQ_LATITUD = (-13.545, -13.515)
WANCHAQ_LONGITUD = (-71.965, -71.930)

# La fecha del reporte la fija el reloj del teléfono, que puede estar mal ajustado: se tolera un día hacia el futuro y se
# rechaza lo anterior al desarrollo de la app.
FECHA_MINIMA = datetime(2024, 1, 1, tzinfo=timezone.utc)
TOLERANCIA_RELOJ = timedelta(days=1)

ID_LOCAL_MAXIMO = 2**63 - 1  # BigInteger de PostgreSQL; el id local de Room es un Long
