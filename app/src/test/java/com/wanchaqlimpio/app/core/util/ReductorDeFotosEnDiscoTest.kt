package com.wanchaqlimpio.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLog
import java.io.File

/**
 * La reducción con píxeles de verdad (decodificar, girar según EXIF, escalar y volver a comprimir): es lo que le pasa a
 * cada foto antes de guardarla, así que un fallo aquí rompería el reporte. Usa los gráficos NATIVOS de Robolectric
 * (los de verdad, no simulados) para poder decodificar y comprimir imágenes en la JVM.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReductorDeFotosEnDiscoTest {
    private lateinit var carpeta: File

    @Before
    fun preparar() {
        // Robolectric guarda los Log.w en memoria; así salen en el informe y, si `reducirFotoEnDisco` devuelve false,
        // se ve la causa (ver el «system-out» del informe de pruebas).
        ShadowLog.stream = System.out
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        carpeta = File(contexto.cacheDir, "reductor-test").apply { mkdirs() }
    }

    @After
    fun limpiar() {
        carpeta.deleteRecursively()
    }

    /** Un JPEG de [ancho] x [alto]: mitad izquierda azul y mitad derecha roja (para distinguir si se giró). */
    private fun crearJpeg(
        nombre: String,
        ancho: Int,
        alto: Int,
        orientacionExif: Int? = null,
        conUbicacion: Boolean = false,
    ): File {
        val archivo = File(carpeta, nombre)
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.RED)
            drawRect(0f, 0f, ancho / 2f, alto.toFloat(), Paint().apply { color = Color.BLUE })
        }
        archivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        if (orientacionExif != null || conUbicacion) {
            ExifInterface(archivo.absolutePath).apply {
                orientacionExif?.let { setAttribute(ExifInterface.TAG_ORIENTATION, it.toString()) }
                if (conUbicacion) {
                    // ≈ 13°31'48" S, 71°56'51" W: dentro de Wanchaq.
                    setAttribute(ExifInterface.TAG_GPS_LATITUDE, "13/1,31/1,48/1")
                    setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S")
                    setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "71/1,56/1,51/1")
                    setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
                }
                saveAttributes()
            }
        }
        return archivo
    }

    private fun tieneUbicacion(archivo: File): Boolean =
        ExifInterface(archivo.absolutePath).getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null

    private fun dimensiones(archivo: File): Pair<Int, Int> {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decodificarArchivo(archivo, opciones)
        return opciones.outWidth to opciones.outHeight
    }

    @Test
    fun `una foto grande se reduce a 1600 px de lado largo conservando la proporcion y pesa menos`() {
        val foto = crearJpeg("grande.jpg", ancho = 3200, alto = 2400)
        val pesoAntes = foto.length()

        val resultado = reducirFotoEnDisco(foto)

        assertTrue(resultado)
        assertEquals(1600 to 1200, dimensiones(foto))
        assertTrue("pesaba $pesoAntes y ahora ${foto.length()}", foto.length() < pesoAntes)
    }

    @Test
    fun `una foto de camara vertical (EXIF girar 90) queda de pie y sin la marca de giro`() {
        // El teléfono en vertical guarda los píxeles acostados (3200 x 2400) y anota «girar 90°».
        val foto =
            crearJpeg("vertical.jpg", ancho = 3200, alto = 2400, orientacionExif = ExifInterface.ORIENTATION_ROTATE_90)

        assertTrue(reducirFotoEnDisco(foto))

        assertEquals("los píxeles ya vienen girados: de pie", 1200 to 1600, dimensiones(foto))
        val orientacion = ExifInterface(foto.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
        assertTrue("sin marca de giro (fue $orientacion)", orientacion <= ExifInterface.ORIENTATION_NORMAL)
    }

    @Test
    fun `la foto reducida ya no lleva la ubicacion en sus metadatos`() {
        val foto = crearJpeg("con-gps.jpg", ancho = 3200, alto = 2400, conUbicacion = true)
        assertTrue("la prueba parte de una foto con ubicación", tieneUbicacion(foto))

        assertTrue(reducirFotoEnDisco(foto))

        assertFalse("la foto reducida no debe llevar la ubicación", tieneUbicacion(foto))
    }

    @Test
    fun `una foto pequena y ligera se deja exactamente como esta`() {
        val foto = crearJpeg("pequena.jpg", ancho = 800, alto = 600)
        val bytesAntes = foto.readBytes()

        assertTrue(reducirFotoEnDisco(foto))

        assertArrayEquals(bytesAntes, foto.readBytes())
    }

    @Test
    fun `un archivo que no es una imagen devuelve false y queda intacto`() {
        val basura = File(carpeta, "basura.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }

        assertFalse(reducirFotoEnDisco(basura))

        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), basura.readBytes())
    }

    @Test
    fun `un archivo que no existe devuelve false sin lanzar`() {
        assertFalse(reducirFotoEnDisco(File(carpeta, "no-existe.jpg")))
    }

    @Test
    fun `no deja archivos temporales, ni al reducir ni al fallar`() {
        reducirFotoEnDisco(crearJpeg("a.jpg", ancho = 3200, alto = 2400))
        reducirFotoEnDisco(File(carpeta, "basura.jpg").apply { writeBytes(byteArrayOf(9, 9)) })

        assertEquals(listOf("a.jpg", "basura.jpg"), carpeta.list().orEmpty().sorted())
    }
}
