package com.wanchaqlimpio.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT * FROM reporte WHERE id = :id")
    suspend fun obtenerPorId(id: Long): ReporteEntity?

    @Query("DELETE FROM reporte WHERE id = :id")
    suspend fun eliminar(id: Long)

    /** Del más antiguo al más nuevo: el envío respeta el orden en que se hicieron los reportes. */
    @Query("SELECT * FROM reporte WHERE estado = :estado ORDER BY fechaHoraEpochMillis ASC, id ASC")
    suspend fun obtenerPorEstado(estado: EstadoReporte): List<ReporteEntity>

    @Query("UPDATE reporte SET estado = :nuevoEstado WHERE id = :id")
    suspend fun actualizarEstado(
        id: Long,
        nuevoEstado: EstadoReporte,
    )

    /**
     * Guarda el resultado de un intento de envío. Si el reporte ya no existe (el ciudadano lo eliminó mientras se
     * enviaba) no hace nada: `@Update` nunca lo vuelve a crear.
     */
    @Update
    suspend fun actualizar(entity: ReporteEntity)
}
