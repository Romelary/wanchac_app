package com.wanchaqlimpio.app.core.location

import com.wanchaqlimpio.app.domain.model.Ubicacion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

private const val OBJETIVO = 15f
private val ESPERA_MAXIMA = 30.seconds

private fun lectura(precisionMetros: Float) = Ubicacion(-13.530, -71.950, precisionMetros)

private class ProveedorGuionado(
    private val guion: () -> Flow<Ubicacion>,
) : ProveedorUbicacion {
    override fun lecturas(): Flow<Ubicacion> = guion()
}

/**
 * Lo que decide si un reporte se pierde por una mala primera lectura del GPS. La primera lectura suele ser la peor y
 * mejora en unos segundos: hay que esperarla, sin rendirse y sin esperar de más. Todo con el reloj virtual de las
 * pruebas, así que «30 segundos» no tardan 30 segundos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RastreadorDeUbicacionTest {
    private val tiempo = TestTimeSource()

    private fun rastreador(guion: () -> Flow<Ubicacion>) = RastreadorDeUbicacion(ProveedorGuionado(guion), tiempo)

    private fun TestScope.milisegundosTranscurridos() = testScheduler.currentTime

    // --- Esperar una lectura buena --------------------------------------------------------------------------

    @Test
    fun `una primera lectura buena se usa enseguida, sin esperar`() =
        runTest {
            val rastreador = rastreador { flowOf(lectura(8f)) }

            val resultado = rastreador.esperarPrecisa(OBJETIVO, ESPERA_MAXIMA)

            assertEquals(lectura(8f), resultado)
            assertEquals(0L, milisegundosTranscurridos())
        }

    @Test
    fun `espera a que el GPS mejore y se queda con la primera lectura que cumple`() =
        runTest {
            val rastreador =
                rastreador {
                    flow {
                        emit(lectura(40f))
                        delay(2_000)
                        emit(lectura(25f))
                        delay(2_000)
                        emit(lectura(12f))
                        delay(2_000)
                        emit(lectura(3f)) // ya no hace falta esperarla
                    }
                }
            val progreso = mutableListOf<Float>()

            val resultado = rastreador.esperarPrecisa(OBJETIVO, ESPERA_MAXIMA) { progreso += it }

            assertEquals(lectura(12f), resultado)
            assertEquals("se informó de cada mejora", listOf(40f, 25f, 12f), progreso)
            assertEquals("se dejó de esperar al conseguirla", 4_000L, milisegundosTranscurridos())
        }

    @Test
    fun `si nunca llega a 15 m devuelve la mejor tras la espera maxima, para decir con que se quedo`() =
        runTest {
            val rastreador =
                rastreador {
                    flow {
                        emit(lectura(40f))
                        delay(1_000)
                        emit(lectura(22f))
                        awaitCancellation()
                    }
                }

            val resultado = rastreador.esperarPrecisa(OBJETIVO, ESPERA_MAXIMA)

            assertEquals(lectura(22f), resultado)
            assertEquals(ESPERA_MAXIMA.inWholeMilliseconds, milisegundosTranscurridos())
        }

    @Test
    fun `una lectura peor no reemplaza a la mejor conseguida`() =
        runTest {
            val rastreador =
                rastreador {
                    flow {
                        emit(lectura(20f))
                        emit(lectura(35f))
                        emit(lectura(28f))
                        awaitCancellation()
                    }
                }
            val progreso = mutableListOf<Float>()

            val resultado = rastreador.esperarPrecisa(OBJETIVO, ESPERA_MAXIMA) { progreso += it }

            assertEquals(lectura(20f), resultado)
            assertEquals("el progreso nunca empeora", listOf(20f, 20f, 20f), progreso)
        }

    @Test
    fun `sin ninguna lectura devuelve null cuando se acaba la espera`() =
        runTest {
            val rastreador = rastreador { flow { awaitCancellation() } }

            assertNull(rastreador.esperarPrecisa(OBJETIVO, ESPERA_MAXIMA))
            assertEquals(ESPERA_MAXIMA.inWholeMilliseconds, milisegundosTranscurridos())
        }

    @Test
    fun `si el proveedor termina sin lecturas (GPS apagado o sin permiso) devuelve null enseguida`() =
        runTest {
            val rastreador = rastreador { emptyFlow() }

            assertNull(rastreador.esperarPrecisa(OBJETIVO, ESPERA_MAXIMA))
            assertEquals("no hay nada que esperar", 0L, milisegundosTranscurridos())
        }

    @Test
    fun `si el proveedor termina con lecturas malas devuelve la mejor sin esperar los 30 s`() =
        runTest {
            val rastreador = rastreador { flowOf(lectura(30f), lectura(22f)) }

            assertEquals(lectura(22f), rastreador.esperarPrecisa(OBJETIVO, ESPERA_MAXIMA))
            assertEquals(0L, milisegundosTranscurridos())
        }

    @Test
    fun `la espera maxima se respeta al milisegundo`() =
        runTest {
            val justoAntes =
                rastreador {
                    flow {
                        delay(ESPERA_MAXIMA - 1.milliseconds)
                        emit(lectura(5f))
                    }
                }
            val justoDespues =
                rastreador {
                    flow {
                        delay(ESPERA_MAXIMA + 1.milliseconds)
                        emit(lectura(5f))
                    }
                }

            assertEquals(lectura(5f), justoAntes.esperarPrecisa(OBJETIVO, ESPERA_MAXIMA))
            assertNull(justoDespues.esperarPrecisa(OBJETIVO, ESPERA_MAXIMA))
        }

    @Test
    fun `una lectura exactamente en el objetivo cuenta como buena`() =
        runTest {
            val rastreador = rastreador { flowOf(lectura(15f)) }

            assertEquals(lectura(15f), rastreador.esperarPrecisa(OBJETIVO, ESPERA_MAXIMA))
        }

    // --- La ubicación con la que se guarda -------------------------------------------------------------------

    @Test
    fun `si la lectura de la foto ya era buena se usa sin pedir otra`() =
        runTest {
            val rastreador = rastreador { error("no debía volver a pedirse el GPS") }

            val resultado = rastreador.ubicacionParaGuardar(lectura(9f), OBJETIVO, ESPERA_MAXIMA)

            assertEquals(lectura(9f), resultado)
        }

    @Test
    fun `si la lectura de la foto era mala se espera una buena y se usa esa`() =
        runTest {
            val rastreador =
                rastreador {
                    flow {
                        delay(3_000)
                        emit(lectura(8f))
                    }
                }
            val progreso = mutableListOf<Float>()

            val resultado = rastreador.ubicacionParaGuardar(lectura(40f), OBJETIVO, ESPERA_MAXIMA) { progreso += it }

            assertEquals(lectura(8f), resultado)
            assertEquals(listOf(8f), progreso)
        }

    @Test
    fun `si ninguna llega al objetivo se queda con la mejor de las dos`() =
        runTest {
            val rastreador = rastreador { flowOf(lectura(25f)) }

            val resultado = rastreador.ubicacionParaGuardar(lectura(18f), OBJETIVO, ESPERA_MAXIMA)

            assertEquals("la de la foto era mejor que la que llegó después", lectura(18f), resultado)
        }

    @Test
    fun `sin lectura de la foto ni del GPS no hay ubicacion`() =
        runTest {
            val rastreador = rastreador { emptyFlow() }

            assertNull(rastreador.ubicacionParaGuardar(alCapturar = null, OBJETIVO, ESPERA_MAXIMA))
        }

    // --- Vigilar el GPS mientras la cámara está abierta -----------------------------------------------------

    @Test
    fun `vigilar publica la precision de cada lectura y guarda la ultima`() =
        runTest {
            val rastreador = rastreador { flowOf(lectura(30f), lectura(12f)) }
            assertNull("antes de vigilar no hay precisión", rastreador.precisionActual.value)

            rastreador.vigilar()

            assertEquals(12f, rastreador.precisionActual.value)
            assertEquals(lectura(12f), rastreador.ubicacionReciente())
        }

    @Test
    fun `una lectura de hace menos de 10 s es reciente y de hace mas ya no`() =
        runTest {
            val rastreador = rastreador { flowOf(lectura(9f)) }
            rastreador.vigilar()

            tiempo += 9.seconds
            assertEquals(lectura(9f), rastreador.ubicacionReciente())

            tiempo += 2.seconds
            assertNull("una lectura vieja no dice dónde se tomó la foto", rastreador.ubicacionReciente())
        }

    @Test
    fun `al empezar a vigilar se olvida lo anterior`() =
        runTest {
            val primera = rastreador { flowOf(lectura(9f)) }
            primera.vigilar()
            assertEquals(lectura(9f), primera.ubicacionReciente())

            val segundaVez = RastreadorDeUbicacion(ProveedorGuionado { emptyFlow() }, tiempo)
            segundaVez.vigilar()

            assertNull(segundaVez.ubicacionReciente())
            assertNull(segundaVez.precisionActual.value)
        }

    @Test
    fun `al cancelar la vigilancia se apaga el GPS`() =
        runTest {
            var recogiendo = false
            val rastreador =
                rastreador {
                    flow {
                        recogiendo = true
                        try {
                            emit(lectura(9f))
                            awaitCancellation()
                        } finally {
                            recogiendo = false // en el proveedor real, aquí se quitan las actualizaciones del GPS
                        }
                    }
                }

            val vigilancia = launch { rastreador.vigilar() }
            testScheduler.runCurrent()
            assertEquals("se vigila mientras la cámara está abierta", true, recogiendo)

            vigilancia.cancel()
            testScheduler.runCurrent()

            assertEquals("al salir de la cámara ya no se recogen lecturas", false, recogiendo)
        }
}
