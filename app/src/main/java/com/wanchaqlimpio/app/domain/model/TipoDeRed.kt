package com.wanchaqlimpio.app.domain.model

/**
 * La conexión que tenía el teléfono en un momento dado. Es un dato técnico (no incluye el nombre de la red ni ningún
 * identificador) que se guarda solo en el teléfono para las pruebas de confiabilidad: el indicador «reportes
 * guardados y sincronizados según condición de red» (Anexo 2) necesita saber en qué condición ocurrió cada envío.
 */
enum class TipoDeRed {
    /** No había ninguna red activa (modo avión, sin datos ni Wi-Fi). */
    SIN_RED,

    WIFI,

    DATOS_MOVILES,

    /** Conectado a una red, pero Android no la da por buena para Internet (p. ej. Wi-Fi sin salida o con portal). */
    SIN_INTERNET,

    /** Ethernet, VPN u otra. */
    OTRA,
}
