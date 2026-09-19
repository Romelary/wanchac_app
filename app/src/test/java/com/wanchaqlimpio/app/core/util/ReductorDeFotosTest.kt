package com.wanchaqlimpio.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El cálculo del tamaño al que se reduce cada foto antes de subirla (la parte con píxeles reales está en
 * [ReductorDeFotosEnDiscoTest]). Una foto de 12 megapíxeles pesa 3–8 MB; de 50 megapíxeles, más de 10 MB, que es lo
 * que el servidor rechaza (413, definitivo).
 */
class ReductorDeFotosTest {
    @Test
    fun `una foto horizontal de 12 megapixeles se reduce a 1600 x 1200`() {
        assertEquals(1600 to 1200, calcularTamanoReducido(ancho = 4000, alto = 3000, ladoMaximo = 1600))
    }

    @Test
    fun `una foto vertical se reduce por su lado largo, que es el alto`() {
        assertEquals(1200 to 1600, calcularTamanoReducido(ancho = 3000, alto = 4000, ladoMaximo = 1600))
    }

    @Test
    fun `una foto de 50 megapixeles tambien queda en 1600 x 1200`() {
        assertEquals(1600 to 1200, calcularTamanoReducido(ancho = 8160, alto = 6120, ladoMaximo = 1600))
    }

    @Test
    fun `una imagen que ya cabe no se toca y nunca se agranda`() {
        assertNull(calcularTamanoReducido(ancho = 1600, alto = 1200, ladoMaximo = 1600))
        assertNull(calcularTamanoReducido(ancho = 1000, alto = 800, ladoMaximo = 1600))
        assertNull(calcularTamanoReducido(ancho = 1600, alto = 1600, ladoMaximo = 1600))
    }

    @Test
    fun `el lado largo queda exactamente en el maximo y la proporcion se conserva`() {
        listOf(4032 to 3024, 4608 to 3456, 5000 to 2812, 3000 to 3999).forEach { (ancho, alto) ->
            val (nuevoAncho, nuevoAlto) = calcularTamanoReducido(ancho, alto, ladoMaximo = 1600)!!

            assertEquals("lado largo de $ancho x $alto", 1600, maxOf(nuevoAncho, nuevoAlto))
            val proporcionOriginal = ancho.toDouble() / alto
            val proporcionNueva = nuevoAncho.toDouble() / nuevoAlto
            assertEquals("proporción de $ancho x $alto", proporcionOriginal, proporcionNueva, 0.01)
        }
    }

    @Test
    fun `una imagen extrema no produce un lado de cero pixeles`() {
        val (ancho, alto) = calcularTamanoReducido(ancho = 10_000, alto = 1, ladoMaximo = 1600)!!

        assertEquals(1600, ancho)
        assertTrue("el lado corto no puede ser 0", alto >= 1)
    }

    @Test
    fun `dimensiones invalidas no se reducen`() {
        assertNull(calcularTamanoReducido(ancho = 0, alto = 3000, ladoMaximo = 1600))
        assertNull(calcularTamanoReducido(ancho = 4000, alto = 0, ladoMaximo = 1600))
        assertNull(calcularTamanoReducido(ancho = -1, alto = -1, ladoMaximo = 1600))
    }
}
