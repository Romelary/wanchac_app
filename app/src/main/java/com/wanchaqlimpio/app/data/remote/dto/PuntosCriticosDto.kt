package com.wanchaqlimpio.app.data.remote.dto

/**
 * Respuesta de `GET /puntos-criticos` (docs/diseno.md §4.2): un `FeatureCollection` GeoJSON (RFC 7946) con los focos
 * **confirmados** en campo.
 *
 * Todos los campos son anulables A PROPÓSITO: Gson rellena con `null` lo que falta en el JSON aunque el tipo de Kotlin
 * sea no anulable, y un servidor al que le falte un campo no debe tumbar la app. El mapper descarta lo inválido.
 */
data class PuntosCriticosDto(
    val type: String? = null,
    val features: List<PuntoCriticoFeatureDto>? = null,
)

data class PuntoCriticoFeatureDto(
    val type: String? = null,
    val geometry: PuntoGeometriaDto? = null,
    val properties: PuntoCriticoPropertiesDto? = null,
)

/** GeoJSON exige el orden [longitud, latitud], no [latitud, longitud]. */
data class PuntoGeometriaDto(
    val type: String? = null,
    val coordinates: List<Double>? = null,
)

data class PuntoCriticoPropertiesDto(
    val id: Long? = null,
    val numReportes: Int? = null,
    val categoriaPredominante: String? = null,
    val fechaDeteccionEpochMillis: Long? = null,
)
