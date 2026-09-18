package com.wanchaqlimpio.app.ml.clasificador

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.domain.repository.ClasificadorResiduo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

private val CONFIGURACION =
    ConfiguracionModelo(
        clases =
            listOf(
                CategoriaResiduo.PLASTICO,
                CategoriaResiduo.CARTON,
                CategoriaResiduo.VIDRIO,
                CategoriaResiduo.ORGANICO,
            ),
        umbralConfianza = 0.70f,
        lado = 224,
    )

private class PreparadorFalso(
    private val entrada: ByteBuffer? = ByteBuffer.allocate(0),
) : PreparadorEntrada {
    val fotosPedidas = mutableListOf<String>()

    override fun preparar(fotoPath: String): ByteBuffer? {
        fotosPedidas += fotoPath
        return entrada
    }
}

private class MotorFalso(
    private val salida: () -> FloatArray,
) : MotorInferencia {
    var invocaciones = 0
        private set

    override fun inferir(entrada: ByteBuffer): FloatArray {
        invocaciones++
        return salida()
    }
}

class ClasificadorResiduoImplTest {
    private val errores = mutableListOf<Pair<String, Throwable>>()

    private fun clasificador(
        preparador: PreparadorEntrada = PreparadorFalso(),
        motor: MotorInferencia = MotorFalso { floatArrayOf(0.05f, 0.85f, 0.05f, 0.05f) },
    ) = ClasificadorResiduoImpl(
        preparador,
        motor,
        CONFIGURACION,
        registrarError = { mensaje, causa -> errores += mensaje to causa },
    )

    @Test
    fun `una foto legible produce la sugerencia del modelo aplicando el umbral`() =
        runTest {
            val sugerencia = clasificador().clasificar("foto.jpg")

            assertEquals(SugerenciaIa(CategoriaResiduo.CARTON, 0.85f, propuesta = CategoriaResiduo.CARTON), sugerencia)
            assertTrue(errores.isEmpty())
        }

    @Test
    fun `con poca confianza propone MIXTO`() =
        runTest {
            val motor = MotorFalso { floatArrayOf(0.30f, 0.40f, 0.20f, 0.10f) }

            val sugerencia = clasificador(motor = motor).clasificar("foto.jpg")

            assertEquals(CategoriaResiduo.MIXTO, sugerencia?.propuesta)
        }

    @Test
    fun `una foto ilegible da null sin invocar al modelo`() =
        runTest {
            val motor = MotorFalso { floatArrayOf(1f, 0f, 0f, 0f) }

            val preparador = PreparadorFalso(entrada = null)

            val sugerencia = clasificador(preparador = preparador, motor = motor).clasificar("rota.jpg")

            assertNull(sugerencia)
            assertEquals(0, motor.invocaciones)
        }

    @Test
    fun `si el modelo falla se registra el error y no hay sugerencia`() =
        runTest {
            val causa = IllegalStateException("interprete cerrado")

            val sugerencia = clasificador(motor = MotorFalso { throw causa }).clasificar("foto.jpg")

            assertNull(sugerencia)
            // No se traga en silencio: queda registrado con su causa.
            assertEquals(1, errores.size)
            assertSame(causa, errores.single().second)
        }

    @Test
    fun `una salida que no es una distribucion de probabilidad no produce sugerencia`() =
        runTest {
            val sugerencia = clasificador(motor = MotorFalso { floatArrayOf(5f, -3f, 1f, 0f) }).clasificar("foto.jpg")

            assertNull(sugerencia)
        }

    @Test
    fun `la cancelacion de la corrutina no se confunde con un fallo del modelo`() {
        val cancelacion = CancellationException("cancelado")

        assertThrows(CancellationException::class.java) {
            runBlocking { clasificador(motor = MotorFalso { throw cancelacion }).clasificar("foto.jpg") }
        }
        assertTrue("no debe registrarse como error", errores.isEmpty())
    }

    @Test
    fun `el clasificador perezoso no carga el modelo hasta que se usa y solo lo carga una vez`() =
        runTest {
            var cargas = 0
            val perezoso =
                ClasificadorPerezoso(crear = {
                    cargas++
                    clasificador()
                })
            assertEquals("crearlo no debe cargar nada", 0, cargas)

            perezoso.clasificar("a.jpg")
            perezoso.clasificar("b.jpg")

            assertEquals(1, cargas)
        }

    @Test
    fun `si el modelo no se puede cargar se registra una vez y el reporte sigue con eleccion manual`() =
        runTest {
            val fallo = UnsatisfiedLinkError("libLiteRt.so no disponible en esta arquitectura")
            val perezoso =
                ClasificadorPerezoso(
                    crear = { throw fallo },
                    registrarError = { mensaje, causa -> errores += mensaje to causa },
                )

            assertNull(perezoso.clasificar("a.jpg"))
            assertNull(perezoso.clasificar("b.jpg"))

            assertEquals("solo se registra la primera vez", 1, errores.size)
            assertSame(fallo, errores.single().second)
        }

    @Test
    fun `ClasificadorNoDisponible nunca sugiere`() =
        runTest {
            val clasificador: ClasificadorResiduo = ClasificadorNoDisponible

            assertNull(clasificador.clasificar("foto.jpg"))
        }
}
