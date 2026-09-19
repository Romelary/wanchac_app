package com.wanchaqlimpio.app.ui.reporte

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.domain.usecase.PRECISION_MAXIMA_METROS
import kotlin.math.roundToInt

const val ETIQUETA_INDICADOR_GPS = "indicador_gps"

/**
 * Le dice al ciudadano, antes de tomar la foto, si el GPS ya es lo bastante preciso para que el reporte se acepte
 * (15 m o menos, RF-02): buscando, listo o impreciso. Con la señal mala no hace falta rendirse ni repetir la foto:
 * basta esperar unos segundos a cielo abierto.
 */
@Composable
fun IndicadorGps(
    precisionMetros: Float?,
    modifier: Modifier = Modifier,
) {
    val listo = precisionMetros != null && precisionMetros <= PRECISION_MAXIMA_METROS
    val colores = MaterialTheme.colorScheme
    val fondo =
        when {
            precisionMetros == null -> colores.surfaceVariant
            listo -> colores.primaryContainer
            else -> colores.errorContainer
        }
    val texto =
        when {
            precisionMetros == null -> stringResource(R.string.gps_buscando)
            listo -> stringResource(R.string.gps_listo, precisionMetros.roundToInt())
            else -> stringResource(R.string.gps_impreciso, precisionMetros.roundToInt())
        }

    Surface(color = fondo, shape = MaterialTheme.shapes.medium, modifier = modifier.testTag(ETIQUETA_INDICADOR_GPS)) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
