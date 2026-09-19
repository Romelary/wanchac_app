package com.wanchaqlimpio.app.ui.ajustes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanchaqlimpio.app.BuildConfig
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.ui.common.ContenidoCentrado
import kotlinx.coroutines.launch

private const val ETIQUETA_LOG = "AjustesScreen"

@Composable
fun AjustesScreen(viewModel: AjustesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()

    AjustesContent(
        uiState = uiState,
        versionName = BuildConfig.VERSION_NAME,
        correoContacto = BuildConfig.CONTACT_EMAIL,
        onEscribirCorreo = { abrirCorreo(context, BuildConfig.CONTACT_EMAIL) },
        onBorrarTodo = viewModel::borrarTodo,
        onCompartirRegistro = {
            alcance.launch {
                compartirRegistroDeEnvios(context, viewModel.generarRegistroDeEnvios(encabezadoDelRegistro()))
            }
        },
    )
}

private fun abrirCorreo(
    context: Context,
    correo: String,
) {
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$correo".toUri()))
    } catch (e: ActivityNotFoundException) {
        // Sin app de correo instalada: el usuario aún puede leer y copiar la dirección en pantalla.
        Log.w(ETIQUETA_LOG, "No hay una app de correo instalada", e)
    }
}

/**
 * Contenido "stateless" (mismo patrón que OnboardingContent, ver pruebas.md): se
 * puede probar con Compose UI Testing sin infraestructura de Hilt ni de Android real.
 */
@Composable
fun AjustesContent(
    uiState: AjustesUiState,
    versionName: String,
    correoContacto: String,
    onEscribirCorreo: () -> Unit,
    onBorrarTodo: () -> Unit,
    onCompartirRegistro: () -> Unit = {},
) {
    // rememberSaveable: el diálogo de confirmación sobrevive a una rotación de pantalla.
    var confirmando by rememberSaveable { mutableStateOf(false) }

    Scaffold { innerPadding ->
        ContenidoCentrado(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                SeccionAcercaDe(versionName)
                uiState.idAnonimo?.let { SeccionCodigo(it) }
                SeccionContacto(correoContacto, onEscribirCorreo)
                SeccionRegistroDeEnvios(onCompartirRegistro)
                SeccionDatos(borrando = uiState.borrando, onBorrar = { confirmando = true })
            }
        }
    }

    if (confirmando) {
        ConfirmarBorradoDialog(
            onConfirmar = {
                confirmando = false
                onBorrarTodo()
            },
            onCancelar = { confirmando = false },
        )
    }
}

@Composable
private fun SeccionAcercaDe(versionName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ajustes_acerca_titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.ajustes_acerca_texto))
        Text(stringResource(R.string.ajustes_version, versionName), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SeccionCodigo(idAnonimo: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ajustes_codigo_titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.ajustes_codigo_texto))
        // Seleccionable para poder copiarlo y pegarlo en el correo.
        SelectionContainer { Text(idAnonimo, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun SeccionContacto(
    correoContacto: String,
    onEscribirCorreo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ajustes_contacto_titulo), style = MaterialTheme.typography.titleMedium)
        if (correoContacto.isBlank()) {
            // Solo alcanzable en compilaciones de desarrollo: la compilación release falla
            // mientras contactEmail no esté definido (ver verificarCorreoContacto).
            Text(stringResource(R.string.ajustes_contacto_sin_configurar), color = MaterialTheme.colorScheme.error)
        } else {
            Text(stringResource(R.string.ajustes_contacto_texto))
            SelectionContainer { Text(correoContacto, style = MaterialTheme.typography.bodyMedium) }
            OutlinedButton(onClick = onEscribirCorreo) {
                Text(stringResource(R.string.ajustes_contacto_boton))
            }
        }
    }
}

@Composable
private fun SeccionDatos(
    borrando: Boolean,
    onBorrar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ajustes_datos_titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.ajustes_datos_texto))
        Button(
            onClick = onBorrar,
            enabled = !borrando,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(if (borrando) R.string.ajustes_borrando else R.string.ajustes_borrar_boton))
        }
    }
}

@Composable
private fun ConfirmarBorradoDialog(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.ajustes_borrar_dialogo_titulo)) },
        text = { Text(stringResource(R.string.ajustes_borrar_dialogo_texto)) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(stringResource(R.string.ajustes_borrar_dialogo_confirmar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.ajustes_borrar_dialogo_cancelar)) }
        },
    )
}
