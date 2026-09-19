package com.wanchaqlimpio.app.ui.reporte

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanchaqlimpio.app.core.location.RastreadorDeUbicacion
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.repository.AlmacenFotos
import com.wanchaqlimpio.app.domain.repository.ClasificadorResiduo
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReductorDeFotos
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import com.wanchaqlimpio.app.domain.usecase.PRECISION_MAXIMA_METROS
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
import kotlin.time.Duration.Companion.seconds

/** Cuánto se espera a una lectura del GPS de 15 m o mejor antes de dejar que la validación diga con qué se quedó. */
private val ESPERA_MAXIMA_DEL_GPS = 30.seconds

@HiltViewModel
class ReporteViewModel
    @Inject
    constructor(
        private val reporteRepository: ReporteRepository,
        private val consentimientoRepository: ConsentimientoRepository,
        private val rastreador: RastreadorDeUbicacion,
        private val almacenFotos: AlmacenFotos,
        private val clasificador: ClasificadorResiduo,
        private val reductorDeFotos: ReductorDeFotos,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ReporteUiState>(ReporteUiState.Capturando)
        val uiState: StateFlow<ReporteUiState> = _uiState.asStateFlow()

        /** La precisión del GPS mientras la cámara está abierta (`null` hasta la primera lectura). */
        val precisionGps: StateFlow<Float?> = rastreador.precisionActual

        init {
            limpiarFotosHuerfanas(inicioEpochMillis = System.currentTimeMillis())
        }

        /** Archivo donde la pantalla debe escribir la próxima foto. */
        fun crearArchivoFoto(): File = almacenFotos.nuevaFoto()

        /**
         * La pantalla de captura la ejecuta mientras la cámara está visible: mantiene el GPS encendido para que, al
         * tomar la foto, ya haya una lectura buena, y para mostrar su precisión. Termina al salir de la cámara.
         */
        suspend fun vigilarGps() = rastreador.vigilar()

        /**
         * La foto ya está guardada en disco: se reduce (para subirla con una red mala), se analiza con el modelo de
         * IA y se pide al ciudadano que confirme o corrija la categoría. Todavía NO se guarda el reporte.
         */
        fun onFotoCapturada(fotoPath: String) {
            // Antes de cualquier espera: es la lectura del GPS de justo ahora, donde está el foco.
            val ubicacionAlCapturar = rastreador.ubicacionReciente()
            viewModelScope.launch {
                _uiState.value = ReporteUiState.Clasificando
                // Si no se puede reducir, la foto original se envía tal cual: reducir no debe impedir reportar.
                reductorDeFotos.reducir(fotoPath)
                // Si el modelo no está disponible (null) el reporte igual funciona: se elige a mano.
                val sugerencia = clasificador.clasificar(fotoPath)
                _uiState.value =
                    ReporteUiState.Confirmando(
                        fotoPath = fotoPath,
                        sugerencia = sugerencia,
                        seleccionada = sugerencia?.propuesta ?: CategoriaResiduo.MIXTO,
                        ubicacionAlCapturar = ubicacionAlCapturar,
                    )
            }
        }

        /** La cámara no pudo guardar la foto (sin espacio, fallo del hardware…): se lo dice al ciudadano. */
        fun onErrorDeCaptura() {
            _uiState.value =
                ReporteUiState.Rechazado(
                    "No se pudo tomar la foto. Inténtalo de nuevo; si sigue fallando, comprueba que el teléfono " +
                        "tenga espacio libre.",
                )
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
                _uiState.value = ReporteUiState.ProcesandoUbicacion()

                val idAnonimo = consentimientoRepository.estado.first().idAnonimo
                // Con la lectura de la foto si ya era buena; si no, se espera una buena y se ve cómo va la espera.
                val ubicacion =
                    if (idAnonimo == null) {
                        null
                    } else {
                        rastreador.ubicacionParaGuardar(
                            alCapturar = confirmacion.ubicacionAlCapturar,
                            precisionObjetivoMetros = PRECISION_MAXIMA_METROS,
                            esperaMaxima = ESPERA_MAXIMA_DEL_GPS,
                        ) { precision -> _uiState.value = ReporteUiState.ProcesandoUbicacion(precision) }
                    }
                val estadoFinal =
                    when {
                        idAnonimo == null -> ReporteUiState.Rechazado("Falta aceptar el consentimiento informado.")
                        ubicacion == null -> ReporteUiState.GpsImpreciso(confirmacion, precisionMetros = null)
                        else ->
                            reporteRepository
                                .crearReporte(
                                    idAnonimo = idAnonimo,
                                    categoria = confirmacion.seleccionada,
                                    ubicacion = ubicacion,
                                    fotoPath = confirmacion.fotoPath,
                                    ahoraEpochMillis = System.currentTimeMillis(),
                                    sugerenciaIa = confirmacion.sugerencia,
                                ).aEstadoUi(confirmacion)
                    }

                // Si el reporte se rechazó por algo que no se arregla esperando, la foto no debe quedar en el
                // almacenamiento: puede contener rostros o placas (Ética, Cap. I) y nadie la volvería a usar. Con un
                // GPS impreciso sí se conserva: unos segundos más a cielo abierto y se guarda sin repetir todo.
                if (estadoFinal is ReporteUiState.Rechazado) almacenFotos.borrar(confirmacion.fotoPath)

                _uiState.value = estadoFinal
            }
        }

        /** Vuelve a intentar la ubicación con la MISMA foto (tras un GPS impreciso o sin señal). */
        fun reintentarUbicacion() {
            val estado = _uiState.value as? ReporteUiState.GpsImpreciso ?: return
            // La lectura de la foto no llegó a la precisión exigida: se espera una nueva en vez de reutilizarla.
            _uiState.value = estado.confirmacion.copy(ubicacionAlCapturar = null)
            confirmar()
        }

        /** El ciudadano prefiere tomar otra foto: la actual se borra (misma razón de privacidad). */
        fun descartarFoto() {
            val fotoPath =
                when (val estado = _uiState.value) {
                    is ReporteUiState.Confirmando -> estado.fotoPath
                    is ReporteUiState.GpsImpreciso -> estado.confirmacion.fotoPath
                    else -> return
                }

            viewModelScope.launch {
                almacenFotos.borrar(fotoPath)
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
    }

private fun ResultadoValidacionReporte.aEstadoUi(confirmacion: ReporteUiState.Confirmando): ReporteUiState =
    when (this) {
        ResultadoValidacionReporte.Valido -> ReporteUiState.Guardado
        is ResultadoValidacionReporte.PrecisionInsuficiente ->
            ReporteUiState.GpsImpreciso(
                confirmacion,
                precisionMetros,
            )
        ResultadoValidacionReporte.FueraDelLimiteDistrital ->
            ReporteUiState.Rechazado("Esta ubicación está fuera del distrito de Wanchaq.")
        ResultadoValidacionReporte.Duplicado ->
            ReporteUiState.Rechazado("Ya reportaste este mismo lugar hace menos de 30 minutos.")
    }
