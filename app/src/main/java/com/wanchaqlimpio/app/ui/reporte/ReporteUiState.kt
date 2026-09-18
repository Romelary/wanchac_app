package com.wanchaqlimpio.app.ui.reporte

/** Estados de la pantalla de reporte, posteriores a la captura de la foto. */
sealed interface ReporteUiState {
    data object Capturando : ReporteUiState

    data object ProcesandoUbicacion : ReporteUiState

    data class Rechazado(
        val mensaje: String,
    ) : ReporteUiState

    data object Guardado : ReporteUiState
}
