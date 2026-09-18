package com.wanchaqlimpio.app.core.util

import android.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Prueba la lógica PURA del decodificador (cuánto reducir, cómo girar según EXIF). La decodificación real de
 * píxeles depende de `BitmapFactory` y solo se ejerce en un dispositivo (ver `pruebas.md`).
 */
class DecodificadorFotosTest {
    @Test
    fun `una foto de camara de 12 megapixeles se reduce sin bajar de 224 px en su lado menor`() {
        // 3000 px de lado menor: /8 = 375 (>= 224) pero /16 = 187 (< 224) -> el mayor muestreo permitido es 8.
        assertEquals(8, calcularMuestreo(ancho = 4000, alto = 3000, ladoMinimo = 224))
    }

    @Test
    fun `el lado menor manda aunque la foto sea vertical`() {
        assertEquals(8, calcularMuestreo(ancho = 3000, alto = 4000, ladoMinimo = 224))
    }

    @Test
    fun `una imagen pequena no se reduce`() {
        assertEquals(1, calcularMuestreo(ancho = 224, alto = 224, ladoMinimo = 224))
        assertEquals(1, calcularMuestreo(ancho = 300, alto = 300, ladoMinimo = 224))
        assertEquals(1, calcularMuestreo(ancho = 100, alto = 100, ladoMinimo = 224))
    }

    @Test
    fun `el muestreo es siempre una potencia de dos`() {
        listOf(500, 640, 1080, 1500, 2000, 3456).forEach { lado ->
            val muestreo = calcularMuestreo(ancho = lado, alto = lado, ladoMinimo = 224)
            assertEquals("$lado -> $muestreo", 0, muestreo and (muestreo - 1))
        }
    }

    @Test
    fun `las 8 orientaciones EXIF se traducen a giro y espejo`() {
        val esperado =
            mapOf(
                ExifInterface.ORIENTATION_NORMAL to TransformacionExif(0, false),
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL to TransformacionExif(0, true),
                ExifInterface.ORIENTATION_ROTATE_180 to TransformacionExif(180, false),
                ExifInterface.ORIENTATION_FLIP_VERTICAL to TransformacionExif(180, true),
                ExifInterface.ORIENTATION_TRANSPOSE to TransformacionExif(90, true),
                ExifInterface.ORIENTATION_ROTATE_90 to TransformacionExif(90, false),
                ExifInterface.ORIENTATION_TRANSVERSE to TransformacionExif(270, true),
                ExifInterface.ORIENTATION_ROTATE_270 to TransformacionExif(270, false),
            )

        esperado.forEach { (orientacion, transformacion) ->
            assertEquals("orientación EXIF $orientacion", transformacion, transformacionExif(orientacion))
        }
    }

    @Test
    fun `una orientacion desconocida o ausente no transforma la imagen`() {
        assertEquals(TransformacionExif.NINGUNA, transformacionExif(ExifInterface.ORIENTATION_UNDEFINED))
        assertEquals(TransformacionExif.NINGUNA, transformacionExif(99))
    }

    @Test
    fun `la foto vertical de CameraX (giro 90) se endereza`() {
        // Caso real: el teléfono en vertical guarda los píxeles acostados y anota "girar 90° a la derecha".
        assertEquals(90, transformacionExif(ExifInterface.ORIENTATION_ROTATE_90).gradosHorario)
    }
}
