package com.wanchaqlimpio.app.domain.model

/**
 * Un foco de acumulación de residuos **confirmado en campo** (docs/diseno.md §6, entidad `Punto_critico`). Es lo único
 * que muestra el mapa público: nunca un reporte individual (CORR-003), por eso no lleva identificador de dispositivo,
 * fotos ni reportes crudos, y tampoco las métricas internas de calidad del agrupamiento (silueta, ruido).
 */
data class PuntoCritico(
    val id: Long,
    val latitud: Double,
    val longitud: Double,
    /** Cuántos reportes ciudadanos integran el foco (el agrupamiento exige un mínimo, ver diseno.md). */
    val numReportes: Int,
    /** La categoría que más se repite entre esos reportes; `null` si el servidor no la informó o no se reconoce. */
    val categoriaPredominante: CategoriaResiduo?,
    val fechaDeteccionEpochMillis: Long,
)
