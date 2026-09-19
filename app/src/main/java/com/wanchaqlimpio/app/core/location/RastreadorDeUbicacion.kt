package com.wanchaqlimpio.app.core.location

import com.wanchaqlimpio.app.domain.model.Ubicacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Una lectura del GPS más vieja que esto ya no dice dónde se hizo la foto: se vuelve a esperar una buena. */
private val EDAD_MAXIMA_DE_UNA_LECTURA = 10.seconds

/**
 * Consigue una ubicación que cumpla el filtro de 15 m (RF-02) SIN perder el reporte por una mala primera lectura.
 *
 * Antes se pedía UNA lectura al pulsar «Guardar»: la primera del GPS suele ser la más imprecisa y, si pasaba de 15 m,
 * se rechazaba el reporte y se borraba la foto: había que repetir todo, y en una calle estrecha eso pasa muy a
 * menudo. Ahora se hacen dos cosas:
 *  1. Mientras la cámara está abierta se mantiene el GPS encendido ([vigilar]) y se muestra su precisión, para que
 *     el ciudadano vea cuándo está listo y para que al tomar la foto ya haya una lectura buena ([ubicacionReciente]).
 *  2. Si al guardar no la hay, se espera hasta [esperarPrecisa] a una lectura buena, sin rendirse a la primera.
 *
 * El filtro de 15 m NO se relaja: si nunca llega una lectura buena, el reporte se sigue rechazando (RF-02), pero sin
 * borrar la foto (ver `ReporteViewModel`).
 */
class RastreadorDeUbicacion
    @Inject
    constructor(
        private val proveedor: ProveedorUbicacion,
        private val fuenteDeTiempo: TimeSource,
    ) {
        private val _precisionActual = MutableStateFlow<Float?>(null)

        /** La precisión de la última lectura mientras se vigila el GPS (`null` si aún no hay ninguna). */
        val precisionActual: StateFlow<Float?> = _precisionActual.asStateFlow()

        private var ultima: Pair<Ubicacion, TimeMark>? = null

        /**
         * Mantiene el GPS encendido y anota cada lectura hasta que se cancele (al salir de la cámara). Se queda con
         * la última lectura para [ubicacionReciente].
         */
        suspend fun vigilar() {
            _precisionActual.value = null
            ultima = null
            proveedor.lecturas().collect { lectura ->
                ultima = lectura to fuenteDeTiempo.markNow()
                _precisionActual.value = lectura.precisionMetros
            }
        }

        /** La lectura de la cámara, si es de hace pocos segundos; `null` si no la hay o ya está vieja. */
        fun ubicacionReciente(): Ubicacion? =
            ultima?.takeIf { (_, marca) -> marca.elapsedNow() <= EDAD_MAXIMA_DE_UNA_LECTURA }?.first

        /**
         * La ubicación con la que guardar un reporte: la que había al tomar la foto ([alCapturar]) si ya era buena; si
         * no, se espera una buena (informando del progreso) y se usa la mejor de las dos, para que el mensaje diga con
         * qué precisión se quedó realmente. `null` si no hubo ninguna lectura.
         */
        suspend fun ubicacionParaGuardar(
            alCapturar: Ubicacion?,
            precisionObjetivoMetros: Float,
            esperaMaxima: Duration,
            alProgresar: (mejorPrecisionMetros: Float) -> Unit = {},
        ): Ubicacion? {
            if (alCapturar != null && alCapturar.precisionMetros <= precisionObjetivoMetros) return alCapturar
            val esperada = esperarPrecisa(precisionObjetivoMetros, esperaMaxima, alProgresar)
            return listOfNotNull(alCapturar, esperada).minByOrNull { it.precisionMetros }
        }

        /**
         * Espera a una lectura de [precisionObjetivoMetros] o mejor, hasta [esperaMaxima]. Devuelve la **mejor** que
         * llegó (aunque no alcance el objetivo, para que quien valida pueda decir con qué precisión se quedó) o
         * `null` si no llegó ninguna. [alProgresar] recibe la mejor precisión conseguida hasta el momento.
         */
        suspend fun esperarPrecisa(
            precisionObjetivoMetros: Float,
            esperaMaxima: Duration,
            alProgresar: (mejorPrecisionMetros: Float) -> Unit = {},
        ): Ubicacion? {
            var mejor: Ubicacion? = null
            withTimeoutOrNull(esperaMaxima) {
                proveedor.lecturas().firstOrNull { lectura ->
                    // Se queda con la más precisa de las que van llegando (la primera del GPS suele ser la peor).
                    val mejorHastaAhora = mejor?.takeIf { it.precisionMetros <= lectura.precisionMetros } ?: lectura
                    mejor = mejorHastaAhora
                    alProgresar(mejorHastaAhora.precisionMetros)
                    lectura.precisionMetros <= precisionObjetivoMetros
                }
            }
            return mejor
        }
    }
