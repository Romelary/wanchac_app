package com.wanchaqlimpio.app.domain.model

/**
 * Lectura cruda del receptor satelital del dispositivo, antes de cualquier
 * filtro. [precisionMetros] es el radio de error que reporta el propio
 * sistema operativo (no una medición independiente).
 */
data class Ubicacion(
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Float,
)
