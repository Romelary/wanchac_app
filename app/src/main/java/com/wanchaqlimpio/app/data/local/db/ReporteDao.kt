package com.wanchaqlimpio.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {
    @Insert
    suspend fun insertar(entity: ReporteEntity): Long

    @Query("SELECT * FROM reporte WHERE idAnonimo = :idAnonimo ORDER BY fechaHoraEpochMillis DESC")
    fun observarPorDispositivo(idAnonimo: String): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reporte WHERE idAnonimo = :idAnonimo ORDER BY fechaHoraEpochMillis DESC")
    suspend fun obtenerPorDispositivo(idAnonimo: String): List<ReporteEntity>
}
