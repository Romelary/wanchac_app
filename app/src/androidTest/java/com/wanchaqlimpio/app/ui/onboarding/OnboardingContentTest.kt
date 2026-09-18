package com.wanchaqlimpio.app.ui.onboarding

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import org.junit.Rule
import org.junit.Test

private const val TEXTO_BOTON = "Aceptar y continuar"

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

        composeTestRule.onNode(isToggleable()).performClick()

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

        composeTestRule.onNode(isToggleable()).performClick()
        composeTestRule.onNodeWithText(TEXTO_BOTON).performClick()

        assert(aceptado) { "Se esperaba que onAceptar() se invocara al presionar el botón" }
    }
}
