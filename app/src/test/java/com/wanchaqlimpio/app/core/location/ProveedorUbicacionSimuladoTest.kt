package com.wanchaqlimpio.app.core.location

import com.wanchaqlimpio.app.core.util.distanciaMetros
import com.wanchaqlimpio.app.domain.LimiteDistritalWanchaq
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import com.wanchaqlimpio.app.domain.usecase.ValidarNuevoReporte
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LLAMADAS_DE_UN_CICLO = 80

/**
 * El modo de prueba solo sirve si lo que devuelve PASA las reglas de aceptación reales (RF-02, RNF-07): si no,
 * no permitiría probar nada. Estas pruebas usan las reglas de verdad, no copias.
 */
class ProveedorUbicacionSimuladoTest {
    @Test
    fun `todas las ubicaciones de un ciclo completo caen dentro de Wanchaq`() =
        runTest {
            val proveedor = ProveedorUbicacionSimulado()

            repeat(LLAMADAS_DE_UN_CICLO) { paso ->
                val u = proveedor.obtenerUbicacionActual()
                assertTrue(
                    "paso $paso fuera del distrito: ${u.latitud}, ${u.longitud}",
                    LimiteDistritalWanchaq.contiene(u.latitud, u.longitud),
                )
            }
        }

    @Test
    fun `la precision simulada cumple el filtro de 15 metros`() =
        runTest {
            assertTrue(ProveedorUbicacionSimulado().obtenerUbicacionActual().precisionMetros <= 15f)
        }

    @Test
    fun `dos reportes seguidos no se descartan como duplicados`() =
        runTest {
            val proveedor = ProveedorUbicacionSimulado()
            val primera = proveedor.obtenerUbicacionActual()
            val segunda = proveedor.obtenerUbicacionActual()
            val previo =
                Reporte(
                    idAnonimo = "device-1",
                    categoria = CategoriaResiduo.PLASTICO,
                    estado = EstadoReporte.PENDIENTE,
                    latitud = primera.latitud,
                    longitud = primera.longitud,
                    fotoPath = "foto.jpg",
                    fechaHoraEpochMillis = 0L,
                )

            val distancia = distanciaMetros(primera.latitud, primera.longitud, segunda.latitud, segunda.longitud)
            val unMinutoDespues = 60_000L
            val resultado = ValidarNuevoReporte()(segunda, listOf(previo), ahoraEpochMillis = unMinutoDespues)

            assertTrue("solo ${distancia.toInt()} m entre reportes", distancia > 20.0)
            assertEquals(ResultadoValidacionReporte.Valido, resultado)
        }

    @Test
    fun `tras un ciclo completo vuelve al punto inicial`() =
        runTest {
            val proveedor = ProveedorUbicacionSimulado()
            val inicial = proveedor.obtenerUbicacionActual()
            repeat(LLAMADAS_DE_UN_CICLO - 1) { proveedor.obtenerUbicacionActual() }

            assertEquals(inicial, proveedor.obtenerUbicacionActual())
        }
}
