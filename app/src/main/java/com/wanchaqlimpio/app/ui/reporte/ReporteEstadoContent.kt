package com.wanchaqlimpio.app.ui.reporte

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.domain.usecase.PRECISION_MAXIMA_METROS
import com.wanchaqlimpio.app.ui.common.ContenidoCentrado
import kotlin.math.roundToInt

/**
 * Contenido "stateless" de los estados posteriores a la captura de la foto
 * (clasificando, procesando, rechazado, guardado). No incluye `Capturando`
 * porque ese requiere la cámara real (CameraX) y no es practico de probar
 * con Compose UI Testing — ver `ReporteScreen` para ese caso y `pruebas.md`
 * para el criterio de qué se prueba y qué no. Tampoco `Confirmando`, que tiene
 * su propio contenido: [ReporteConfirmacionContent].
 *
 * Usa `Scaffold` (respeta las barras del sistema, que con pantalla completa de borde a borde ya no
 * "empujan" el contenido) y [ContenidoCentrado] (no se estira en una tablet).
 */
@Composable
fun ReporteEstadoContent(
    uiState: ReporteUiState,
    onReintentar: () -> Unit,
    onReintentarUbicacion: () -> Unit = {},
    onTomarOtraFoto: () -> Unit = {},
) {
    Scaffold { innerPadding ->
        ContenidoCentrado(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                when (uiState) {
                    ReporteUiState.Capturando, is ReporteUiState.Confirmando -> Unit
                    ReporteUiState.Clasificando -> {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.reporte_clasificando))
                    }
                    is ReporteUiState.ProcesandoUbicacion -> ProcesandoUbicacion(uiState.precisionActualMetros)
                    is ReporteUiState.GpsImpreciso ->
                        GpsImpreciso(
                            precisionMetros = uiState.precisionMetros,
                            onReintentarUbicacion = onReintentarUbicacion,
                            onTomarOtraFoto = onTomarOtraFoto,
                        )
                    is ReporteUiState.Rechazado -> {
                        Text(uiState.mensaje)
                        Button(onClick = onReintentar) {
                            Text(stringResource(R.string.reporte_boton_reintentar))
                        }
                    }
                    ReporteUiState.Guardado -> {
                        Text(stringResource(R.string.reporte_guardado))
                        Button(onClick = onReintentar) {
                            Text(stringResource(R.string.reporte_boton_otro))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcesandoUbicacion(precisionActualMetros: Float?) {
    CircularProgressIndicator()
    Text(stringResource(R.string.reporte_procesando_ubicacion))
    precisionActualMetros?.let {
        Text(
            stringResource(
                R.string.reporte_procesando_ubicacion_precision,
                it.roundToInt(),
                PRECISION_MAXIMA_METROS.roundToInt(),
            ),
        )
    }
}

/** La foto se conserva: con unos segundos más a cielo abierto el GPS mejora y no hace falta repetir todo. */
@Composable
private fun GpsImpreciso(
    precisionMetros: Float?,
    onReintentarUbicacion: () -> Unit,
    onTomarOtraFoto: () -> Unit,
) {
    val maximo = PRECISION_MAXIMA_METROS.roundToInt()
    Text(
        if (precisionMetros == null) {
            stringResource(R.string.reporte_gps_sin_senal)
        } else {
            stringResource(R.string.reporte_gps_impreciso, precisionMetros.roundToInt(), maximo)
        },
    )
    Button(onClick = onReintentarUbicacion) { Text(stringResource(R.string.reporte_boton_reintentar_ubicacion)) }
    OutlinedButton(onClick = onTomarOtraFoto) { Text(stringResource(R.string.reporte_boton_otra_foto)) }
}
