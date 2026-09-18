package com.wanchaqlimpio.app.domain.model

/**
 * Reporte individual de un ciudadano (Marco Conceptual, Cap. II): foto +
 * coordenadas + marca temporal + categoría. [idAnonimo] identifica el
 * dispositivo, no a la persona (ADR-0001) — se usa solo para el filtro de
 * duplicados y para "Mis reportes" (Módulo 6).
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
)
