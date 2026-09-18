package com.wanchaqlimpio.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanchaqlimpio.app.domain.model.ConsentimientoEstado
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val repository: ConsentimientoRepository,
    ) : ViewModel() {
        val estado: StateFlow<ConsentimientoEstado?> =
            repository.estado.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null,
            )

        fun aceptar() {
            viewModelScope.launch {
                repository.aceptarConsentimiento()
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
