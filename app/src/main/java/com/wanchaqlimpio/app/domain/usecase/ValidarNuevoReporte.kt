package com.wanchaqlimpio.app.domain.usecase

import com.wanchaqlimpio.app.core.util.distanciaMetros
import com.wanchaqlimpio.app.domain.LimiteDistritalWanchaq
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.Ubicacion
import javax.inject.Inject

private const val PRECISION_MAXIMA_METROS = 15.0
private const val RADIO_DUPLICADO_METROS = 20.0
private const val VENTANA_DUPLICADO_MINUTOS = 30.0

sealed interface ResultadoValidacionReporte {
    data object Valido : ResultadoValidacionReporte

    data class PrecisionInsuficiente(
        val precisionMetros: Float,
    ) : ResultadoValidacionReporte

    data object FueraDelLimiteDistrital : ResultadoValidacionReporte

    data object Duplicado : ResultadoValidacionReporte
}

/**
 * Reglas de aceptación de un reporte nuevo, previas a guardarlo (RF-02, RNF-07
 * en diseno.md). Es una clase de dominio pura (sin Android ni Room) para que
 * se pueda probar sin infraestructura — ver pruebas.md.
 */
class ValidarNuevoReporte
    @Inject
    constructor() {
        operator fun invoke(
            ubicacion: Ubicacion,
            reportesPreviosDelMismoDispositivo: List<Reporte>,
            ahoraEpochMillis: Long,
        ): ResultadoValidacionReporte =
            when {
                ubicacion.precisionMetros > PRECISION_MAXIMA_METROS ->
                    ResultadoValidacionReporte.PrecisionInsuficiente(ubicacion.precisionMetros)

                !LimiteDistritalWanchaq.contiene(ubicacion.latitud, ubicacion.longitud) ->
                    ResultadoValidacionReporte.FueraDelLimiteDistrital

                reportesPreviosDelMismoDispositivo.any { esDuplicadoDe(it, ubicacion, ahoraEpochMillis) } ->
                    ResultadoValidacionReporte.Duplicado

                else -> ResultadoValidacionReporte.Valido
            }

        private fun esDuplicadoDe(
            previo: Reporte,
            ubicacion: Ubicacion,
            ahoraEpochMillis: Long,
        ): Boolean {
            val minutosTranscurridos = (ahoraEpochMillis - previo.fechaHoraEpochMillis) / 60_000.0
            if (minutosTranscurridos >= VENTANA_DUPLICADO_MINUTOS) return false
            val distancia = distanciaMetros(ubicacion.latitud, ubicacion.longitud, previo.latitud, previo.longitud)
            return distancia < RADIO_DUPLICADO_METROS
        }
    }
