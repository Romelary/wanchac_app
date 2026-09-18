package com.wanchaqlimpio.app.data.remote.dto

/** Formato GeoJSON (RFC 7946) para enviar un reporte al backend (diseno.md §4.2). */
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

/**
 * [idLocal] es el id del reporte en la base local del dispositivo. Junto con
 * [idAnonimo] forma la clave natural del reporte en el servidor: permite que
 * un reenvío no duplique el registro y que la baja (DELETE) lo identifique.
 *
 * [categoria] es la que el ciudadano confirmó; [categoriaIa]/[confianzaIa] son la respuesta cruda del modelo
 * de IA (omitidas si el modelo no estuvo disponible) y permiten medir su exactitud frente al observador de
 * campo sin mezclarla con las correcciones humanas (Anexo 2).
 */
data class ReportePropertiesDto(
    val idAnonimo: String,
    val idLocal: Long,
    val categoria: String,
    val fechaHoraEpochMillis: Long,
    val categoriaIa: String? = null,
    val confianzaIa: Float? = null,
)
