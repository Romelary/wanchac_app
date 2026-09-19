package com.wanchaqlimpio.app.ui.reporte

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.ui.common.ContenidoCentrado

/**
 * Lo que se ve mientras faltan la cámara o la ubicación precisa. Si el ciudadano negó el permiso dos veces, Android
 * ya no vuelve a preguntar: solo se puede activar desde los Ajustes de la app, y sin [onAbrirAjustes] esta pantalla no
 * tendría salida.
 */
@Composable
internal fun PermisosRequeridosContent(
    onSolicitar: () -> Unit,
    onAbrirAjustes: () -> Unit,
) {
    Scaffold { innerPadding ->
        ContenidoCentrado(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.reporte_permisos_titulo))
                Text(stringResource(R.string.reporte_permisos_texto))
                Button(onClick = onSolicitar) {
                    Text(stringResource(R.string.reporte_permisos_boton))
                }
                Text(stringResource(R.string.reporte_permisos_ayuda))
                OutlinedButton(onClick = onAbrirAjustes) {
                    Text(stringResource(R.string.reporte_permisos_boton_ajustes))
                }
            }
        }
    }
}

/** Abre la pantalla de ajustes de esta app en Android, donde se pueden activar los permisos negados. */
internal fun abrirAjustesDeLaApp(context: Context) {
    val ajustes =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(ajustes)
}
