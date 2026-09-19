package com.wanchaqlimpio.app.domain.model

/**
 * Reporte individual de un ciudadano (Marco Conceptual, Cap. II): foto +
 * coordenadas + marca temporal + categoría. [idAnonimo] identifica el
 * dispositivo, no a la persona (ADR-0001) — se usa solo para el filtro de
 * duplicados y para "Mis reportes" (Módulo 6).
 *
 * [categoria] es la que el ciudadano CONFIRMÓ (puede haber corregido la sugerencia). [categoriaIa] y
 * [confianzaIa] son la respuesta cruda del modelo de IA, guardadas aparte para poder medir la exactitud
 * de la IA (Anexo 2) sin mezclarla con las correcciones humanas; son `null` si el modelo no estuvo
 * disponible (ver [SugerenciaIa]).
 *
 * [redAlGuardar] y [envio] son datos técnicos para las pruebas de confiabilidad y para explicar el estado del envío
 * (ver [SeguimientoDeEnvio]); solo viven en el teléfono, nunca se mandan al servidor.
 */
data class Reporte(
    val id: Long = 0,
    val idAnonimo: String,
    val categoria: CategoriaResiduo,
    val estado: EstadoReporte,
    val latitud: Double,
    val longitud: Double,
    val fotoPath: String,
    val fechaHoraEpochMillis: Long,
    val categoriaIa: CategoriaResiduo? = null,
    val confianzaIa: Float? = null,
    val redAlGuardar: TipoDeRed? = null,
    val envio: SeguimientoDeEnvio = SeguimientoDeEnvio(),
) {
    /**
     * Un reporte rechazado se puede reintentar a mano (el rechazo pudo ser un error del servidor, no del reporte),
     * salvo que la causa sea que la foto ya no está: sin foto no hay nada que reenviar.
     */
    val sePuedeReintentar: Boolean
        get() = estado == EstadoReporte.RECHAZADO && envio.ultimoFallo?.motivo != MotivoFalloEnvio.FOTO_NO_DISPONIBLE
}
