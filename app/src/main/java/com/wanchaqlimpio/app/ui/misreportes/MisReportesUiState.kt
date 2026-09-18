package com.wanchaqlimpio.app.ui.misreportes

import com.wanchaqlimpio.app.domain.model.Reporte

sealed interface MisReportesUiState {
    data object Cargando : MisReportesUiState

    data class Lista(
        val reportes: List<Reporte>,
    ) : MisReportesUiState
}
