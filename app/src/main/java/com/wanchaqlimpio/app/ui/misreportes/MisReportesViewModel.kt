package com.wanchaqlimpio.app.ui.misreportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Lista los reportes hechos desde este dispositivo (identificado por `id_anonimo`,
 * no por una cuenta — ADR-0001) y permite eliminarlos (RNF-05, ADR-0002).
 */
@HiltViewModel
class MisReportesViewModel
    @Inject
    constructor(
        private val reporteRepository: ReporteRepository,
        consentimientoRepository: ConsentimientoRepository,
    ) : ViewModel() {
        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<MisReportesUiState> =
            consentimientoRepository.estado
                .map { it.idAnonimo }
                .distinctUntilChanged()
                .flatMapLatest { idAnonimo ->
                    // Sin consentimiento aceptado no hay id_anonimo, y por tanto no hay reportes.
                    if (idAnonimo == null) {
                        flowOf(emptyList<Reporte>())
                    } else {
                        reporteRepository.observarReportesDe(idAnonimo)
                    }
                }.map<List<Reporte>, MisReportesUiState> { MisReportesUiState.Lista(it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = MisReportesUiState.Cargando,
                )

        fun eliminar(id: Long) {
            viewModelScope.launch {
                reporteRepository.eliminarReporte(id)
            }
        }

        /** «Enviar ahora»: vuelve a intentar enseguida el envío de los reportes pendientes. */
        fun enviarAhora() = reporteRepository.enviarPendientesAhora()

        /** «Reintentar» en un reporte que el servidor rechazó: vuelve a ponerlo en la cola de envío. */
        fun reintentar(id: Long) {
            viewModelScope.launch {
                reporteRepository.reintentarEnvio(id)
            }
        }
    }
