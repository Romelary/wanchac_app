package com.wanchaqlimpio.app.ui.ajustes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wanchaqlimpio.app.BuildConfig
import com.wanchaqlimpio.app.R
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val ETIQUETA_LOG = "RegistroDeEnvios"

/**
 * El botón para compartir el registro de envíos (ver `aCsvDeEnvios`): la tabla que el investigador saca del teléfono
 * en las pruebas de confiabilidad, sin cables ni herramientas.
 */
@Composable
internal fun SeccionRegistroDeEnvios(onCompartir: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ajustes_registro_titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.ajustes_registro_texto))
        OutlinedButton(onClick = onCompartir) {
            Text(stringResource(R.string.ajustes_registro_boton))
        }
    }
}

/** Las líneas de cabecera del registro: qué app, en qué teléfono y cuándo se generó (para no mezclar las tandas). */
internal fun encabezadoDelRegistro(): List<String> =
    listOf(
        "Wanchaq Limpio ${BuildConfig.VERSION_NAME}",
        "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "generado ${Instant.now().truncatedTo(ChronoUnit.SECONDS)}",
    )

/** Abre el selector de apps de Android para mandar [texto] (correo, chat, notas…). */
internal fun compartirRegistroDeEnvios(
    context: Context,
    texto: String,
) {
    val envio =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.ajustes_registro_asunto))
            putExtra(Intent.EXTRA_TEXT, texto)
        }
    try {
        context.startActivity(Intent.createChooser(envio, context.getString(R.string.ajustes_registro_elegir_app)))
    } catch (e: ActivityNotFoundException) {
        // Sin ninguna app que acepte texto (muy raro): no hay nada que hacer más que dejar constancia.
        Log.w(ETIQUETA_LOG, "No hay ninguna app para compartir el registro", e)
    }
}
