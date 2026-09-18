package com.wanchaqlimpio.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad placeholder para validar que Room compila y persiste correctamente.
 * Se reemplaza en el Módulo 2 (docs/plan.md) por las entidades reales
 * (Reporte, CategoriaResiduo, EstadoReporte) definidas en docs/diseno.md §6.
 */
@Entity(tableName = "prueba")
data class PruebaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descripcion: String,
)
