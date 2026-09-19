package com.wanchaqlimpio.app.ui.reporte

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import org.junit.Rule
import org.junit.Test

class ReporteEstadoContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun estadoRechazadoMuestraElMensajeYElBotonReintentarFunciona() {
        var reintentoInvocado = false
        val mensaje = "La precisión del GPS es insuficiente."

        composeTestRule.setContent {
            WanchaqLimpioTheme {
                ReporteEstadoContent(
                    uiState = ReporteUiState.Rechazado(mensaje),
                    onReintentar = { reintentoInvocado = true },
                )
            }
        }

        composeTestRule.onNodeWithText(mensaje).assertIsDisplayed()
        composeTestRule.onNodeWithText("Reintentar").performClick()

        assert(reintentoInvocado) { "Se esperaba que onReintentar() se invocara al presionar el botón" }
    }

    @Test
    fun estadoGuardadoMuestraElMensajeDeExito() {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                ReporteEstadoContent(uiState = ReporteUiState.Guardado, onReintentar = {})
            }
        }

        composeTestRule.onNodeWithText("Reportar otro punto").assertIsDisplayed()
    }

    @Test
    fun estadoClasificandoMuestraQueSeEstaAnalizandoLaFoto() {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                ReporteEstadoContent(uiState = ReporteUiState.Clasificando, onReintentar = {})
            }
        }

        composeTestRule.onNodeWithText("Analizando la foto…").assertIsDisplayed()
    }

    @Test
    fun estadoProcesandoUbicacionMuestraElTextoDeCarga() {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                ReporteEstadoContent(uiState = ReporteUiState.ProcesandoUbicacion(), onReintentar = {})
            }
        }

        composeTestRule.onNodeWithText("Obteniendo tu ubicación…").assertIsDisplayed()
    }
}
