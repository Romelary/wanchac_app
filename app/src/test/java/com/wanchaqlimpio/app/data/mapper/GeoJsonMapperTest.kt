package com.wanchaqlimpio.app.data.mapper

import com.google.gson.Gson
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `la sugerencia cruda de la IA viaja aparte de la categoria confirmada`() {
        val reporte =
            Reporte(
                idAnonimo = "device-1",
                categoria = CategoriaResiduo.VIDRIO,
                estado = EstadoReporte.PENDIENTE,
                latitud = -13.530,
                longitud = -71.950,
                fotoPath = "foto.jpg",
                fechaHoraEpochMillis = 123L,
                categoriaIa = CategoriaResiduo.PLASTICO,
                confianzaIa = 0.87f,
            )

        val propiedades = reporte.aGeoJsonFeature().properties

        assertEquals("VIDRIO", propiedades.categoria)
        assertEquals("PLASTICO", propiedades.categoriaIa)
        assertEquals(0.87f, propiedades.confianzaIa)
    }

    @Test
    fun `la precision del GPS viaja y sin ella el campo no se envia`() {
        val base =
            Reporte(
                idAnonimo = "device-1",
                categoria = CategoriaResiduo.ORGANICO,
                estado = EstadoReporte.PENDIENTE,
                latitud = -13.530,
                longitud = -71.950,
                fotoPath = "foto.jpg",
                fechaHoraEpochMillis = 123L,
            )

        val con = base.copy(precisionMetros = 8.5f)

        assertEquals(8.5f, con.aGeoJsonFeature().properties.precisionMetros)
        assertTrue(Gson().toJson(con.aGeoJsonFeature()).contains("\"precisionMetros\":8.5"))
        assertFalse(Gson().toJson(base.aGeoJsonFeature()).contains("precisionMetros"))
    }

    @Test
    fun `sin sugerencia de la IA esos campos no se envian`() {
        val reporte =
            Reporte(
                idAnonimo = "device-1",
                categoria = CategoriaResiduo.ORGANICO,
                estado = EstadoReporte.PENDIENTE,
                latitud = -13.530,
                longitud = -71.950,
                fotoPath = "foto.jpg",
                fechaHoraEpochMillis = 123L,
            )

        val feature = reporte.aGeoJsonFeature()
        val json = Gson().toJson(feature)

        assertNull(feature.properties.categoriaIa)
        assertNull(feature.properties.confianzaIa)
        // Gson omite los nulos: al backend le llega un JSON sin esos campos, no con "null".
        assertFalse(json.contains("categoriaIa"))
        assertFalse(json.contains("confianzaIa"))
        assertTrue(json.contains("\"categoria\":\"ORGANICO\""))
    }
}
