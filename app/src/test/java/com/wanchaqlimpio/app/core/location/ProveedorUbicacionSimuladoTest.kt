package com.wanchaqlimpio.app.core.location

import com.wanchaqlimpio.app.core.util.distanciaMetros
import com.wanchaqlimpio.app.domain.LimiteDistritalWanchaq
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.usecase.PRECISION_MAXIMA_METROS
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import com.wanchaqlimpio.app.domain.usecase.ValidarNuevoReporte
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LLAMADAS_DE_UN_CICLO = 80
private const val COLUMNAS_POSIBLES = 40
private const val UN_MINUTO = 60_000L

/** Un proveedor que arrancó [minuto] minutos después del origen del reloj (la columna depende de ese minuto). */
private fun proveedorArrancadoEnElMinuto(minuto: Int) = ProveedorUbicacionSimulado { minuto * UN_MINUTO }

/**
 * El modo de prueba solo sirve si lo que devuelve PASA las reglas de aceptación reales (RF-02, RNF-07): si no,
 * no permitiría probar nada. Estas pruebas usan las reglas de verdad, no copias.
 */
class ProveedorUbicacionSimuladoTest {
    @Test
    fun `todas las ubicaciones de todos los ciclos y columnas caen dentro de Wanchaq`() =
        runTest {
            repeat(COLUMNAS_POSIBLES) { minuto ->
                val proveedor = proveedorArrancadoEnElMinuto(minuto)
                repeat(LLAMADAS_DE_UN_CICLO) { paso ->
                    val u = proveedor.lecturas().first()
                    assertTrue(
                        "minuto $minuto, paso $paso fuera del distrito: ${u.latitud}, ${u.longitud}",
                        LimiteDistritalWanchaq.contiene(u.latitud, u.longitud),
                    )
                }
            }
        }

    @Test
    fun `la precision simulada cumple el filtro de 15 metros`() =
        runTest {
            assertTrue(ProveedorUbicacionSimulado().lecturas().first().precisionMetros <= PRECISION_MAXIMA_METROS)
        }

    @Test
    fun `da una sola lectura por recogida y termina, sin dejar al GPS esperando`() =
        runTest {
            assertEquals(1, ProveedorUbicacionSimulado().lecturas().toList().size)
        }

    @Test
    fun `dos reportes seguidos no se descartan como duplicados`() =
        runTest {
            val proveedor = proveedorArrancadoEnElMinuto(0)
            val primera = proveedor.lecturas().first()
            val segunda = proveedor.lecturas().first()
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
            val proveedor = proveedorArrancadoEnElMinuto(0)
            val inicial = proveedor.lecturas().first()
            repeat(LLAMADAS_DE_UN_CICLO - 1) { proveedor.lecturas().first() }

            assertEquals(inicial, proveedor.lecturas().first())
        }

    @Test
    fun `tras un reinicio de la app los puntos no repiten los de los reportes anteriores`() =
        runTest {
            // La cuenta de pasos vuelve a cero al reiniciar; sin la columna, el reporte 1 de después chocaría con el 1
            // de antes (la regla de duplicados: menos de 20 m y 30 minutos).
            val antes = proveedorArrancadoEnElMinuto(0)
            val puntosDeAntes = List(6) { antes.lecturas().first() }

            for (minutosDespues in 1 until COLUMNAS_POSIBLES) {
                val despues = proveedorArrancadoEnElMinuto(minutosDespues)
                val puntosDeDespues = List(6) { despues.lecturas().first() }

                val masCercano =
                    puntosDeAntes.minOf { a ->
                        puntosDeDespues.minOf { d -> distanciaMetros(a.latitud, a.longitud, d.latitud, d.longitud) }
                    }
                assertTrue(
                    "un reinicio $minutosDespues min después repite un punto (a solo ${masCercano.toInt()} m)",
                    masCercano > 20.0,
                )
            }
        }
}
