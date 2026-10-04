package com.wanchaqlimpio.app.ui.mapa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.domain.model.PuntoCritico
import com.wanchaqlimpio.app.domain.repository.MotivoFalloMapa
import com.wanchaqlimpio.app.ui.common.ANCHO_MAXIMO_CONTENIDO

/**
 * Contenido "stateless" del mapa.
 */
@Composable
fun MapaContent(
    estado: MapaUiState,
    avisarFaltaLaClave: Boolean,
    onActualizar: () -> Unit,
    modifier: Modifier = Modifier,
    mapa: @Composable (List<PuntoCritico>) -> Unit,
) {
    var mostrarMensaje by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        // SIEMPRE dibujar el mapa en el fondo para que el selector de capas sea visible
        val puntosActuales = if (estado is MapaUiState.ConPuntos) estado.puntos else emptyList()
        Box(Modifier.fillMaxSize().testTag(MapaTags.MAPA)) { mapa(puntosActuales) }

        when (estado) {
            MapaUiState.Cargando ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).testTag(MapaTags.CARGANDO))

            is MapaUiState.Error -> {
                if (mostrarMensaje) {
                    Mensaje(
                        texto = stringResource(estado.motivo.textoRecurso()),
                        boton = stringResource(R.string.mapa_boton_reintentar),
                        etiquetaBoton = MapaTags.BOTON_REINTENTAR,
                        onBoton = onActualizar,
                        etiqueta = MapaTags.ERROR,
                        onClose = { mostrarMensaje = false }
                    )
                } else {
                    BotonMostrarMensaje(
                        modifier = Modifier.align(Alignment.TopCenter),
                        onClick = { mostrarMensaje = true }
                    )
                }
            }

            MapaUiState.SinPuntos -> {
                if (mostrarMensaje) {
                    Mensaje(
                        titulo = stringResource(R.string.mapa_sin_focos_titulo),
                        texto = stringResource(R.string.mapa_sin_focos_texto),
                        boton = stringResource(R.string.mapa_boton_actualizar),
                        etiquetaBoton = MapaTags.BOTON_ACTUALIZAR,
                        onBoton = onActualizar,
                        etiqueta = MapaTags.SIN_FOCOS,
                        alineacion = Alignment.TopCenter,
                        onClose = { mostrarMensaje = false }
                    )
                } else {
                    BotonMostrarMensaje(
                        modifier = Modifier.align(Alignment.TopCenter),
                        onClick = { mostrarMensaje = true }
                    )
                }
            }

            is MapaUiState.ConPuntos -> {
                ContadorDeFocos(cantidad = estado.puntos.size, modifier = Modifier.align(Alignment.TopStart))
            }
        }

        if (avisarFaltaLaClave) AvisoFaltaLaClave(modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun BotonMostrarMensaje(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Icon(Icons.Default.Info, contentDescription = "Mostrar estado")
    }
}

@Composable
private fun ContadorDeFocos(
    cantidad: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
                .testTag(MapaTags.CONTADOR),
    ) {
        Text(
            text = pluralStringResource(R.plurals.mapa_focos_confirmados, cantidad, cantidad),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun AvisoFaltaLaClave(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
                .widthIn(max = ANCHO_MAXIMO_CONTENIDO)
                .testTag(MapaTags.AVISO_CLAVE),
    ) {
        Text(
            text = stringResource(R.string.mapa_aviso_sin_clave),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun Mensaje(
    texto: String,
    boton: String,
    etiquetaBoton: String,
    onBoton: () -> Unit,
    etiqueta: String,
    alineacion: Alignment = Alignment.Center,
    titulo: String? = null,
    onClose: (() -> Unit)? = null,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = alineacion) {
        Card(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
                    .widthIn(max = ANCHO_MAXIMO_CONTENIDO)
                    .fillMaxWidth()
                    .testTag(etiqueta),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (onClose != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                }
                if (titulo != null) Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                Text(text = texto, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onBoton, modifier = Modifier.testTag(etiquetaBoton)) { Text(boton) }
            }
        }
    }
}

private fun MotivoFalloMapa.textoRecurso(): Int =
    when (this) {
        MotivoFalloMapa.SIN_CONEXION -> R.string.mapa_error_sin_conexion
        MotivoFalloMapa.SERVIDOR -> R.string.mapa_error_servidor
        MotivoFalloMapa.RESPUESTA_INVALIDA -> R.string.mapa_error_respuesta
    }
