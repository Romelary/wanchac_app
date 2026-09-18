package com.wanchaqlimpio.app.ml.clasificador

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LABELS = "plastico\ncarton\nvidrio\norganico\n"

/** metadata.json mínimo válido; cada prueba cambia solo lo que quiere romper. */
private fun metadata(
    clases: String = "\"plastico\",\"carton\",\"vidrio\",\"organico\"",
    manuales: String = "\"escombros\",\"mixto\"",
    umbralApp: String = "\"umbral_confianza_app\": 0.70,",
    umbralSugerido: String = "0.5",
    entrada: String = "[1,224,224,3]",
    salida: String = "[1,4]",
) = """
    {
      "clases": [$clases],
      "categorias_manuales": [$manuales],
      $umbralApp
      "umbral_confianza_sugerido": $umbralSugerido,
      "entrada": {"forma": $entrada},
      "salida": {"forma": $salida}
    }
    """.trimIndent()

class ConfiguracionModeloTest {
    @Test
    fun `un contrato valido se lee con las clases en el orden de la salida del modelo`() {
        val configuracion = ConfiguracionModelo.desde(LABELS, metadata())

        assertEquals(
            listOf(
                CategoriaResiduo.PLASTICO,
                CategoriaResiduo.CARTON,
                CategoriaResiduo.VIDRIO,
                CategoriaResiduo.ORGANICO,
            ),
            configuracion.clases,
        )
        assertEquals(224, configuracion.lado)
    }

    @Test
    fun `el umbral que aplica la app manda sobre el sugerido por el cuaderno`() {
        assertEquals(0.70f, ConfiguracionModelo.desde(LABELS, metadata()).umbralConfianza)
    }

    @Test
    fun `sin umbral propio de la app se usa el sugerido por el cuaderno`() {
        val configuracion = ConfiguracionModelo.desde(LABELS, metadata(umbralApp = "", umbralSugerido = "0.55"))

        assertEquals(0.55f, configuracion.umbralConfianza)
    }

    @Test
    fun `una etiqueta que no es categoria de la app se rechaza`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                val clases = "\"plastico\",\"metal\""
                ConfiguracionModelo.desde("plastico\nmetal\n", metadata(clases = clases, salida = "[1,2]"))
            }
        assertTrue(error.message!!.contains("metal"))
    }

    @Test
    fun `si labels y metadata difieren en el orden se rechaza porque se etiquetaria mal todo`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracionModelo.desde(LABELS, metadata(clases = "\"carton\",\"plastico\",\"vidrio\",\"organico\""))
        }
    }

    @Test
    fun `una categoria manual no puede ser tambien una clase del modelo`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracionModelo.desde(LABELS, metadata(manuales = "\"organico\",\"mixto\""))
        }
    }

    @Test
    fun `una clase repetida se rechaza`() {
        assertThrows(IllegalArgumentException::class.java) {
            val clases = "\"plastico\",\"plastico\""
            ConfiguracionModelo.desde("plastico\nplastico\n", metadata(clases = clases, salida = "[1,2]"))
        }
    }

    @Test
    fun `una entrada que no es cuadrada de 3 canales se rechaza`() {
        listOf("[1,224,200,3]", "[1,224,224,1]").forEach { entrada ->
            assertThrows(IllegalArgumentException::class.java) {
                ConfiguracionModelo.desde(LABELS, metadata(entrada = entrada))
            }
        }
    }

    @Test
    fun `una salida que no coincide con el numero de clases se rechaza`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracionModelo.desde(LABELS, metadata(salida = "[1,5]"))
        }
    }

    @Test
    fun `un umbral fuera de cero a uno se rechaza`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracionModelo.desde(LABELS, metadata(umbralApp = "\"umbral_confianza_app\": 1.5,"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConfiguracionModelo.desde(LABELS, metadata(umbralApp = "\"umbral_confianza_app\": 0,"))
        }
    }

    @Test
    fun `un metadata vacio se rechaza con un mensaje claro`() {
        assertThrows(IllegalArgumentException::class.java) { ConfiguracionModelo.desde(LABELS, "") }
    }
}
