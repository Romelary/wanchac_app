package com.wanchaqlimpio.app.core.location

import com.wanchaqlimpio.app.domain.model.Ubicacion
import java.util.concurrent.atomic.AtomicInteger

private const val LATITUD_INICIAL = -13.540
private const val LONGITUD = -71.9475
private const val SALTO_GRADOS = 0.0003 // ≈ 33 m hacia el norte
private const val PASOS_ANTES_DE_REPETIR = 80 // de -13.540 a ≈ -13.516: aún dentro del distrito (tope: -13.515)
private const val PRECISION_SIMULADA_METROS = 5f

/**
 * SOLO PARA PRUEBAS en un dispositivo (compilación de depuración con `-PubicacionSimulada=true`; ver
 * `docs/prueba-en-dispositivo.md`). Devuelve una ubicación DENTRO de Wanchaq con 5 m de precisión, para poder
 * ejercitar el resto del flujo de reporte (guardar, Mis reportes…) en un dispositivo que no está en el distrito o
 * no tiene buena señal GPS. **Nunca** debe estar activo en una versión de publicación: los datos que produce son
 * falsos.
 *
 * Cada llamada se corre ≈ 33 m hacia el norte: así dos reportes seguidos no se descartan como duplicados (regla
 * de RNF-07: menos de 20 m y 30 minutos del mismo dispositivo).
 */
class ProveedorUbicacionSimulado : ProveedorUbicacion {
    private val llamadas = AtomicInteger(0)

    override suspend fun obtenerUbicacionActual(): Ubicacion {
        val paso = llamadas.getAndIncrement() % PASOS_ANTES_DE_REPETIR
        return Ubicacion(LATITUD_INICIAL + paso * SALTO_GRADOS, LONGITUD, PRECISION_SIMULADA_METROS)
    }
}
