package com.wanchaqlimpio.app.ui.misreportes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.ui.common.ContenidoCentrado
import com.wanchaqlimpio.app.ui.common.etiqueta
import com.wanchaqlimpio.app.ui.common.formatearFechaHora

@Composable
fun MisReportesScreen(viewModel: MisReportesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MisReportesContent(uiState = uiState, onEliminar = viewModel::eliminar)
}

/**
 * Contenido "stateless" (mismo patrón que OnboardingContent, ver pruebas.md):
 * se puede probar con Compose UI Testing sin infraestructura de Hilt.
 */
@Composable
fun MisReportesContent(
    uiState: MisReportesUiState,
    onEliminar: (Long) -> Unit,
) {
    // rememberSaveable: el diálogo de confirmación sobrevive a una rotación de pantalla.
    var idPorConfirmar by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold { innerPadding ->
        ContenidoCentrado(modifier = Modifier.padding(innerPadding)) {
            when (uiState) {
                MisReportesUiState.Cargando ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is MisReportesUiState.Lista ->
                    if (uiState.reportes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.mis_reportes_vacio),
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                    } else {
                        ListaDeReportes(reportes = uiState.reportes, onEliminar = { idPorConfirmar = it })
                    }
            }
        }
    }

    idPorConfirmar?.let { id ->
        ConfirmarEliminacionDialog(
            onConfirmar = {
                onEliminar(id)
                idPorConfirmar = null
            },
            onCancelar = { idPorConfirmar = null },
        )
    }
}

@Composable
private fun ListaDeReportes(
    reportes: List<Reporte>,
    onEliminar: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(reportes, key = { it.id }) { reporte ->
            TarjetaDeReporte(reporte = reporte, onEliminar = { onEliminar(reporte.id) })
        }
    }
}

@Composable
private fun TarjetaDeReporte(
    reporte: Reporte,
    onEliminar: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reporte.categoria.etiqueta(), style = MaterialTheme.typography.titleMedium)
                Text(text = formatearFechaHora(reporte.fechaHoraEpochMillis))
                Text(text = reporte.estado.etiqueta(), style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(onClick = onEliminar) {
                Text(stringResource(R.string.mis_reportes_boton_eliminar))
            }
        }
    }
}

@Composable
private fun ConfirmarEliminacionDialog(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.mis_reportes_dialogo_titulo)) },
        text = { Text(stringResource(R.string.mis_reportes_dialogo_texto)) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(stringResource(R.string.mis_reportes_dialogo_confirmar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.mis_reportes_dialogo_cancelar)) }
        },
    )
}
