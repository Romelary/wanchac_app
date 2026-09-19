package com.wanchaqlimpio.app.ui.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanchaqlimpio.app.domain.model.PuntoCritico
import com.wanchaqlimpio.app.domain.repository.PuntosCriticosRepository
import com.wanchaqlimpio.app.domain.repository.ResultadoPuntosCriticos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado del mapa público de focos confirmados (Módulo 5, ADR-0005).
 *
 * No carga nada por sí solo: la pantalla llama a [recargar] cada vez que se abre la pestaña (un foco pudo confirmarse
 * mientras tanto) y el usuario puede pedirlo con "Actualizar" o "Reintentar". Mientras recarga con datos ya en
 * pantalla, esos datos se conservan (no parpadea a "cargando").
 */
@HiltViewModel
class MapaViewModel
    @Inject
    constructor(
        private val repositorio: PuntosCriticosRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<MapaUiState>(MapaUiState.Cargando)
        val uiState: StateFlow<MapaUiState> = _uiState.asStateFlow()

        private var carga: Job? = null

        fun recargar() {
            carga?.cancel() // una recarga nueva anula la anterior: no se pisan dos respuestas
            if (_uiState.value is MapaUiState.Error) _uiState.value = MapaUiState.Cargando
            carga = viewModelScope.launch { _uiState.value = repositorio.obtenerConfirmados().aEstado() }
        }
    }

private fun ResultadoPuntosCriticos.aEstado(): MapaUiState =
    when (this) {
        is ResultadoPuntosCriticos.Exito -> puntos.aEstadoDeFocos()
        is ResultadoPuntosCriticos.Fallo -> MapaUiState.Error(motivo)
    }

/** Sin focos no es un error: es el estado normal mientras no haya focos confirmados (ver [MapaUiState.SinPuntos]). */
private fun List<PuntoCritico>.aEstadoDeFocos(): MapaUiState =
    if (isEmpty()) MapaUiState.SinPuntos else MapaUiState.ConPuntos(this)
