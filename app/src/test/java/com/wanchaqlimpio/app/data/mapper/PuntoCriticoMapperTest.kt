package com.wanchaqlimpio.app.data.mapper

import com.wanchaqlimpio.app.data.remote.dto.PuntoCriticoFeatureDto
import com.wanchaqlimpio.app.data.remote.dto.PuntoCriticoPropertiesDto
import com.wanchaqlimpio.app.data.remote.dto.PuntoGeometriaDto
import com.wanchaqlimpio.app.data.remote.dto.PuntosCriticosDto
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val LONGITUD = -71.9475
private const val LATITUD = -13.53

/** Un foco válido con la forma del contrato (docs/diseno.md §4.2); cada prueba estropea SOLO lo que quiere probar. */
private fun feature(
    coordenadas: List<Double>? = listOf(LONGITUD, LATITUD),
    tipoGeometria: String? = "Point",
    id: Long? = 7,
    numReportes: Int? = 5,
    categoria: String? = "PLASTICO",
    fecha: Long? = 1_790_000_000_000,
    conGeometria: Boolean = true,
    conPropiedades: Boolean = true,
) = PuntoCriticoFeatureDto(
    type = "Feature",
    geometry = if (conGeometria) PuntoGeometriaDto(tipoGeometria, coordenadas) else null,
    properties = if (conPropiedades) PuntoCriticoPropertiesDto(id, numReportes, categoria, fecha) else null,
)

private fun coleccion(vararg features: PuntoCriticoFeatureDto) =
    PuntosCriticosDto("FeatureCollection", features.toList())

class PuntoCriticoMapperTest {
    @Test
    fun `un foco valido se convierte con latitud y longitud en su sitio`() {
        val punto = coleccion(feature()).aPuntosCriticos().single()

        assertEquals(7L, punto.id)
        assertEquals(LATITUD, punto.latitud, 0.0) // GeoJSON viene como [longitud, latitud]: no se invierten
        assertEquals(LONGITUD, punto.longitud, 0.0)
        assertEquals(5, punto.numReportes)
        assertEquals(CategoriaResiduo.PLASTICO, punto.categoriaPredominante)
        assertEquals(1_790_000_000_000, punto.fechaDeteccionEpochMillis)
    }

    @Test
    fun `una coleccion sin features o con features nulas da una lista vacia`() {
        assertEquals(emptyList<Any>(), coleccion().aPuntosCriticos())
        assertEquals(emptyList<Any>(), PuntosCriticosDto(type = "FeatureCollection", features = null).aPuntosCriticos())
        assertEquals(emptyList<Any>(), PuntosCriticosDto().aPuntosCriticos())
    }

    @Test
    fun `un foco sin id, sin geometria o sin propiedades se descarta y los demas se conservan`() {
        val puntos =
            coleccion(
                feature(id = 1),
                feature(id = null),
                feature(conGeometria = false),
                feature(conPropiedades = false),
                feature(id = 2),
            ).aPuntosCriticos()

        assertEquals(listOf(1L, 2L), puntos.map { it.id })
    }

    @Test
    fun `las coordenadas imposibles se descartan`() {
        val invalidas =
            listOf(
                listOf(LONGITUD, 95.0), // latitud fuera de rango
                listOf(-200.0, LATITUD), // longitud fuera de rango
                listOf(Double.NaN, LATITUD),
                listOf(LONGITUD, Double.POSITIVE_INFINITY),
            )
        invalidas.forEach { coordenadas ->
            assertEquals(
                coordenadas.toString(),
                0,
                coleccion(feature(coordenadas = coordenadas)).aPuntosCriticos().size,
            )
        }
    }

    @Test
    fun `una geometria que no es un Point o con un numero de coordenadas distinto de dos se descarta`() {
        assertEquals(0, coleccion(feature(tipoGeometria = "LineString")).aPuntosCriticos().size)
        assertEquals(0, coleccion(feature(tipoGeometria = null)).aPuntosCriticos().size)
        assertEquals(0, coleccion(feature(coordenadas = listOf(LONGITUD))).aPuntosCriticos().size)
        assertEquals(0, coleccion(feature(coordenadas = listOf(LONGITUD, LATITUD, 3400.0))).aPuntosCriticos().size)
        assertEquals(0, coleccion(feature(coordenadas = null)).aPuntosCriticos().size)
    }

    @Test
    fun `una categoria desconocida o ausente deja el foco sin categoria en vez de descartarlo`() {
        assertNull(
            coleccion(
                feature(categoria = "CATEGORIA_NUEVA_DEL_SERVIDOR"),
            ).aPuntosCriticos().single().categoriaPredominante,
        )
        assertNull(coleccion(feature(categoria = null)).aPuntosCriticos().single().categoriaPredominante)
        // distingue mayúsculas: "plastico" en minúscula no es ninguna de las categorías del estudio
        assertNull(coleccion(feature(categoria = "plastico")).aPuntosCriticos().single().categoriaPredominante)
    }

    @Test
    fun `las seis categorias del estudio se reconocen`() {
        CategoriaResiduo.entries.forEach { categoria ->
            val punto = coleccion(feature(categoria = categoria.name)).aPuntosCriticos().single()
            assertEquals(categoria, punto.categoriaPredominante)
        }
    }

    @Test
    fun `un numero de reportes negativo o ausente se trata como cero y la fecha ausente como cero`() {
        assertEquals(0, coleccion(feature(numReportes = -3)).aPuntosCriticos().single().numReportes)
        assertEquals(0, coleccion(feature(numReportes = null)).aPuntosCriticos().single().numReportes)
        assertEquals(0L, coleccion(feature(fecha = null)).aPuntosCriticos().single().fechaDeteccionEpochMillis)
    }

    @Test
    fun `los limites de latitud y longitud son validos`() {
        assertEquals(1, coleccion(feature(coordenadas = listOf(180.0, 90.0))).aPuntosCriticos().size)
        assertEquals(1, coleccion(feature(coordenadas = listOf(-180.0, -90.0))).aPuntosCriticos().size)
    }
}
