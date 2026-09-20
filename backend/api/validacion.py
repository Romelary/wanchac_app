"""Validación del `geojson` que envía la app (contrato: docs/diseno.md §4.2).

Regla de diseño: NUNCA se devuelve al cliente (ni se registra) el valor que falló, solo un código fijo y un texto fijo.
El `geojson` contiene el seudónimo del dispositivo y la ubicación del ciudadano; un mensaje de error que los repitiera los
filtraría a los registros.

Códigos HTTP: 400 = la petición está mal formada (el cliente no puede arreglarla reintentando); 422 = está bien formada
pero incumple una regla del estudio (fuera de Wanchaq, fecha imposible).
"""

import json
import math
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone

from api.constantes import (
    CATEGORIAS,
    CATEGORIAS_IA,
    ERROR_POSICION_MAXIMO_M,
    FECHA_MINIMA,
    ID_LOCAL_MAXIMO,
    TOLERANCIA_RELOJ,
    WANCHAQ_LATITUD,
    WANCHAQ_LONGITUD,
)


class ErrorDeValidacion(Exception):
    def __init__(self, codigo, http, detalle):
        super().__init__(codigo)
        self.codigo = codigo
        self.http = http
        self.detalle = detalle


@dataclass(frozen=True)
class DatosReporte:
    id_anonimo: uuid.UUID
    id_local: int
    categoria: str
    fecha_hora: datetime
    latitud: float
    longitud: float
    categoria_ia: str | None
    confianza_ia: float | None
    error_posicion_m: float | None = None
    es_prueba: bool = False


def _numero(valor):
    return isinstance(valor, (int, float)) and not isinstance(valor, bool) and math.isfinite(valor)


def _entero(valor):
    return isinstance(valor, int) and not isinstance(valor, bool)


def _error(codigo, detalle, http=400):
    return ErrorDeValidacion(codigo, http, detalle)


def _id_anonimo(valor):
    """UUID en su forma canónica (la que genera `UUID.randomUUID().toString()` en Android)."""
    if not isinstance(valor, str):
        raise _error("id_anonimo_invalido", "idAnonimo debe ser un UUID.")
    try:
        parseado = uuid.UUID(valor)
    except ValueError:
        raise _error("id_anonimo_invalido", "idAnonimo debe ser un UUID.") from None
    if str(parseado) != valor.lower():  # UUID() acepta llaves, urn: y otras formas: solo se admite la canónica
        raise _error("id_anonimo_invalido", "idAnonimo debe ser un UUID.")
    return parseado


def _fecha(valor, ahora):
    if not _entero(valor):
        raise _error("fecha_invalida", "fechaHoraEpochMillis debe ser un entero (milisegundos).")
    try:
        fecha = datetime.fromtimestamp(valor / 1000, tz=timezone.utc)
    except (OverflowError, OSError, ValueError):
        raise _error("fecha_fuera_de_rango", "La fecha del reporte no es plausible.", 422) from None
    if not FECHA_MINIMA <= fecha <= ahora + TOLERANCIA_RELOJ:
        raise _error("fecha_fuera_de_rango", "La fecha del reporte no es plausible.", 422)
    return fecha


def _ia(propiedades):
    """`categoriaIa` y `confianzaIa` van juntas o ninguna: se omiten si el modelo no estuvo disponible (CORR-010)."""
    categoria, confianza = propiedades.get("categoriaIa"), propiedades.get("confianzaIa")
    if categoria is None and confianza is None:
        return None, None
    if categoria is None or confianza is None:
        raise _error("ia_incompleta", "categoriaIa y confianzaIa van juntas o ninguna.")
    if categoria not in CATEGORIAS_IA:
        raise _error("categoria_ia_invalida", "categoriaIa no es una clase del modelo.")
    if not _numero(confianza) or not 0 <= confianza <= 1:
        raise _error("confianza_ia_invalida", "confianzaIa debe estar entre 0 y 1.")
    return categoria, float(confianza)


def _precision(propiedades):
    """`precisionMetros` es opcional (las versiones anteriores de la app no lo envían)."""
    valor = propiedades.get("precisionMetros")
    if valor is None:
        return None
    if not _numero(valor) or not 0 <= valor <= ERROR_POSICION_MAXIMO_M:
        raise _error("precision_invalida", f"precisionMetros debe estar entre 0 y {ERROR_POSICION_MAXIMO_M}.")
    return float(valor)


def _es_prueba(propiedades):
    """`esPrueba` (APK de prueba) es opcional y, si viene, debe ser booleano."""
    valor = propiedades.get("esPrueba", False)
    if not isinstance(valor, bool):
        raise _error("es_prueba_invalido", "esPrueba debe ser verdadero o falso.")
    return valor


def validar_geojson(texto, ahora=None):
    """Devuelve los [DatosReporte] o lanza [ErrorDeValidacion]. `ahora` se inyecta para poder probar las fechas."""
    ahora = ahora or datetime.now(timezone.utc)
    try:
        documento = json.loads(texto)
    except (TypeError, ValueError, RecursionError):
        raise _error("geojson_invalido", "El campo geojson no es JSON válido.") from None
    if not isinstance(documento, dict) or documento.get("type") != "Feature":
        raise _error("geojson_invalido", "El geojson debe ser un Feature (RFC 7946).")

    geometria = documento.get("geometry")
    if not isinstance(geometria, dict) or geometria.get("type") != "Point":
        raise _error("geometria_invalida", "La geometría debe ser un Point.")
    coordenadas = geometria.get("coordinates")
    if not (isinstance(coordenadas, list) and len(coordenadas) == 2 and all(_numero(c) for c in coordenadas)):
        raise _error("coordenadas_invalidas", "Las coordenadas deben ser [longitud, latitud].")
    longitud, latitud = float(coordenadas[0]), float(coordenadas[1])
    if not (-180 <= longitud <= 180 and -90 <= latitud <= 90):
        raise _error("coordenadas_invalidas", "Las coordenadas están fuera de rango.")

    propiedades = documento.get("properties")
    if not isinstance(propiedades, dict):
        raise _error("propiedades_invalidas", "Faltan las propiedades del reporte.")
    id_anonimo = _id_anonimo(propiedades.get("idAnonimo"))
    id_local = propiedades.get("idLocal")
    if not _entero(id_local) or not 1 <= id_local <= ID_LOCAL_MAXIMO:
        raise _error("id_local_invalido", "idLocal debe ser un entero positivo.")
    categoria = propiedades.get("categoria")
    if categoria not in CATEGORIAS:
        raise _error("categoria_invalida", "La categoría no es una de las del estudio.")
    fecha_hora = _fecha(propiedades.get("fechaHoraEpochMillis"), ahora)
    categoria_ia, confianza_ia = _ia(propiedades)
    error_posicion_m = _precision(propiedades)
    es_prueba = _es_prueba(propiedades)

    # La regla del estudio va DESPUÉS de comprobar la forma: así un 422 significa "bien formado pero fuera del distrito".
    # Los reportes del APK de prueba (esPrueba) se aceptan en cualquier sitio: se excluyen de focos y estadísticas.
    dentro = WANCHAQ_LATITUD[0] <= latitud <= WANCHAQ_LATITUD[1] and WANCHAQ_LONGITUD[0] <= longitud <= WANCHAQ_LONGITUD[1]
    if not (dentro or es_prueba):
        raise _error("fuera_de_wanchaq", "La ubicación está fuera del distrito de Wanchaq.", 422)

    return DatosReporte(
        id_anonimo, id_local, categoria, fecha_hora, latitud, longitud, categoria_ia, confianza_ia, error_posicion_m, es_prueba
    )
