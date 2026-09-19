package com.wanchaqlimpio.app.ui.ajustes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import org.junit.Rule
import org.junit.Test

class AjustesContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(
        uiState: AjustesUiState = AjustesUiState(idAnonimo = "device-1"),
        correoContacto: String = "contacto@proyecto.test",
        onBorrarTodo: () -> Unit = {},
        onCompartirRegistro: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                AjustesContent(
                    uiState = uiState,
                    versionName = "1.0",
                    correoContacto = correoContacto,
                    onEscribirCorreo = {},
                    onBorrarTodo = onBorrarTodo,
                    onCompartirRegistro = onCompartirRegistro,
                )
            }
        }
    }

    @Test
    fun muestraElCodigoAnonimoYElCorreoDeContacto() {
        mostrar()

        composeTestRule.onNodeWithText("device-1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("contacto@proyecto.test").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sinCorreoConfiguradoSeAvisaEnVezDeMostrarUnaDireccionVacia() {
        mostrar(correoContacto = "")

        composeTestRule
            .onNodeWithText("Correo de contacto sin configurar: falta definirlo antes de publicar la app.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun borrarTodoPideConfirmacionYSoloAlConfirmarSeInvocaElCallback() {
        var borrado = false
        mostrar(onBorrarTodo = { borrado = true })

        composeTestRule.onNodeWithText("Borrar todos mis datos").performScrollTo().performClick()
        composeTestRule.onNodeWithText("¿Borrar todos tus datos?").assertIsDisplayed()
        assert(!borrado) { "No debe borrarse antes de confirmar" }

        composeTestRule.onNodeWithText("Borrar todo").performClick()

        assert(borrado) { "Se esperaba que se invocara onBorrarTodo() al confirmar" }
    }

    @Test
    fun cancelarElDialogoNoBorraNada() {
        var borrado = false
        mostrar(onBorrarTodo = { borrado = true })

        composeTestRule.onNodeWithText("Borrar todos mis datos").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Cancelar").performClick()

        assert(!borrado) { "Cancelar no debe borrar nada" }
    }

    @Test
    fun mientrasSeBorraElBotonQuedaDeshabilitado() {
        mostrar(uiState = AjustesUiState(idAnonimo = "device-1", borrando = true))

        composeTestRule.onNodeWithText("Borrando…").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun compartirElRegistroDeEnviosInvocaElCallbackYExplicaQueNoLlevaFotosNiUbicaciones() {
        var compartido = false
        mostrar(onCompartirRegistro = { compartido = true })

        composeTestRule.onNodeWithText("Registro de envíos").performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("No incluye fotos, ubicaciones ni tu código anónimo", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Compartir registro de envíos").performScrollTo().performClick()

        assert(compartido) { "Se esperaba que se invocara onCompartirRegistro()" }
    }
}
