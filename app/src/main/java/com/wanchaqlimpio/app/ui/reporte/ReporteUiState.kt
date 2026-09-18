package com.wanchaqlimpio.app.ui.reporte

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.SugerenciaIa

/**
 * Estados de la pantalla de reporte. Flujo feliz:
 * `Capturando` → `Clasificando` → `Confirmando` → `ProcesandoUbicacion` → `Guardado`.
 */
sealed interface ReporteUiState {
    data object Capturando : ReporteUiState

    /** La foto ya se tomó y el modelo de IA la está analizando (unos cientos de milisegundos). */
    data object Clasificando : ReporteUiState

    /**
     * Foto tomada y analizada: el ciudadano confirma o corrige el tipo de residuo ANTES de guardar.
     * [sugerencia] es `null` si el modelo no estuvo disponible; entonces se elige a mano.
     * [seleccionada] arranca en lo que propone el modelo (o `MIXTO` si dudó o no hay sugerencia).
     */
    data class Confirmando(
        val fotoPath: String,
        val sugerencia: SugerenciaIa?,
        val seleccionada: CategoriaResiduo,
    ) : ReporteUiState

    data object ProcesandoUbicacion : ReporteUiState

    data class Rechazado(
        val mensaje: String,
    ) : ReporteUiState

    data object Guardado : ReporteUiState
}
