package com.wanchaqlimpio.app.core.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val RADIO_TIERRA_METROS = 6_371_000.0

/**
 * Distancia entre dos coordenadas (fórmula de Haversine), en metros.
 * Usada por el filtro de duplicados (diseno.md RNF-07).
 */
fun distanciaMetros(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    val deltaLat = Math.toRadians(lat2 - lat1)
    val deltaLon = Math.toRadians(lon2 - lon1)
    val a =
        sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(deltaLon / 2) * sin(deltaLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return RADIO_TIERRA_METROS * c
}
