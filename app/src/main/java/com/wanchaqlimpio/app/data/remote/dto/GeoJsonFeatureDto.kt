package com.wanchaqlimpio.app.data.remote.dto

/** Formato GeoJSON (RFC 7946) para enviar un reporte al backend (diseno.md §3.6.4). */
data class GeoJsonFeatureDto(
    val type: String = "Feature",
    val geometry: GeoJsonPointDto,
    val properties: ReportePropertiesDto,
)

/** GeoJSON exige el orden [longitud, latitud], no [latitud, longitud]. */
data class GeoJsonPointDto(
    val type: String = "Point",
    val coordinates: List<Double>,
)

data class ReportePropertiesDto(
    val idAnonimo: String,
    val categoria: String,
    val fechaHoraEpochMillis: Long,
)
