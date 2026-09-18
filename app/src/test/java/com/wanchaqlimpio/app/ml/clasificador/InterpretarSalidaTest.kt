package com.wanchaqlimpio.app.ml.clasificador

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val CLASES =
    listOf(CategoriaResiduo.PLASTICO, CategoriaResiduo.CARTON, CategoriaResiduo.VIDRIO, CategoriaResiduo.ORGANICO)
private const val UMBRAL = 0.70f

class InterpretarSalidaTest {
    @Test
    fun `con confianza suficiente se propone la clase mas probable`() {
        val sugerencia = interpretarSalida(floatArrayOf(0.05f, 0.85f, 0.05f, 0.05f), CLASES, UMBRAL)!!

        assertEquals(CategoriaResiduo.CARTON, sugerencia.categoria)
        assertEquals(0.85f, sugerencia.confianza)
        assertEquals(CategoriaResiduo.CARTON, sugerencia.propuesta)
    }

    @Test
    fun `con poca confianza se propone MIXTO pero se conserva lo que dijo el modelo`() {
        val sugerencia = interpretarSalida(floatArrayOf(0.30f, 0.40f, 0.20f, 0.10f), CLASES, UMBRAL)!!

        assertEquals(CategoriaResiduo.CARTON, sugerencia.categoria)
        assertEquals(0.40f, sugerencia.confianza)
        assertEquals(CategoriaResiduo.MIXTO, sugerencia.propuesta)
    }

    @Test
    fun `una confianza exactamente igual al umbral si alcanza`() {
        val sugerencia = interpretarSalida(floatArrayOf(0.10f, 0.10f, 0.70f, 0.10f), CLASES, UMBRAL)!!

        assertEquals(CategoriaResiduo.VIDRIO, sugerencia.propuesta)
    }

    @Test
    fun `en un empate gana la primera clase para que el resultado sea determinista`() {
        val sugerencia = interpretarSalida(floatArrayOf(0.5f, 0.5f, 0f, 0f), CLASES, UMBRAL)!!

        assertEquals(CategoriaResiduo.PLASTICO, sugerencia.categoria)
    }

    @Test
    fun `la clase del modelo nunca es MIXTO ni ESCOMBROS`() {
        val sugerencia = interpretarSalida(floatArrayOf(0.1f, 0.1f, 0.1f, 0.7f), CLASES, UMBRAL)!!

        assertEquals(CategoriaResiduo.ORGANICO, sugerencia.categoria)
    }

    @Test
    fun `una salida con otro numero de clases es invalida`() {
        assertNull(interpretarSalida(floatArrayOf(0.5f, 0.5f), CLASES, UMBRAL))
        assertNull(interpretarSalida(floatArrayOf(0.2f, 0.2f, 0.2f, 0.2f, 0.2f), CLASES, UMBRAL))
    }

    @Test
    fun `valores no numericos o fuera de rango son invalidos`() {
        assertNull(interpretarSalida(floatArrayOf(Float.NaN, 0.5f, 0.25f, 0.25f), CLASES, UMBRAL))
        assertNull(interpretarSalida(floatArrayOf(Float.POSITIVE_INFINITY, 0f, 0f, 0f), CLASES, UMBRAL))
        assertNull(interpretarSalida(floatArrayOf(-0.2f, 0.6f, 0.3f, 0.3f), CLASES, UMBRAL))
    }

    @Test
    fun `una salida que no suma uno no es softmax y se rechaza`() {
        // Típico de un modelo que devuelve logits en vez de probabilidades: la "confianza" no significaría nada.
        assertNull(interpretarSalida(floatArrayOf(0.9f, 0.9f, 0.9f, 0.9f), CLASES, UMBRAL))
        assertNull(interpretarSalida(floatArrayOf(0.1f, 0.1f, 0.1f, 0.1f), CLASES, UMBRAL))
    }

    @Test
    fun `sin clases no hay sugerencia posible`() {
        assertNull(interpretarSalida(floatArrayOf(), emptyList(), UMBRAL))
    }
}
