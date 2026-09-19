package com.wanchaqlimpio.app.ml.clasificador

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.wanchaqlimpio.app.core.di.cargarConfiguracion
import com.wanchaqlimpio.app.core.di.cargarInterprete
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Diferencia máxima admitida entre lo que calcula la app y lo que calculó Python (ver generar_referencias.py).
 *
 * `generar_referencias.py` sugiere 0,05, pero **medido en la Galaxy Tab S9+ (2026-09-19) no alcanza**: con los MISMOS
 * píxeles de entrada (comprobado: la entrada de la app y la de Python son idénticas, diferencia 0,0 en las 150 528
 * cifras de cada una de las 4 imágenes), el modelo en el teléfono da hasta 0,059 de diferencia en una probabilidad y
 * 0,077 en la confianza del degradado rojo-azul, una imagen sintética muy sensible; otras dos imágenes coinciden
 * hasta la cuarta cifra. Es una diferencia de cómo el intérprete del teléfono calcula el modelo, no del
 * preprocesado. Con 0,10 esta prueba sigue detectando lo que debe (canales BGR, escala 0–1 o EXIF ignorado cambian las
 * probabilidades en 0,3 o más), pero **la confianza de la app no es idéntica a la de Python**: el umbral de confianza
 * debe calibrarse con las cifras del teléfono (ver `docs/pruebas.md`, «Módulo 3b»).
 */
private const val TOLERANCIA = 0.10f

/**
 * ÚNICA comprobación automática de que la app prepara la foto igual que el entrenamiento y lee bien la salida
 * del modelo: LiteRT no corre en la JVM, así que esta prueba solo se puede ejecutar en un dispositivo o
 * emulador (`./gradlew connectedDebugAndroidTest`).
 *
 * Usa el modelo REAL empaquetado en la app y 4 imágenes de referencia de 224x224 cuyas probabilidades se
 * calcularon en Python con el mismo `.tflite` (`ml/entrenamiento/generar_referencias.py`). Si la app confundiera
 * el orden de los canales (BGR en vez de RGB), la escala (0–1 en vez de 0–255) o ignorara la orientación EXIF,
 * las probabilidades no coincidirían.
 */
@RunWith(AndroidJUnit4::class)
class ClasificadorResiduoReferenciaTest {
    private val instrumentacion = InstrumentationRegistry.getInstrumentation()
    private val contextoApp: Context = instrumentacion.targetContext
    private val contextoPruebas: Context = instrumentacion.context
    private lateinit var referencias: JsonObject

    @Before
    fun cargarReferencias() {
        val texto =
            contextoPruebas.assets
                .open("referencias/referencias.json")
                .bufferedReader()
                .use { it.readText() }
        referencias = Gson().fromJson(texto, JsonObject::class.java)
    }

    /** Las imágenes están en los assets de la prueba; el preprocesador lee archivos, así que se copian a la caché. */
    private fun copiarAImagen(nombre: String): File {
        val destino = File(contextoApp.cacheDir, "ref_$nombre")
        contextoPruebas.assets.open("referencias/$nombre").use { origen ->
            destino.outputStream().use { origen.copyTo(it) }
        }
        return destino
    }

    private fun nombresDeReferencia() = referencias.getAsJsonObject("referencias").keySet()

    @Test
    fun elModeloEmpaquetadoEsElQueUsoPythonParaLasReferencias() {
        val esperado = referencias.get("modelo_sha256").asString
        val metadata =
            contextoApp.assets
                .open("clasificador/metadata.json")
                .bufferedReader()
                .use { Gson().fromJson(it.readText(), JsonObject::class.java) }

        assertEquals(
            "regenerar las referencias (generar_referencias.py) tras cambiar el modelo",
            esperado,
            metadata.get("sha256_tflite").asString,
        )
    }

    @Test
    fun lasProbabilidadesQueCalculaLaAppCoincidenConLasDePython() {
        val configuracion = cargarConfiguracion(contextoApp)
        val motor = MotorLiteRt(cargarInterprete(contextoApp, configuracion), configuracion.clases.size)
        val preparador = PreprocesadorImagen(configuracion.lado)

        nombresDeReferencia().forEach { nombre ->
            val esperadas =
                referencias
                    .getAsJsonObject(
                        "referencias",
                    ).getAsJsonObject(nombre)
                    .getAsJsonArray("probabilidades")
                    .map { it.asFloat }
            val entrada = preparador.preparar(copiarAImagen(nombre).absolutePath)
            assertNotNull("no se pudo leer $nombre", entrada)

            val obtenidas = motor.inferir(entrada!!)

            esperadas.forEachIndexed { i, esperada ->
                assertEquals("$nombre, clase ${configuracion.clases[i]}", esperada, obtenidas[i], TOLERANCIA)
            }
        }
    }

    @Test
    fun elClasificadorCompletoDaLaMismaClaseYConfianzaQuePython() =
        runBlocking {
            val configuracion = cargarConfiguracion(contextoApp)
            val clasificador =
                ClasificadorResiduoImpl(
                    preparador = PreprocesadorImagen(configuracion.lado),
                    motor = MotorLiteRt(cargarInterprete(contextoApp, configuracion), configuracion.clases.size),
                    configuracion = configuracion,
                )

            nombresDeReferencia().forEach { nombre ->
                val esperado = referencias.getAsJsonObject("referencias").getAsJsonObject(nombre)
                val sugerencia = clasificador.clasificar(copiarAImagen(nombre).absolutePath)

                assertNotNull("sin sugerencia para $nombre", sugerencia)
                assertEquals("clase de $nombre", esperado.get("top1").asString, sugerencia!!.categoria.name.lowercase())
                val confianza = esperado.get("confianza").asFloat
                assertEquals("confianza de $nombre", confianza, sugerencia.confianza, TOLERANCIA)
            }
        }

    @Test
    fun unaFotoQueNoExisteNoDaSugerenciaNiRompeLaApp() =
        runBlocking {
            val configuracion = cargarConfiguracion(contextoApp)
            val clasificador =
                ClasificadorResiduoImpl(
                    PreprocesadorImagen(configuracion.lado),
                    MotorLiteRt(cargarInterprete(contextoApp, configuracion), configuracion.clases.size),
                    configuracion,
                )

            assertNull(clasificador.clasificar(File(contextoApp.cacheDir, "no-existe.jpg").absolutePath))
        }
}
