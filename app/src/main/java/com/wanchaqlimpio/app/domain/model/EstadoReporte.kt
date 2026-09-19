package com.wanchaqlimpio.app.domain.model

/**
 * Pipeline real del reporte (ver diseno.md §7 y CORR-008): captura ->
 * sincronización -> agrupamiento (DBSCAN, backend) -> verificación en campo.
 * Reemplaza el vocabulario ("pendiente", "atendido") del Cap. IV original,
 * que presuponía una acción municipal directa que el sistema no contempla.
 *
 * [RECHAZADO] es un estado SOLO LOCAL (el servidor no lo conoce): el envío terminó en un rechazo definitivo
 * (ver [MotivoFalloEnvio]) y no se reintentará. Sin él, un reporte que el servidor nunca aceptará se reenviaría para
 * siempre, gastando batería y datos, y contaría como "pendiente" en las pruebas de confiabilidad.
 */
enum class EstadoReporte {
    PENDIENTE,
    SINCRONIZADO,
    AGRUPADO,
    VERIFICADO,
    DESCARTADO,
    RECHAZADO,
}
