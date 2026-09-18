package com.wanchaqlimpio.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte

/**
 * Reflejo local (Room) de un Reporte, según diseno.md §6. `idAnonimo`
 * reemplaza al `id_usuario` de cuenta del Cap. IV original (ADR-0001).
 */
@Entity(tableName = "reporte")
data class ReporteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idAnonimo: String,
    val categoria: CategoriaResiduo,
    val estado: EstadoReporte,
    val latitud: Double,
    val longitud: Double,
    val fotoPath: String,
    val fechaHoraEpochMillis: Long,
)
