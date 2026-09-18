package com.wanchaqlimpio.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*
 * Textos de presentación de los modelos de dominio. Viven en la capa de UI (y no en
 * el dominio) porque dependen de recursos de Android y del idioma del dispositivo;
 * el dominio se mantiene en Kotlin puro.
 */

@Composable
fun CategoriaResiduo.etiqueta(): String =
    when (this) {
        CategoriaResiduo.PLASTICO -> stringResource(R.string.categoria_plastico)
        CategoriaResiduo.CARTON -> stringResource(R.string.categoria_carton)
        CategoriaResiduo.VIDRIO -> stringResource(R.string.categoria_vidrio)
        CategoriaResiduo.ORGANICO -> stringResource(R.string.categoria_organico)
        CategoriaResiduo.ESCOMBROS -> stringResource(R.string.categoria_escombros)
        CategoriaResiduo.MIXTO -> stringResource(R.string.categoria_mixto)
    }

@Composable
fun EstadoReporte.etiqueta(): String =
    when (this) {
        EstadoReporte.PENDIENTE -> stringResource(R.string.estado_pendiente)
        EstadoReporte.SINCRONIZADO -> stringResource(R.string.estado_sincronizado)
        EstadoReporte.AGRUPADO -> stringResource(R.string.estado_agrupado)
        EstadoReporte.VERIFICADO -> stringResource(R.string.estado_verificado)
        EstadoReporte.DESCARTADO -> stringResource(R.string.estado_descartado)
    }

private val FORMATO_FECHA_HORA = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

fun formatearFechaHora(epochMillis: Long): String =
    FORMATO_FECHA_HORA.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
