package com.wanchaqlimpio.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wanchaqlimpio.app.data.local.entity.PruebaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PruebaDao {
    @Insert
    suspend fun insertar(entity: PruebaEntity): Long

    @Query("SELECT * FROM prueba")
    fun observarTodos(): Flow<List<PruebaEntity>>
}
