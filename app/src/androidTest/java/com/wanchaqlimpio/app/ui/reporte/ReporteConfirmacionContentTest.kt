package com.wanchaqlimpio.app.ui.reporte

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import org.junit.Rule
import org.junit.Test

/** Lo que la pantalla dice cuando el modelo duda o no está disponible. */
private const val TEXTO_SIN_SUGERENCIA = "No pude sugerir un tipo con seguridad. Elige el que corresponda."

/** El archivo de foto no existe: la pantalla debe seguir funcionando sin la vista previa. */
private const val FOTO_INEXISTENTE = "/no/existe.jpg"

class ReporteConfirmacionContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(
        estado: ReporteUiState.Confirmando,
        onSeleccionar: (CategoriaResiduo) -> Unit = {},
        onGuardar: () -> Unit = {},
        onDescartar: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                ReporteConfirmacionContent(estado, onSeleccionar, onGuardar, onDescartar)
            }
        }
    }

    @Test
    fun conSugerenciaSeguraDiceCualEsYLaDejaSeleccionada() {
        val sugerencia =
            SugerenciaIa(CategoriaResiduo.PLASTICO, confianza = 0.9f, propuesta = CategoriaResiduo.PLASTICO)

        mostrar(ReporteUiState.Confirmando(FOTO_INEXISTENTE, sugerencia, seleccionada = CategoriaResiduo.PLASTICO))

        val texto = "Sugerencia automática: Plástico. Cámbiala si no es correcta."
        composeTestRule.onNodeWithText(texto).assertIsDisplayed()
        composeTestRule.onNodeWithText("Plástico").assertIsSelected()
    }

    @Test
    fun siElModeloDudoNoAfirmaNadaYPideElegir() {
        // El modelo cree que es cartón pero con poca confianza: propone MIXTO y la pantalla no debe decir "cartón".
        val sugerencia = SugerenciaIa(CategoriaResiduo.CARTON, confianza = 0.4f, propuesta = CategoriaResiduo.MIXTO)

        mostrar(ReporteUiState.Confirmando(FOTO_INEXISTENTE, sugerencia, seleccionada = CategoriaResiduo.MIXTO))

        composeTestRule.onNodeWithText(TEXTO_SIN_SUGERENCIA).assertIsDisplayed()
        composeTestRule.onNodeWithText("Mixto").assertIsSelected()
    }

    @Test
    fun sinModeloDisponibleSeEligeAMano() {
        mostrar(ReporteUiState.Confirmando(FOTO_INEXISTENTE, sugerencia = null, seleccionada = CategoriaResiduo.MIXTO))

        composeTestRule.onNodeWithText(TEXTO_SIN_SUGERENCIA).assertIsDisplayed()
    }

    @Test
    fun seOfrecenLasSeisCategoriasIncluidasLasManuales() {
        mostrar(ReporteUiState.Confirmando(FOTO_INEXISTENTE, sugerencia = null, seleccionada = CategoriaResiduo.MIXTO))

        listOf("Plástico", "Cartón", "Vidrio", "Orgánico", "Escombros", "Mixto").forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun alTocarUnaCategoriaSeAvisaCualEs() {
        var elegida: CategoriaResiduo? = null
        mostrar(
            ReporteUiState.Confirmando(FOTO_INEXISTENTE, sugerencia = null, seleccionada = CategoriaResiduo.MIXTO),
            onSeleccionar = { elegida = it },
        )

        composeTestRule.onNodeWithText("Escombros").performClick()

        assert(elegida == CategoriaResiduo.ESCOMBROS) { "Se esperaba ESCOMBROS y fue $elegida" }
    }

    @Test
    fun guardarYTomarOtraFotoInvocanSusCallbacks() {
        var guardado = false
        var descartado = false
        mostrar(
            ReporteUiState.Confirmando(FOTO_INEXISTENTE, sugerencia = null, seleccionada = CategoriaResiduo.MIXTO),
            onGuardar = { guardado = true },
            onDescartar = { descartado = true },
        )

        composeTestRule.onNodeWithText("Guardar reporte").performClick()
        composeTestRule.onNodeWithText("Tomar otra foto").performClick()

        assert(guardado) { "Se esperaba onGuardar()" }
        assert(descartado) { "Se esperaba onDescartar()" }
    }
}
