package com.wanchaqlimpio.app.ui.mapa

import com.wanchaqlimpio.app.domain.model.PuntoCritico
import com.wanchaqlimpio.app.domain.repository.MotivoFalloMapa

sealed interface MapaUiState {
    data object Cargando : MapaUiState

    /**
     * Ningún foco confirmado todavía: es el estado normal durante las primeras semanas, porque hay que reunir
     * reportes y verificarlos en campo.
     */
    data object SinPuntos : MapaUiState

    data class ConPuntos(
        val puntos: List<PuntoCritico>,
    ) : MapaUiState

    data class Error(
        val motivo: MotivoFalloMapa,
    ) : MapaUiState
}
