package com.wanchaqlimpio.app.domain.usecase

import com.wanchaqlimpio.app.BuildConfig
import com.wanchaqlimpio.app.core.util.distanciaMetros
import com.wanchaqlimpio.app.domain.LimiteDistritalWanchaq
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.Ubicacion
import javax.inject.Inject

/** El error de posicionamiento máximo de un reporte (RF-02). Lo usan también la espera del GPS y su indicador. */
const val PRECISION_MAXIMA_METROS = 15f
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
        ): ResultadoValidacionReporte {
            
            // Bypass para pruebas de campo sin restricción de Wanchaq ni límite estricto de GPS
            val fueraDelDistrito = !LimiteDistritalWanchaq.contiene(ubicacion.latitud, ubicacion.longitud)
            val malaPrecision = ubicacion.precisionMetros > PRECISION_MAXIMA_METROS

            if (!BuildConfig.MODO_PRUEBA) {
                if (malaPrecision) return ResultadoValidacionReporte.PrecisionInsuficiente(ubicacion.precisionMetros)
                if (fueraDelDistrito) return ResultadoValidacionReporte.FueraDelLimiteDistrital
            }

            if (reportesPreviosDelMismoDispositivo.any { esDuplicadoDe(it, ubicacion, ahoraEpochMillis) }) {
                return ResultadoValidacionReporte.Duplicado
            }

            return ResultadoValidacionReporte.Valido
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
