package com.wanchaqlimpio.app.data.mapper

import com.wanchaqlimpio.app.data.remote.dto.GeoJsonFeatureDto
import com.wanchaqlimpio.app.data.remote.dto.GeoJsonPointDto
import com.wanchaqlimpio.app.data.remote.dto.ReportePropertiesDto
import com.wanchaqlimpio.app.domain.model.Reporte

fun Reporte.aGeoJsonFeature(): GeoJsonFeatureDto =
    GeoJsonFeatureDto(
        // GeoJSON: [longitud, latitud], al revés del orden habitual lat/lon.
        geometry = GeoJsonPointDto(coordinates = listOf(longitud, latitud)),
        properties =
            ReportePropertiesDto(
                idAnonimo = idAnonimo,
                idLocal = id,
                categoria = categoria.name,
                fechaHoraEpochMillis = fechaHoraEpochMillis,
            ),
    )
