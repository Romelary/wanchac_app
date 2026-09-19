package com.wanchaqlimpio.app.core.location

import com.wanchaqlimpio.app.domain.model.Ubicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicInteger

private const val LATITUD_INICIAL = -13.540
private const val LONGITUD_INICIAL = -71.9535 // el borde oeste de las columnas
private const val SALTO_GRADOS = 0.0003 // ≈ 33 m (hacia el norte por paso, hacia el este por columna)
private const val PASOS_ANTES_DE_REPETIR = 80 // de -13.540 a ≈ -13.516: aún dentro del distrito (tope: -13.515)
private const val COLUMNAS = 40 // 40 × 0,0003° ≈ 1,3 km de ancho: de -71.9535 a ≈ -71.9418, dentro del distrito
private const val MILISEGUNDOS_POR_MINUTO = 60_000L
private const val PRECISION_SIMULADA_METROS = 5f

/**
 * SOLO PARA PRUEBAS en un dispositivo (compilación de depuración con `-PubicacionSimulada=true`; ver
 * `docs/prueba-en-dispositivo.md`). Devuelve una ubicación DENTRO de Wanchaq con 5 m de precisión, para poder
 * ejercitar el resto del flujo de reporte (guardar, Mis reportes…) en un dispositivo que no está en el distrito o
 * no tiene buena señal GPS. **Nunca** debe estar activo en una versión de publicación: los datos que produce son
 * falsos.
 *
 * Cada lectura se corre ≈ 33 m hacia el norte: así dos reportes seguidos no se descartan como duplicados (regla
 * de RNF-07: menos de 20 m y 30 minutos del mismo dispositivo). Da UNA lectura por recogida y termina.
 *
 * La cuenta de pasos empieza de cero cada vez que arranca la app, y Android la arranca de nuevo por su cuenta (p. ej.
 * para enviar reportes pendientes): sin más, los reportes hechos tras ese reinicio repetirían los puntos de los
 * anteriores y se descartarían como duplicados (se vio en la tablet). Por eso cada arranque usa además una COLUMNA
 * distinta, según el minuto del reloj (≈ 33 m al este por minuto): dos arranques con más de un minuto de diferencia y
 * menos de 40 no comparten ningún punto.
 *
 * [ahoraEpochMillis] es el reloj (se cambia en las pruebas).
 */
class ProveedorUbicacionSimulado(
    ahoraEpochMillis: () -> Long = System::currentTimeMillis,
) : ProveedorUbicacion {
    private val llamadas = AtomicInteger(0)
    private val columna = ((ahoraEpochMillis() / MILISEGUNDOS_POR_MINUTO) % COLUMNAS).toInt()

    override fun lecturas(): Flow<Ubicacion> = flow { emit(siguiente()) }

    private fun siguiente(): Ubicacion {
        val paso = llamadas.getAndIncrement() % PASOS_ANTES_DE_REPETIR
        return Ubicacion(
            LATITUD_INICIAL + paso * SALTO_GRADOS,
            LONGITUD_INICIAL + columna * SALTO_GRADOS,
            PRECISION_SIMULADA_METROS,
        )
    }
}
