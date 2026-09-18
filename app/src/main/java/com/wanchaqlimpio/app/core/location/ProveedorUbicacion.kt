package com.wanchaqlimpio.app.core.location

import com.wanchaqlimpio.app.domain.model.Ubicacion

/**
 * Abstrae el origen de la ubicación para que ReporteViewModel se pueda
 * probar con una implementación falsa, sin depender de Google Play Services
 * ni de permisos reales (ver pruebas.md).
 */
interface ProveedorUbicacion {
    /** `null` si no se pudo obtener una lectura (GPS apagado, sin señal, etc.). */
    suspend fun obtenerUbicacionActual(): Ubicacion?
}
