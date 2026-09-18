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
)
