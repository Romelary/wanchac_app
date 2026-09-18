package com.wanchaqlimpio.app.ml.clasificador

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteOrder

class PreprocesadorImagenTest {
    @Test
    fun `los pixeles se entregan como float RGB en 0 a 255 sin normalizar`() {
        // ARGB: rojo puro, y (10, 20, 30). La normalización a [-1, 1] va DENTRO del modelo, no aquí.
        val buffer = aBufferFloat(intArrayOf(0xFFFF0000.toInt(), 0xFF0A141E.toInt()))

        assertEquals(255f, buffer.getFloat(), 0f) // R del píxel 0
        assertEquals(0f, buffer.getFloat(), 0f) // G
        assertEquals(0f, buffer.getFloat(), 0f) // B
        assertEquals(10f, buffer.getFloat(), 0f) // R del píxel 1
        assertEquals(20f, buffer.getFloat(), 0f) // G
        assertEquals(30f, buffer.getFloat(), 0f) // B
    }

    @Test
    fun `el buffer es directo, en orden nativo y del tamano exacto de la entrada del modelo`() {
        val lado = 224

        val buffer = aBufferFloat(IntArray(lado * lado))

        assertTrue("el intérprete exige un búfer directo", buffer.isDirect)
        assertEquals(ByteOrder.nativeOrder(), buffer.order())
        assertEquals(1 * lado * lado * 3 * Float.SIZE_BYTES, buffer.capacity())
        assertEquals("debe quedar listo para leerse desde el inicio", 0, buffer.position())
    }

    @Test
    fun `el canal alfa se ignora`() {
        val opaco = aBufferFloat(intArrayOf(0xFF102030.toInt()))
        val transparente = aBufferFloat(intArrayOf(0x00102030))

        repeat(3) { assertEquals(opaco.getFloat(), transparente.getFloat(), 0f) }
    }
}
