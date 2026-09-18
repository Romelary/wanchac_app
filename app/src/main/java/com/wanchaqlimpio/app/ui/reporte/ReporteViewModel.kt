package com.wanchaqlimpio.app.ui.reporte

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanchaqlimpio.app.core.location.ProveedorUbicacion
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.repository.AlmacenFotos
import com.wanchaqlimpio.app.domain.repository.ClasificadorResiduo
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
        private val clasificador: ClasificadorResiduo,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ReporteUiState>(ReporteUiState.Capturando)
        val uiState: StateFlow<ReporteUiState> = _uiState.asStateFlow()

        init {
            limpiarFotosHuerfanas(inicioEpochMillis = System.currentTimeMillis())
        }

        /** Archivo donde la pantalla debe escribir la próxima foto. */
        fun crearArchivoFoto(): File = almacenFotos.nuevaFoto()

        /**
         * La foto ya está guardada en disco: se analiza con el modelo de IA y se pide al ciudadano que
         * confirme o corrija la categoría. Todavía NO se guarda el reporte.
         */
        fun onFotoCapturada(fotoPath: String) {
            viewModelScope.launch {
                _uiState.value = ReporteUiState.Clasificando
                // Si el modelo no está disponible (null) el reporte igual funciona: se elige a mano.
                val sugerencia = clasificador.clasificar(fotoPath)
                _uiState.value =
                    ReporteUiState.Confirmando(
                        fotoPath = fotoPath,
                        sugerencia = sugerencia,
                        seleccionada = sugerencia?.propuesta ?: CategoriaResiduo.MIXTO,
                    )
            }
        }

        fun seleccionarCategoria(categoria: CategoriaResiduo) {
            _uiState.update { estado ->
                if (estado is ReporteUiState.Confirmando) estado.copy(seleccionada = categoria) else estado
            }
        }

        /** El ciudadano confirmó la categoría: se obtiene la ubicación, se valida y se guarda. */
        fun confirmar() {
            val confirmacion = _uiState.value as? ReporteUiState.Confirmando ?: return

            viewModelScope.launch {
                _uiState.value = ReporteUiState.ProcesandoUbicacion

                val estadoFinal = procesarFoto(confirmacion)

                // Si el reporte no se guardó, la foto no debe quedar en el almacenamiento:
                // puede contener rostros o placas (Ética, Cap. I) y nadie la volvería a usar.
                if (estadoFinal !is ReporteUiState.Guardado) almacenFotos.borrar(confirmacion.fotoPath)

                _uiState.value = estadoFinal
            }
        }

        /** El ciudadano prefiere tomar otra foto: la actual se borra (misma razón de privacidad). */
        fun descartarFoto() {
            val confirmacion = _uiState.value as? ReporteUiState.Confirmando ?: return

            viewModelScope.launch {
                almacenFotos.borrar(confirmacion.fotoPath)
                _uiState.value = ReporteUiState.Capturando
            }
        }

        fun reintentar() {
            _uiState.value = ReporteUiState.Capturando
        }

        /**
         * Si la app se cerró con una foto sin confirmar, esa foto quedó en el disco sin ningún reporte que la
         * reclame. Al empezar se borran las fotos que ningún reporte usa (y que son anteriores a este momento,
         * para no tocar una foto tomada mientras esto corre).
         */
        private fun limpiarFotosHuerfanas(inicioEpochMillis: Long) {
            viewModelScope.launch {
                val idAnonimo = consentimientoRepository.estado.first().idAnonimo ?: return@launch
                val enUso =
                    reporteRepository
                        .observarReportesDe(idAnonimo)
                        .first()
                        .map { it.fotoPath }
                        .toSet()
                almacenFotos.borrarHuerfanas(conservar = enUso, modificadasAntesDeEpochMillis = inicioEpochMillis)
            }
        }

        private suspend fun procesarFoto(confirmacion: ReporteUiState.Confirmando): ReporteUiState {
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
                            categoria = confirmacion.seleccionada,
                            ubicacion = ubicacion,
                            fotoPath = confirmacion.fotoPath,
                            ahoraEpochMillis = System.currentTimeMillis(),
                            sugerenciaIa = confirmacion.sugerencia,
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
