package com.wanchaqlimpio.app.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import com.wanchaqlimpio.app.domain.usecase.BorrarTodosMisDatos
import com.wanchaqlimpio.app.domain.usecase.aCsvDeEnvios
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class AjustesViewModel
    @Inject
    constructor(
        private val consentimientoRepository: ConsentimientoRepository,
        private val reporteRepository: ReporteRepository,
        private val borrarTodosMisDatos: BorrarTodosMisDatos,
    ) : ViewModel() {
        private val borrando = MutableStateFlow(false)

        val uiState: StateFlow<AjustesUiState> =
            combine(consentimientoRepository.estado, borrando) { consentimiento, estaBorrando ->
                AjustesUiState(idAnonimo = consentimiento.idAnonimo, borrando = estaBorrando)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AjustesUiState(),
            )

        /**
         * Al terminar, el consentimiento queda revocado y `MainActivity` vuelve solo a la
         * pantalla de onboarding (observa el mismo estado), por lo que no hace falta
         * navegar desde aquí.
         */
        fun borrarTodo() {
            if (borrando.value) return
            borrando.value = true
            viewModelScope.launch {
                try {
                    borrarTodosMisDatos()
                } finally {
                    borrando.value = false
                }
            }
        }

        /**
         * El registro de envíos de los reportes de este teléfono, en CSV (ver [aCsvDeEnvios]): lo que se comparte en
         * las pruebas de confiabilidad. [comentarios] son las líneas de cabecera (versión de la app, modelo…). Sin
         * consentimiento (no hay código anónimo) no hay reportes y sale solo el encabezado.
         */
        suspend fun generarRegistroDeEnvios(comentarios: List<String>): String {
            val idAnonimo = consentimientoRepository.estado.first().idAnonimo
            val reportes =
                if (idAnonimo == null) emptyList() else reporteRepository.observarReportesDe(idAnonimo).first()
            return reportes.aCsvDeEnvios(comentarios)
        }
    }
