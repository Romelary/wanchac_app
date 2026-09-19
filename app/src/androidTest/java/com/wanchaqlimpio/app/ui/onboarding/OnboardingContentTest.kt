package com.wanchaqlimpio.app.ui.onboarding

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import org.junit.Rule
import org.junit.Test

private const val TEXTO_BOTON = "Aceptar y continuar"

/*
 * En una tablet en horizontal (1752 px de alto en la Galaxy Tab S9+) la casilla y el botón quedan por debajo del borde
 * visible: el contenido se desplaza, así que hay que llegar hasta ellos (`performScrollTo`) antes de tocarlos. Sin
 * eso, el toque cae fuera de la zona visible y no hace nada (fallaron las dos pruebas en el dispositivo, 2026-09-19).
 */

class OnboardingContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elBotonEmpiezaDeshabilitadoHastaAceptarElCheckbox() {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                OnboardingContent(onAceptar = {})
            }
        }

        composeTestRule.onNodeWithText(TEXTO_BOTON).assertIsNotEnabled()

        composeTestRule.onNode(isToggleable()).performScrollTo().performClick()

        composeTestRule.onNodeWithText(TEXTO_BOTON).assertIsEnabled()
    }

    @Test
    fun alAceptarSeInvocaElCallback() {
        var aceptado = false

        composeTestRule.setContent {
            WanchaqLimpioTheme {
                OnboardingContent(onAceptar = { aceptado = true })
            }
        }

        composeTestRule.onNode(isToggleable()).performScrollTo().performClick()
        composeTestRule.onNodeWithText(TEXTO_BOTON).performScrollTo().performClick()

        assert(aceptado) { "Se esperaba que onAceptar() se invocara al presionar el botón" }
    }
}
