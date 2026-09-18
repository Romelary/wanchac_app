package com.wanchaqlimpio.app.ui.misreportes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import org.junit.Rule
import org.junit.Test

private fun reporte(id: Long) =
    Reporte(
        id = id,
        idAnonimo = "device-1",
        categoria = CategoriaResiduo.VIDRIO,
        estado = EstadoReporte.PENDIENTE,
        latitud = -13.530,
        longitud = -71.950,
        fotoPath = "foto-$id.jpg",
        fechaHoraEpochMillis = 0L,
    )

class MisReportesContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sinReportesSeMuestraElMensajeVacio() {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                MisReportesContent(uiState = MisReportesUiState.Lista(emptyList()), onEliminar = {})
            }
        }

        composeTestRule.onNodeWithText("Aún no has hecho ningún reporte.").assertIsDisplayed()
    }

    @Test
    fun eliminarPideConfirmacionYSoloAlConfirmarSeInvocaElCallback() {
        var idEliminado: Long? = null
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                MisReportesContent(
                    uiState = MisReportesUiState.Lista(listOf(reporte(5))),
                    onEliminar = { idEliminado = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Eliminar").performClick()
        composeTestRule.onNodeWithText("¿Eliminar este reporte?").assertIsDisplayed()
        assert(idEliminado == null) { "No debe eliminarse antes de confirmar" }

        // Ahora hay dos botones "Eliminar" (el de la tarjeta y el del diálogo); el del diálogo es el último.
        composeTestRule.onAllNodesWithText("Eliminar").onLast().performClick()

        assert(idEliminado == 5L) { "Se esperaba eliminar el reporte 5, fue $idEliminado" }
    }

    @Test
    fun cancelarElDialogoNoEliminaNada() {
        var eliminado = false
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                MisReportesContent(
                    uiState = MisReportesUiState.Lista(listOf(reporte(5))),
                    onEliminar = { eliminado = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Eliminar").performClick()
        composeTestRule.onNodeWithText("Cancelar").performClick()

        assert(!eliminado) { "Cancelar no debe eliminar el reporte" }
    }
}
