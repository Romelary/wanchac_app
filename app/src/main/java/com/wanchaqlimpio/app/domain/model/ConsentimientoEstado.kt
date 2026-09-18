package com.wanchaqlimpio.app.domain.model

/**
 * Estado del consentimiento informado (Anexo 9) en este dispositivo.
 * [idAnonimo] es un UUID v4 generado localmente al aceptar; no identifica
 * a la persona (ver docs/diseno.md §0 y docs/adr/0001-sin-login-id-anonimo.md).
 */
data class ConsentimientoEstado(
    val idAnonimo: String?,
    val fechaConsentimientoEpochMillis: Long?,
) {
    val haConsentido: Boolean get() = idAnonimo != null
}
