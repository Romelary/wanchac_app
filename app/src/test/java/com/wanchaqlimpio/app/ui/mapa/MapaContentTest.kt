package com.wanchaqlimpio.app.ui.mapa

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.PuntoCritico
import com.wanchaqlimpio.app.domain.repository.MotivoFalloMapa
import com.wanchaqlimpio.app.ui.common.ANCHO_MAXIMO_CONTENIDO
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TABLET = "w1000dp-h700dp"

private fun punto(id: Long) = PuntoCritico(id, -13.53, -71.9475, 5, CategoriaResiduo.PLASTICO, 1_790_000_000_000)

/**
 * Prueba TODA la lógica visible del mapa sin Google Play services: el mapa de Google llega por un espacio (`mapa`) que
 * aquí se rellena con un texto. Lo que NO se puede comprobar en la JVM es el dibujo real de Google Maps (necesita una
 * clave y un dispositivo): eso está en el protocolo de prueba en dispositivo.
 */
@RunWith(RobolectricTestRunner::class)
class MapaContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val contexto: Context get() = ApplicationProvider.getApplicationContext()

    private fun texto(id: Int) = contexto.getString(id)

    private fun mostrar(
        estado: MapaUiState,
        avisarFaltaLaClave: Boolean = false,
        onActualizar: () -> Unit = {},
        mapaFalso: (List<PuntoCritico>) -> Unit = {},
    ) {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                MapaContent(
                    estado = estado,
                    avisarFaltaLaClave = avisarFaltaLaClave,
                    onActualizar = onActualizar,
                ) { puntos ->
                    mapaFalso(puntos)
                    Text("MAPA_FALSO:${puntos.size}")
                }
            }
        }
    }

    @Test
    fun `cargando muestra el indicador y NO el mapa`() {
        mostrar(MapaUiState.Cargando)

        composeTestRule.onNodeWithTag(MapaTags.CARGANDO).assertIsDisplayed()
        composeTestRule.onNodeWithTag(MapaTags.MAPA).assertDoesNotExist()
    }

    @Test
    fun `con focos se dibuja el mapa con exactamente esos puntos`() {
        var recibidos: List<PuntoCritico>? = null
        val puntos = listOf(punto(1), punto(2), punto(3))
        mostrar(MapaUiState.ConPuntos(puntos), mapaFalso = { recibidos = it })

        composeTestRule.onNodeWithText("MAPA_FALSO:3").assertIsDisplayed()
        assertEquals(puntos, recibidos)
    }

    @Test
    fun `el contador dice cuantos focos hay, en singular y en plural`() {
        mostrar(MapaUiState.ConPuntos(listOf(punto(1))))
        composeTestRule
            .onNodeWithText(contexto.resources.getQuantityString(R.plurals.mapa_focos_confirmados, 1, 1))
            .assertIsDisplayed()
    }

    @Test
    fun `el contador en plural`() {
        mostrar(MapaUiState.ConPuntos(listOf(punto(1), punto(2))))
        composeTestRule
            .onNodeWithText(contexto.resources.getQuantityString(R.plurals.mapa_focos_confirmados, 2, 2))
            .assertIsDisplayed()
    }

    @Test
    fun `sin focos se ve el mapa VACIO del distrito y un mensaje que explica por que`() {
        var recibidos: List<PuntoCritico>? = null
        mostrar(MapaUiState.SinPuntos, mapaFalso = { recibidos = it })

        composeTestRule.onNodeWithText("MAPA_FALSO:0").assertIsDisplayed()
        assertEquals(emptyList<PuntoCritico>(), recibidos)
        composeTestRule.onNodeWithText(texto(R.string.mapa_sin_focos_titulo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.mapa_sin_focos_texto)).assertIsDisplayed()
    }

    @Test
    fun `sin focos, Actualizar vuelve a pedir la lista`() {
        var pedidas = 0
        mostrar(MapaUiState.SinPuntos, onActualizar = { pedidas++ })

        composeTestRule.onNodeWithTag(MapaTags.BOTON_ACTUALIZAR).performClick()

        assertEquals(1, pedidas)
    }

    @Test
    fun `cada motivo de error muestra su propio mensaje y Reintentar funciona`() {
        val esperados =
            mapOf(
                MotivoFalloMapa.SIN_CONEXION to R.string.mapa_error_sin_conexion,
                MotivoFalloMapa.SERVIDOR to R.string.mapa_error_servidor,
                MotivoFalloMapa.RESPUESTA_INVALIDA to R.string.mapa_error_respuesta,
            )
        // si algún día se añade un motivo de fallo, esta comprobación obliga a darle su texto
        assertEquals(MotivoFalloMapa.entries.toSet(), esperados.keys)
        var pedidas = 0
        val motivo = mutableStateOf(MotivoFalloMapa.SIN_CONEXION)
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                MapaContent(
                    MapaUiState.Error(motivo.value),
                    false,
                    { pedidas++ },
                ) { Text("MAPA_FALSO") }
            }
        }

        esperados.forEach { (m, recurso) ->
            motivo.value = m
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(texto(recurso)).assertIsDisplayed()
            composeTestRule.onNodeWithTag(MapaTags.BOTON_REINTENTAR).performClick()
        }
        assertEquals(3, pedidas)
        composeTestRule.onNodeWithText("MAPA_FALSO").assertDoesNotExist() // en error no se dibuja el mapa
    }

    @Test
    fun `el aviso de clave de Maps solo aparece cuando se pide`() {
        mostrar(MapaUiState.SinPuntos, avisarFaltaLaClave = true)
        composeTestRule.onNodeWithTag(MapaTags.AVISO_CLAVE).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.mapa_aviso_sin_clave)).assertIsDisplayed()
    }

    @Test
    fun `sin la bandera no hay aviso de clave`() {
        mostrar(MapaUiState.SinPuntos, avisarFaltaLaClave = false)
        composeTestRule.onNodeWithTag(MapaTags.AVISO_CLAVE).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = TABLET)
    fun `en una tablet los mensajes no se estiran de borde a borde`() {
        mostrar(MapaUiState.Error(MotivoFalloMapa.SERVIDOR))

        val ancho =
            composeTestRule
                .onNodeWithTag(MapaTags.ERROR)
                .getUnclippedBoundsInRoot()
                .width.value
        assertTrue("ancho $ancho dp", ancho <= ANCHO_MAXIMO_CONTENIDO.value + 1f)
    }
}
