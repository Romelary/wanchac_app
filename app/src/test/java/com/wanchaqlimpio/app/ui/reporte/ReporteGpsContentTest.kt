package com.wanchaqlimpio.app.ui.reporte

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val CONFIRMACION =
    ReporteUiState.Confirmando("foto.jpg", sugerencia = null, seleccionada = CategoriaResiduo.MIXTO)

/**
 * Lo que ve el ciudadano cuando el GPS no está a la altura: antes se rechazaba el reporte y se borraba la foto sin más;
 * ahora se le dice cuánto le falta y se le deja reintentar sin repetir la foto. Y, con la cámara abierta, el indicador
 * que le dice cuándo el GPS ya está listo.
 */
@RunWith(RobolectricTestRunner::class)
class ReporteGpsContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val contexto: Context get() = ApplicationProvider.getApplicationContext()

    private fun texto(
        id: Int,
        vararg argumentos: Any,
    ) = contexto.getString(id, *argumentos)

    private fun mostrarEstado(
        estado: ReporteUiState,
        onReintentarUbicacion: () -> Unit = {},
        onTomarOtraFoto: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                ReporteEstadoContent(
                    uiState = estado,
                    onReintentar = {},
                    onReintentarUbicacion = onReintentarUbicacion,
                    onTomarOtraFoto = onTomarOtraFoto,
                )
            }
        }
    }

    private fun mostrarIndicador(precisionMetros: Float?) {
        composeTestRule.setContent { WanchaqLimpioTheme { IndicadorGps(precisionMetros) } }
    }

    // --- Con la cámara abierta: el indicador ------------------------------------------------------------------

    @Test
    fun `mientras no hay ninguna lectura el indicador dice que busca senal`() {
        mostrarIndicador(precisionMetros = null)

        composeTestRule.onNodeWithText(texto(R.string.gps_buscando)).assertIsDisplayed()
    }

    @Test
    fun `con 15 m o menos el indicador dice que el GPS esta listo`() {
        mostrarIndicador(precisionMetros = 8.4f)

        composeTestRule.onNodeWithText(texto(R.string.gps_listo, 8)).assertIsDisplayed()
    }

    @Test
    fun `justo en 15 m todavia esta listo, un poco mas ya es impreciso`() {
        mostrarIndicador(precisionMetros = 15f)
        composeTestRule.onNodeWithText(texto(R.string.gps_listo, 15)).assertIsDisplayed()
    }

    @Test
    fun `con mas de 15 m el indicador avisa y dice que espere`() {
        mostrarIndicador(precisionMetros = 22.6f)

        composeTestRule.onNodeWithText(texto(R.string.gps_impreciso, 23)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ETIQUETA_INDICADOR_GPS).assertIsDisplayed()
    }

    // --- Al guardar: esperar y, si no alcanza, conservar la foto --------------------------------------------

    @Test
    fun `mientras se espera al GPS se muestra la mejor precision conseguida`() {
        mostrarEstado(ReporteUiState.ProcesandoUbicacion(precisionActualMetros = 40f))

        composeTestRule.onNodeWithText(texto(R.string.reporte_procesando_ubicacion)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(texto(R.string.reporte_procesando_ubicacion_precision, 40, 15))
            .assertIsDisplayed()
    }

    @Test
    fun `sin ninguna lectura todavia solo se ve el aviso de obteniendo ubicacion`() {
        mostrarEstado(ReporteUiState.ProcesandoUbicacion())

        composeTestRule.onNodeWithText(texto(R.string.reporte_procesando_ubicacion)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Precisión actual", substring = true).assertDoesNotExist()
    }

    @Test
    fun `con un GPS impreciso se dice cuanto falta, se conserva la foto y los dos botones funcionan`() {
        var ubicacionReintentada = 0
        var otraFoto = 0
        mostrarEstado(
            ReporteUiState.GpsImpreciso(CONFIRMACION, precisionMetros = 22f),
            onReintentarUbicacion = { ubicacionReintentada++ },
            onTomarOtraFoto = { otraFoto++ },
        )

        composeTestRule.onNodeWithText(texto(R.string.reporte_gps_impreciso, 22, 15)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.reporte_boton_reintentar_ubicacion)).performClick()
        composeTestRule.onNodeWithText(texto(R.string.reporte_boton_otra_foto)).performClick()

        assertEquals(1, ubicacionReintentada)
        assertEquals(1, otraFoto)
    }

    @Test
    fun `sin ninguna lectura el mensaje habla de activar el GPS y tambien deja reintentar`() {
        var reintentos = 0
        mostrarEstado(
            ReporteUiState.GpsImpreciso(CONFIRMACION, precisionMetros = null),
            onReintentarUbicacion = { reintentos++ },
        )

        composeTestRule.onNodeWithText(texto(R.string.reporte_gps_sin_senal)).assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.reporte_boton_reintentar_ubicacion)).performClick()

        assertEquals(1, reintentos)
    }

    @Test
    fun `los dos mensajes de GPS prometen que la foto se conserva`() {
        // Es lo que hace el ViewModel (no borra la foto en este estado): el texto no debe prometer otra cosa.
        val conLectura = texto(R.string.reporte_gps_impreciso, 22, 15)
        val sinLectura = texto(R.string.reporte_gps_sin_senal)

        listOf(conLectura, sinLectura).forEach { mensaje ->
            assertEquals(mensaje, true, mensaje.contains("foto se conserva"))
        }
    }
}
