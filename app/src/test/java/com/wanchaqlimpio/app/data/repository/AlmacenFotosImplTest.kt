package com.wanchaqlimpio.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Usa Robolectric para tener un `filesDir` real (mismo motivo que las demás pruebas de datos, ver pruebas.md). */
@RunWith(RobolectricTestRunner::class)
class AlmacenFotosImplTest {
    private lateinit var contexto: Context
    private lateinit var almacen: AlmacenFotosImpl

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        almacen = AlmacenFotosImpl(contexto)
    }

    @After
    fun limpiar() {
        contexto.filesDir.deleteRecursively()
    }

    @Test
    fun `nuevaFoto devuelve un archivo dentro de la carpeta privada de la app y crea la carpeta`() {
        val foto = almacen.nuevaFoto()

        assertTrue("la carpeta de fotos debe existir para poder escribir", foto.parentFile?.isDirectory == true)
        assertEquals(contexto.filesDir, foto.parentFile?.parentFile)
        assertFalse("es un archivo nuevo, aun sin escribir", foto.exists())
    }

    @Test
    fun `borrar elimina la foto`() =
        runTest {
            val foto = almacen.nuevaFoto().apply { writeBytes(byteArrayOf(1, 2, 3)) }

            almacen.borrar(foto.absolutePath)

            assertFalse(foto.exists())
        }

    @Test
    fun `borrar tolera que el archivo ya no exista`() =
        runTest {
            almacen.borrar(File(contexto.filesDir, "no-existe.jpg").absolutePath)
        }

    @Test
    fun `borrarTodas elimina las fotos, incluidas las huerfanas, y no toca otros archivos de la app`() =
        runTest {
            val foto1 = almacen.nuevaFoto().apply { writeBytes(byteArrayOf(1)) }
            val foto2 = File(foto1.parentFile, "huerfana.jpg").apply { writeBytes(byteArrayOf(2)) }
            val ajeno = File(contexto.filesDir, "otro-archivo.txt").apply { writeText("no debe borrarse") }

            almacen.borrarTodas()

            assertFalse(foto1.exists())
            assertFalse(foto2.exists())
            assertTrue("solo se borra la carpeta de fotos", ajeno.exists())
        }

    @Test
    fun `borrarHuerfanas borra solo las fotos sin reporte y anteriores al limite`() =
        runTest {
            val enUso = almacen.nuevaFoto().apply { writeBytes(byteArrayOf(1)) }
            val huerfanaVieja = File(enUso.parentFile, "huerfana-vieja.jpg").apply { writeBytes(byteArrayOf(2)) }
            val huerfanaReciente = File(enUso.parentFile, "huerfana-reciente.jpg").apply { writeBytes(byteArrayOf(3)) }
            val limite = 1_000_000L
            enUso.setLastModified(limite - 10)
            huerfanaVieja.setLastModified(limite - 10)
            huerfanaReciente.setLastModified(limite + 10) // tomada mientras corría la limpieza

            almacen.borrarHuerfanas(conservar = setOf(enUso.absolutePath), modificadasAntesDeEpochMillis = limite)

            assertTrue("una foto que un reporte usa NUNCA se borra", enUso.exists())
            assertFalse("la huérfana anterior al límite se borra", huerfanaVieja.exists())
            assertTrue("una foto posterior al límite podría estar a mitad de un reporte", huerfanaReciente.exists())
        }

    @Test
    fun `borrarHuerfanas tolera que la carpeta de fotos aun no exista`() =
        runTest {
            almacen.borrarHuerfanas(conservar = emptySet(), modificadasAntesDeEpochMillis = Long.MAX_VALUE)
        }
}
