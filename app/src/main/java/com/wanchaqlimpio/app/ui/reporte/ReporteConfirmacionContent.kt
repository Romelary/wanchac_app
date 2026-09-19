package com.wanchaqlimpio.app.ui.reporte

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.core.util.decodificarFoto
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.ui.common.ContenidoCentrado
import com.wanchaqlimpio.app.ui.common.etiqueta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LADO_MINIMO_VISTA_PREVIA = 512
private val ALTO_MAXIMO_FOTO = 280.dp

/**
 * Paso posterior a la captura: muestra la foto y la categoría que sugiere el modelo de IA, y deja al
 * ciudadano confirmarla o cambiarla antes de guardar (RF-04: "con opción de corrección manual").
 * "Stateless": recibe todo por parámetros, así se prueba sin Hilt (ver `pruebas.md`).
 *
 * No se muestra el porcentaje de confianza: sería una falsa precisión, y la sugerencia ya se nota si dudó
 * (en ese caso propone "Mixto" y lo dice).
 */
@Composable
fun ReporteConfirmacionContent(
    estado: ReporteUiState.Confirmando,
    onSeleccionar: (CategoriaResiduo) -> Unit,
    onGuardar: () -> Unit,
    onDescartar: () -> Unit,
) {
    val foto by rememberFotoParaMostrar(estado.fotoPath)

    Scaffold { innerPadding ->
        ContenidoCentrado(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                foto?.let {
                    Image(
                        bitmap = it,
                        contentDescription = stringResource(R.string.reporte_foto_descripcion),
                        modifier = Modifier.fillMaxWidth().heightIn(max = ALTO_MAXIMO_FOTO),
                        contentScale = ContentScale.Fit,
                    )
                }

                Text(stringResource(R.string.reporte_confirmar_titulo), style = MaterialTheme.typography.titleMedium)
                Text(textoDeSugerencia(estado))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoriaResiduo.entries.forEach { categoria ->
                        FilterChip(
                            selected = categoria == estado.seleccionada,
                            onClick = { onSeleccionar(categoria) },
                            label = { Text(categoria.etiqueta()) },
                        )
                    }
                }

                Button(onClick = onGuardar, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reporte_boton_guardar))
                }
                OutlinedButton(onClick = onDescartar, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reporte_boton_otra_foto))
                }
            }
        }
    }
}

@Composable
private fun textoDeSugerencia(estado: ReporteUiState.Confirmando): String {
    val sugerencia = estado.sugerencia
    return if (sugerencia != null && sugerencia.propuesta != CategoriaResiduo.MIXTO) {
        stringResource(R.string.reporte_sugerencia_ia, sugerencia.propuesta.etiqueta())
    } else {
        stringResource(R.string.reporte_sin_sugerencia)
    }
}

/** Decodifica la foto fuera del hilo principal; mientras tanto (o si no se puede leer) no muestra nada. */
@Composable
private fun rememberFotoParaMostrar(fotoPath: String) =
    produceState<ImageBitmap?>(initialValue = null, fotoPath) {
        value =
            withContext(Dispatchers.IO) {
                decodificarFoto(fotoPath, LADO_MINIMO_VISTA_PREVIA)?.asImageBitmap()
            }
    }
