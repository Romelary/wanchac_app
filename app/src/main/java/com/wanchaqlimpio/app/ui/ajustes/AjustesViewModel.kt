package com.wanchaqlimpio.app.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.usecase.BorrarTodosMisDatos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class AjustesViewModel
    @Inject
    constructor(
        consentimientoRepository: ConsentimientoRepository,
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
    }
