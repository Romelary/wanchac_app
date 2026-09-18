package com.wanchaqlimpio.app.ml.clasificador

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Comprueba los ARCHIVOS REALES del modelo que viajan dentro de la app (`assets/clasificador/`), no datos de
 * ejemplo. Es la mitad, del lado de Kotlin, del contrato que `ml/entrenamiento/probar_notebook.py` verifica del
 * lado de Python: si alguien reemplaza el modelo y deja desactualizados `labels.txt` o `metadata.json`, esta
 * prueba falla en vez de que la app etiquete mal todas las predicciones sin dar ningún error.
 *
 * Los tests unitarios de Gradle corren con la carpeta del módulo (`app/`) como directorio de trabajo.
 */
class ContratoModeloAssetsTest {
    private val carpeta = File("src/main/assets/clasificador")
    private val labels by lazy { File(carpeta, "labels.txt").readText(Charsets.UTF_8) }
    private val metadataTexto by lazy { File(carpeta, "metadata.json").readText(Charsets.UTF_8) }
    private val metadata by lazy { Gson().fromJson(metadataTexto, JsonObject::class.java) }

    @Test
    fun `los tres archivos del modelo existen`() {
        listOf("clasificador_residuos.tflite", "labels.txt", "metadata.json").forEach {
            assertTrue("falta assets/clasificador/$it", File(carpeta, it).isFile)
        }
    }

    @Test
    fun `labels y metadata reales cumplen el contrato que exige la app`() {
        val configuracion = ConfiguracionModelo.desde(labels, metadataTexto)

        assertEquals(224, configuracion.lado)
        assertEquals(4, configuracion.clases.size)
    }

    @Test
    fun `el orden de las clases del modelo es el del enum CategoriaResiduo sin las categorias manuales`() {
        // Espejo de test_labels_coincide_con_el_enum_de_kotlin (Python). ESCOMBROS y MIXTO son de elección manual.
        val manuales = setOf(CategoriaResiduo.ESCOMBROS, CategoriaResiduo.MIXTO)

        val esperado = CategoriaResiduo.entries.filter { it !in manuales }

        assertEquals(esperado, ConfiguracionModelo.desde(labels, metadataTexto).clases)
        val declaradas = metadata.getAsJsonArray("categorias_manuales").map { it.asString.uppercase() }.toSet()
        assertEquals(manuales.map { it.name }.toSet(), declaradas)
    }

    @Test
    fun `el umbral que aplica la app esta en el rango esperado`() {
        val umbral = ConfiguracionModelo.desde(labels, metadataTexto).umbralConfianza

        // 0,70 es PROVISIONAL (ver docs/progreso.md, Sesión 17). Si se recalibra con fotos de calle, actualizar aquí.
        assertEquals(0.70f, umbral)
    }

    @Test
    fun `el modelo empaquetado es exactamente el que describe metadata`() {
        val modelo = File(carpeta, "clasificador_residuos.tflite")
        val bytes = MessageDigest.getInstance("SHA-256").digest(modelo.readBytes())
        val huella = bytes.joinToString("") { "%02x".format(it) }

        assertEquals(
            "el .tflite no corresponde a metadata.json (¿se cambió uno sin el otro?)",
            metadata.get("sha256_tflite").asString,
            huella,
        )
        assertTrue("un modelo de menos de 1 MB no puede ser MobileNetV2", modelo.length() > 1_000_000)
    }

    @Test
    fun `cada correccion manual de metadata deja constancia de que cambio y por que`() {
        val correcciones = metadata.getAsJsonArray("correcciones_manuales")

        assertTrue("metadata.json fue editado a mano y debe registrarlo", correcciones.size() > 0)
        correcciones.forEach {
            val c = it.asJsonObject
            listOf("fecha", "campo", "antes", "despues", "motivo").forEach { clave ->
                assertTrue("una corrección no dice '$clave'", c.has(clave))
            }
        }
    }
}
