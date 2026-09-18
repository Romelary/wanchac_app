package com.wanchaqlimpio.app.domain.model

/**
 * Pipeline real del reporte (ver diseno.md §7 y CORR-008): captura ->
 * sincronización -> agrupamiento (DBSCAN, backend) -> verificación en campo.
 * Reemplaza el vocabulario ("pendiente", "atendido") del Cap. IV original,
 * que presuponía una acción municipal directa que el sistema no contempla.
 */
enum class EstadoReporte {
    PENDIENTE,
    SINCRONIZADO,
    AGRUPADO,
    VERIFICADO,
    DESCARTADO,
}
