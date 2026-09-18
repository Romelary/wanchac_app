package com.wanchaqlimpio.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {
    @Insert
    suspend fun insertar(entity: ReporteEntity): Long

    @Query("SELECT * FROM reporte WHERE idAnonimo = :idAnonimo ORDER BY fechaHoraEpochMillis DESC")
    fun observarPorDispositivo(idAnonimo: String): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reporte WHERE idAnonimo = :idAnonimo ORDER BY fechaHoraEpochMillis DESC")
    suspend fun obtenerPorDispositivo(idAnonimo: String): List<ReporteEntity>

    @Query("SELECT * FROM reporte WHERE estado = :estado")
    suspend fun obtenerPorEstado(estado: EstadoReporte): List<ReporteEntity>

    @Query("UPDATE reporte SET estado = :nuevoEstado WHERE id = :id")
    suspend fun actualizarEstado(
        id: Long,
        nuevoEstado: EstadoReporte,
    )
}
