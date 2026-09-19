package com.wanchaqlimpio.app.domain.model

/**
 * Por qué un intento de envío al servidor no terminó bien. Se guarda en cada reporte para saber qué pasó (al usuario
 * se le explica en "Mis reportes" y, en las pruebas de confiabilidad, permite separar los fallos de la red de los
 * del servidor o de la app). Solo los dos últimos son definitivos: el resto se reintenta.
 */
enum class MotivoFalloEnvio {
    /** Sin red, o no se pudo llegar al servidor (DNS, conexión rechazada, TLS…). Transitorio. */
    SIN_CONEXION,

    /** La red respondió demasiado despacio, o el servidor gratuito estaba despertando (≈ 1 min). Transitorio. */
    TIEMPO_AGOTADO,

    /** 5xx, 408 o 429: el servidor está ocupado o falló. Transitorio. */
    SERVIDOR_OCUPADO,

    /** Una respuesta que no dice nada sobre este reporte (404, 401, 403, redirección…): suele ser de configuración. */
    RESPUESTA_INESPERADA,

    /** Una excepción imprevista de la app. Se reintenta unas pocas veces y luego se da por definitivo. */
    ERROR_INTERNO,

    /** 400, 410, 413, 415 o 422: el servidor dijo que ESTE reporte no se puede aceptar. Definitivo. */
    RECHAZADO_POR_EL_SERVIDOR,

    /** La foto ya no existe, no se puede leer o está vacía: sin ella no hay evidencia. Definitivo. */
    FOTO_NO_DISPONIBLE,
}
