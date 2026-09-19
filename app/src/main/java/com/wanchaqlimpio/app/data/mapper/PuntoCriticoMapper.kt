package com.wanchaqlimpio.app.data.mapper

import com.wanchaqlimpio.app.data.remote.dto.PuntoCriticoFeatureDto
import com.wanchaqlimpio.app.data.remote.dto.PuntoGeometriaDto
import com.wanchaqlimpio.app.data.remote.dto.PuntosCriticosDto
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.PuntoCritico

private const val LATITUD_MAXIMA = 90.0
private const val LONGITUD_MAXIMA = 180.0

/**
 * Convierte la respuesta del servidor en modelos de dominio. **Tolerante**: un foco mal formado (sin coordenadas, con
 * coordenadas imposibles o sin identificador) se descarta en vez de tumbar toda la lista, y una categoría que la app no
 * conoce (p. ej. una nueva en el servidor) se deja en `null`: el foco se muestra igual, sin categoría.
 */
fun PuntosCriticosDto.aPuntosCriticos(): List<PuntoCritico> = features.orEmpty().mapNotNull { it.aPuntoCriticoOrNull() }

private fun PuntoCriticoFeatureDto.aPuntoCriticoOrNull(): PuntoCritico? {
    val coordenadas = geometry.aCoordenadasOrNull()
    val id = properties?.id
    return if (coordenadas == null || properties == null || id == null) {
        null
    } else {
        PuntoCritico(
            id = id,
            latitud = coordenadas.first,
            longitud = coordenadas.second,
            numReportes = (properties.numReportes ?: 0).coerceAtLeast(0),
            categoriaPredominante =
                CategoriaResiduo.entries.firstOrNull {
                    it.name == properties.categoriaPredominante
                },
            fechaDeteccionEpochMillis = properties.fechaDeteccionEpochMillis ?: 0L,
        )
    }
}

/** Devuelve (latitud, longitud) si la geometría es un `Point` válido en orden GeoJSON [longitud, latitud]. */
private fun PuntoGeometriaDto?.aCoordenadasOrNull(): Pair<Double, Double>? =
    this?.let { geometria ->
        geometria.coordinates?.takeIf { geometria.type == "Point" && it.size == 2 }?.let { (longitud, latitud) ->
            if (latitud.esValida(LATITUD_MAXIMA) && longitud.esValida(LONGITUD_MAXIMA)) latitud to longitud else null
        }
    }

private fun Double.esValida(maximo: Double): Boolean = isFinite() && this in -maximo..maximo
