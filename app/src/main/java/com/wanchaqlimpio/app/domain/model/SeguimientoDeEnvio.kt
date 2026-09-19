package com.wanchaqlimpio.app.domain.model

/** El último fallo de un envío: la causa y, si el servidor llegó a contestar, su código HTTP. */
data class FalloDeEnvio(
    val motivo: MotivoFalloEnvio,
    val codigoHttp: Int? = null,
)

/**
 * Lo que se sabe del envío de un reporte al servidor (Módulo 4). Existe para dos cosas: explicarle al ciudadano por
 * qué su reporte sigue pendiente y poder medir la confiabilidad del envío (cuántos intentos hicieron falta, cuánto
 * tardó, en qué red y por qué falló) sin depender de la memoria de quien hace la prueba.
 *
 * [intentos] cuenta cada petición al servidor. [enviadoEpochMillis] es el momento en que el servidor lo aceptó (`null`
 * si aún no, o si el reporte se envió antes de que se guardara este dato). [redAlUltimoIntento] es la conexión que
 * había en el último intento.
 */
data class SeguimientoDeEnvio(
    val intentos: Int = 0,
    val ultimoIntentoEpochMillis: Long? = null,
    val enviadoEpochMillis: Long? = null,
    val ultimoFallo: FalloDeEnvio? = null,
    val redAlUltimoIntento: TipoDeRed? = null,
)
