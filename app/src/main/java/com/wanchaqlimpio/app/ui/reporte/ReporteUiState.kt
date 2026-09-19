package com.wanchaqlimpio.app.ui.reporte

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.domain.model.Ubicacion

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
     * [ubicacionAlCapturar] es la lectura del GPS que había justo al tomar la foto (`null` si aún no había una
     * reciente): es donde está el foco, aunque el ciudadano tarde en confirmar o se mueva mientras tanto.
     */
    data class Confirmando(
        val fotoPath: String,
        val sugerencia: SugerenciaIa?,
        val seleccionada: CategoriaResiduo,
        val ubicacionAlCapturar: Ubicacion? = null,
    ) : ReporteUiState

    /** Se está consiguiendo la ubicación. [precisionActualMetros]: la mejor lectura hasta ahora (`null` si ninguna). */
    data class ProcesandoUbicacion(
        val precisionActualMetros: Float? = null,
    ) : ReporteUiState

    /**
     * El GPS no llegó a la precisión que exige el estudio (RF-02) o no dio ninguna lectura. A diferencia de los demás
     * rechazos, aquí **la foto se conserva**: con unos segundos más a cielo abierto el GPS mejora y no hay que
     * repetir todo. [precisionMetros] es la mejor que se consiguió (`null` si no hubo ninguna lectura).
     */
    data class GpsImpreciso(
        val confirmacion: Confirmando,
        val precisionMetros: Float?,
    ) : ReporteUiState

    data class Rechazado(
        val mensaje: String,
    ) : ReporteUiState

    data object Guardado : ReporteUiState
}
