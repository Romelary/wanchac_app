package com.wanchaqlimpio.app.ui.reporte

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanchaqlimpio.app.core.location.ProveedorUbicacion
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.repository.AlmacenFotos
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReporteViewModel
    @Inject
    constructor(
        private val reporteRepository: ReporteRepository,
        private val consentimientoRepository: ConsentimientoRepository,
        private val proveedorUbicacion: ProveedorUbicacion,
        private val almacenFotos: AlmacenFotos,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ReporteUiState>(ReporteUiState.Capturando)
        val uiState: StateFlow<ReporteUiState> = _uiState.asStateFlow()

        // TODO(Módulo 3): reemplazar por la sugerencia automática del modelo de IA
        // on-device (MobileNetV2/TFLite), dejando esta selección como corrección manual.
        private var categoriaSeleccionada: CategoriaResiduo = CategoriaResiduo.MIXTO

        fun seleccionarCategoria(categoria: CategoriaResiduo) {
            categoriaSeleccionada = categoria
        }

        /** Archivo donde la pantalla debe escribir la próxima foto. */
        fun crearArchivoFoto(): File = almacenFotos.nuevaFoto()

        fun onFotoCapturada(fotoPath: String) {
            viewModelScope.launch {
                _uiState.value = ReporteUiState.ProcesandoUbicacion

                val estadoFinal = procesarFoto(fotoPath)

                // Si el reporte no se guardó, la foto no debe quedar en el almacenamiento:
                // puede contener rostros o placas (Ética, Cap. I) y nadie la volvería a usar.
                if (estadoFinal !is ReporteUiState.Guardado) almacenFotos.borrar(fotoPath)

                _uiState.value = estadoFinal
            }
        }

        fun reintentar() {
            _uiState.value = ReporteUiState.Capturando
        }

        private suspend fun procesarFoto(fotoPath: String): ReporteUiState {
            val idAnonimo = consentimientoRepository.estado.first().idAnonimo
            val ubicacion = if (idAnonimo != null) proveedorUbicacion.obtenerUbicacionActual() else null

            return when {
                idAnonimo == null -> ReporteUiState.Rechazado("Falta aceptar el consentimiento informado.")
                ubicacion == null ->
                    ReporteUiState.Rechazado(
                        "No se pudo obtener tu ubicación. Verifica que el GPS esté activado e intenta de nuevo.",
                    )
                else ->
                    reporteRepository
                        .crearReporte(
                            idAnonimo = idAnonimo,
                            categoria = categoriaSeleccionada,
                            ubicacion = ubicacion,
                            fotoPath = fotoPath,
                            ahoraEpochMillis = System.currentTimeMillis(),
                        ).aEstadoUi()
            }
        }

        private fun ResultadoValidacionReporte.aEstadoUi(): ReporteUiState =
            when (this) {
                ResultadoValidacionReporte.Valido -> ReporteUiState.Guardado
                is ResultadoValidacionReporte.PrecisionInsuficiente ->
                    ReporteUiState.Rechazado(
                        "La precisión del GPS (~${precisionMetros.toInt()} m) es insuficiente (máximo 15 m). " +
                            "Intenta en un lugar más despejado.",
                    )
                ResultadoValidacionReporte.FueraDelLimiteDistrital ->
                    ReporteUiState.Rechazado("Esta ubicación está fuera del distrito de Wanchaq.")
                ResultadoValidacionReporte.Duplicado ->
                    ReporteUiState.Rechazado("Ya reportaste este mismo lugar hace menos de 30 minutos.")
            }
    }
