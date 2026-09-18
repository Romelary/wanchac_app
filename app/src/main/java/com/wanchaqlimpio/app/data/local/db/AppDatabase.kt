package com.wanchaqlimpio.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wanchaqlimpio.app.data.local.entity.PruebaEntity

/**
 * Base de datos local (offline-first, ver docs/diseno.md §5).
 * La entidad PruebaEntity es temporal (Módulo 0); se reemplaza en el Módulo 2.
 */
@Database(entities = [PruebaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pruebaDao(): PruebaDao
}
