package com.wanchaqlimpio.app.ui.misreportes

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.FalloDeEnvio
import com.wanchaqlimpio.app.domain.model.MotivoFalloEnvio
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.SeguimientoDeEnvio
import com.wanchaqlimpio.app.ui.common.formatearFechaHora
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun reporte(
    id: Long,
    estado: EstadoReporte = EstadoReporte.PENDIENTE,
    envio: SeguimientoDeEnvio = SeguimientoDeEnvio(),
) = Reporte(
    id = id,
    idAnonimo = "device-1",
    categoria = CategoriaResiduo.VIDRIO,
    estado = estado,
    latitud = -13.530,
    longitud = -71.950,
    fotoPath = "foto-$id.jpg",
    fechaHoraEpochMillis = 1_790_000_000_000L,
    envio = envio,
)

/**
 * Lo que el ciudadano (y quien hace una prueba de campo) ve del envío en «Mis reportes»: cuántos reportes siguen sin
 * enviarse, por qué, y el botón para reintentar sin esperar. Sin él, un reporte «pendiente» no explica nada.
 */
@RunWith(RobolectricTestRunner::class)
class MisReportesEnvioContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val contexto: Context get() = ApplicationProvider.getApplicationContext()

    private fun texto(
        id: Int,
        vararg argumentos: Any,
    ) = contexto.getString(id, *argumentos)

    private fun mostrar(
        vararg reportes: Reporte,
        onEnviarAhora: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            WanchaqLimpioTheme {
                MisReportesContent(
                    uiState = MisReportesUiState.Lista(reportes.toList()),
                    onEliminar = {},
                    onEnviarAhora = onEnviarAhora,
                )
            }
        }
    }

    @Test
    fun `con reportes pendientes se avisa cuantos son y Enviar ahora los reintenta`() {
        var pedidos = 0
        mostrar(reporte(1), reporte(2), reporte(3, EstadoReporte.SINCRONIZADO), onEnviarAhora = { pedidos++ })

        composeTestRule
            .onNodeWithText(contexto.resources.getQuantityString(R.plurals.mis_reportes_pendientes, 2, 2))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(texto(R.string.mis_reportes_boton_enviar_ahora)).performClick()

        assertEquals(1, pedidos)
    }

    @Test
    fun `con un solo reporte pendiente el aviso va en singular`() {
        mostrar(reporte(1))

        composeTestRule
            .onNodeWithText(contexto.resources.getQuantityString(R.plurals.mis_reportes_pendientes, 1, 1))
            .assertIsDisplayed()
    }

    @Test
    fun `sin reportes pendientes no hay aviso ni boton Enviar ahora`() {
        mostrar(reporte(1, EstadoReporte.SINCRONIZADO), reporte(2, EstadoReporte.RECHAZADO))

        composeTestRule.onNodeWithText(texto(R.string.mis_reportes_boton_enviar_ahora)).assertDoesNotExist()
    }

    @Test
    fun `un pendiente que ya fallo dice cuantos intentos lleva y por que fallo`() {
        val envio =
            SeguimientoDeEnvio(intentos = 3, ultimoFallo = FalloDeEnvio(MotivoFalloEnvio.SIN_CONEXION))
        mostrar(reporte(1, envio = envio))

        composeTestRule
            .onNodeWithText(texto(R.string.envio_detalle_pendiente, 3, texto(R.string.envio_motivo_sin_conexion)))
            .assertIsDisplayed()
    }

    @Test
    fun `un pendiente que aun no se intento no muestra ningun detalle de fallo`() {
        mostrar(reporte(1))

        composeTestRule.onNodeWithText("Intentos", substring = true).assertDoesNotExist()
    }

    @Test
    fun `un reporte enviado dice cuando lo acepto el servidor`() {
        val enviadoEn = 1_790_000_123_000L
        mostrar(
            reporte(1, EstadoReporte.SINCRONIZADO, SeguimientoDeEnvio(intentos = 1, enviadoEpochMillis = enviadoEn)),
        )

        composeTestRule
            .onNodeWithText(texto(R.string.envio_detalle_enviado, formatearFechaHora(enviadoEn)))
            .assertIsDisplayed()
    }

    @Test
    fun `un reporte rechazado dice el motivo y que se puede eliminar y reportar de nuevo`() {
        val envio =
            SeguimientoDeEnvio(
                intentos = 1,
                ultimoFallo = FalloDeEnvio(MotivoFalloEnvio.RECHAZADO_POR_EL_SERVIDOR, 422),
            )
        mostrar(reporte(1, EstadoReporte.RECHAZADO, envio))

        composeTestRule.onNodeWithText(texto(R.string.estado_rechazado)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(texto(R.string.envio_detalle_rechazado, texto(R.string.envio_motivo_rechazado)))
            .assertIsDisplayed()
    }
}
