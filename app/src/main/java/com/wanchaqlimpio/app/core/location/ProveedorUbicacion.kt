package com.wanchaqlimpio.app.core.location

import com.wanchaqlimpio.app.domain.model.Ubicacion
import kotlinx.coroutines.flow.Flow

/**
 * Abstrae el origen de la ubicación para que ReporteViewModel se pueda
 * probar con una implementación falsa, sin depender de Google Play Services
 * ni de permisos reales (ver pruebas.md).
 */
interface ProveedorUbicacion {
    /**
     * Las lecturas del GPS, una tras otra, mientras alguien las recoja: al dejar de recogerlas se apaga el GPS. Una
     * sola lectura no basta para el filtro de 15 m (RF-02): la primera suele ser la más imprecisa y mejora en unos
     * segundos, así que quien la use debe esperar a una buena (ver [RastreadorDeUbicacion]).
     *
     * El flujo termina solo, sin error, si no se puede obtener ubicación (sin permiso, GPS apagado, servicio no
     * disponible): quien lo recoge lo ve como «no hubo lecturas».
     */
    fun lecturas(): Flow<Ubicacion>
}
