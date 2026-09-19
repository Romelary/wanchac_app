package com.wanchaqlimpio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.width
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.ui.common.ANCHO_MAXIMO_CONTENIDO
import com.wanchaqlimpio.app.ui.common.ContenidoCentrado
import com.wanchaqlimpio.app.ui.onboarding.OnboardingContent
import com.wanchaqlimpio.app.ui.reporte.ReporteConfirmacionContent
import com.wanchaqlimpio.app.ui.reporte.ReporteEstadoContent
import com.wanchaqlimpio.app.ui.reporte.ReporteUiState
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TABLET = "w1000dp-h700dp" // ≈ una Galaxy Tab S9+ en horizontal
private const val TABLET_VERTICAL = "w700dp-h1000dp"
private const val TELEFONO = "w360dp-h640dp"
private const val TELEFONO_HORIZONTAL = "w640dp-h360dp"

/** La foto no existe: la pantalla debe funcionar sin vista previa. */
private const val FOTO_INEXISTENTE = "/no/existe.jpg"
private const val MARGEN_DP = 1f // tolerancia de redondeo al medir

/**
 * Comprueba la ADAPTACIÓN a distintos tamaños de ventana sin necesitar un dispositivo: Robolectric simula la
 * pantalla (`qualifiers`) y Compose mide el contenido real. No sustituye a probar en una tablet de verdad
 * (ver `docs/prueba-en-dispositivo.md`: aquí no hay cámara, barras del sistema reales, ni rotación física),
 * pero atrapa de inmediato que un cambio vuelva a estirar el contenido de borde a borde.
 */
@RunWith(RobolectricTestRunner::class)
class AdaptabilidadTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun anchoDe(etiqueta: String) =
        composeTestRule
            .onNodeWithTag(etiqueta)
            .getUnclippedBoundsInRoot()
            .width.value

    private fun izquierdaDe(etiqueta: String) =
        composeTestRule
            .onNodeWithTag(etiqueta)
            .getUnclippedBoundsInRoot()
            .left.value

    private fun mostrarContenidoCentrado() {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                ContenidoCentrado {
                    Box(Modifier.fillMaxSize().testTag("contenido"))
                }
            }
        }
    }

    // --- El contenedor -----------------------------------------------------------------------------------

    @Test
    @Config(qualifiers = TABLET)
    fun `en una tablet el contenido no pasa del ancho maximo de lectura y queda centrado`() {
        mostrarContenidoCentrado()

        assertEquals(ANCHO_MAXIMO_CONTENIDO.value, anchoDe("contenido"), MARGEN_DP)
        assertEquals((1000f - 640f) / 2f, izquierdaDe("contenido"), MARGEN_DP)
    }

    @Test
    @Config(qualifiers = TELEFONO)
    fun `en un telefono el contenido ocupa todo el ancho igual que antes`() {
        mostrarContenidoCentrado()

        assertEquals(360f, anchoDe("contenido"), MARGEN_DP)
        assertEquals(0f, izquierdaDe("contenido"), MARGEN_DP)
    }

    @Test
    @Config(qualifiers = TABLET_VERTICAL)
    fun `en una tablet vertical de 700 dp tambien se limita a 640 dp`() {
        mostrarContenidoCentrado()

        assertEquals(640f, anchoDe("contenido"), MARGEN_DP)
    }

    // --- Pantallas reales ------------------------------------------------------------------------------

    @Test
    @Config(qualifiers = TABLET)
    fun `el consentimiento en una tablet no estira el boton de borde a borde`() {
        composeTestRule.setContent { WanchaqLimpioTheme { OnboardingContent(onAceptar = {}) } }

        val boton = composeTestRule.onNodeWithText("Aceptar y continuar").getUnclippedBoundsInRoot()

        // 640 dp de contenido menos 24 dp de margen a cada lado.
        assertTrue("el botón mide ${boton.width.value} dp", boton.width.value <= 640f - 48f + MARGEN_DP)
        assertTrue("el botón debe quedar centrado, no pegado a la izquierda", boton.left.value >= (1000f - 640f) / 2f)
    }

    @Test
    @Config(qualifiers = TELEFONO)
    fun `el consentimiento en un telefono conserva el boton a lo ancho de la pantalla`() {
        composeTestRule.setContent { WanchaqLimpioTheme { OnboardingContent(onAceptar = {}) } }

        val boton = composeTestRule.onNodeWithText("Aceptar y continuar").getUnclippedBoundsInRoot()

        assertEquals(360f - 48f, boton.width.value, MARGEN_DP)
    }

    @Test
    @Config(qualifiers = TABLET)
    fun `un mensaje largo de rechazo en una tablet se parte dentro del ancho de lectura`() {
        // Largo a propósito: si el mensaje cupiera en una línea corta, la prueba pasaría aunque no hubiera límite.
        val mensaje = "Esta ubicación está fuera del distrito de Wanchaq. ".repeat(12).trim()
        composeTestRule.setContent {
            WanchaqLimpioTheme { ReporteEstadoContent(uiState = ReporteUiState.Rechazado(mensaje), onReintentar = {}) }
        }

        val texto = composeTestRule.onNodeWithText(mensaje).getUnclippedBoundsInRoot()

        // 640 dp de contenido menos 24 dp de margen a cada lado. Sin el límite mediría casi 950 dp.
        assertTrue("el mensaje mide ${texto.width.value} dp", texto.width.value <= 640f - 48f + MARGEN_DP)
        composeTestRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = TABLET)
    fun `la confirmacion en una tablet muestra las seis categorias y los dos botones`() {
        mostrarConfirmacion()

        listOf(
            "Plástico",
            "Cartón",
            "Vidrio",
            "Orgánico",
            "Escombros",
            "Mixto",
            "Guardar reporte",
            "Tomar otra foto",
        ).forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = TELEFONO_HORIZONTAL)
    fun `en un telefono horizontal (poca altura) los botones siguen alcanzables gracias al desplazamiento`() {
        mostrarConfirmacion()

        // Con solo 360 dp de alto no cabe todo a la vez: el contenido debe poder desplazarse hasta los botones.
        composeTestRule.onNodeWithText("Guardar reporte").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Tomar otra foto").performScrollTo().assertIsDisplayed()
    }

    private fun mostrarConfirmacion() {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                ReporteConfirmacionContent(
                    estado =
                        ReporteUiState.Confirmando(
                            FOTO_INEXISTENTE,
                            sugerencia = null,
                            seleccionada = CategoriaResiduo.MIXTO,
                        ),
                    onSeleccionar = {},
                    onGuardar = {},
                    onDescartar = {},
                )
            }
        }
    }
}
