package com.wanchaqlimpio.app.ui.reporte

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wanchaqlimpio.app.R

/**
 * Contenido "stateless" de los estados posteriores a la captura de la foto
 * (procesando, rechazado, guardado). No incluye el estado `Capturando`
 * porque ese requiere la cámara real (CameraX) y no es practico de probar
 * con Compose UI Testing — ver `ReporteScreen` para ese caso y `pruebas.md`
 * para el criterio de qué se prueba y qué no.
 */
@Composable
fun ReporteEstadoContent(
    uiState: ReporteUiState,
    onReintentar: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (uiState) {
            ReporteUiState.Capturando -> Unit
            ReporteUiState.ProcesandoUbicacion -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.reporte_procesando_ubicacion))
            }
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
