package com.wanchaqlimpio.app.data.mapper

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoJsonMapperTest {
    @Test
    fun `el feature usa el orden longitud, latitud exigido por GeoJSON`() {
        val reporte =
            Reporte(
                idAnonimo = "device-1",
                categoria = CategoriaResiduo.VIDRIO,
                estado = EstadoReporte.PENDIENTE,
                latitud = -13.530,
                longitud = -71.950,
                fotoPath = "foto.jpg",
                fechaHoraEpochMillis = 123L,
            )

        val feature = reporte.aGeoJsonFeature()

        assertEquals("Feature", feature.type)
        assertEquals("Point", feature.geometry.type)
        assertEquals(listOf(-71.950, -13.530), feature.geometry.coordinates)
    }

    @Test
    fun `las propiedades reflejan idAnonimo, idLocal, categoria y fecha del reporte`() {
        val reporte =
            Reporte(
                id = 7L,
                idAnonimo = "device-1",
                categoria = CategoriaResiduo.ESCOMBROS,
                estado = EstadoReporte.PENDIENTE,
                latitud = -13.530,
                longitud = -71.950,
                fotoPath = "foto.jpg",
                fechaHoraEpochMillis = 123L,
            )

        val propiedades = reporte.aGeoJsonFeature().properties

        assertEquals("device-1", propiedades.idAnonimo)
        assertEquals(7L, propiedades.idLocal)
        assertEquals("ESCOMBROS", propiedades.categoria)
        assertEquals(123L, propiedades.fechaHoraEpochMillis)
    }
}
