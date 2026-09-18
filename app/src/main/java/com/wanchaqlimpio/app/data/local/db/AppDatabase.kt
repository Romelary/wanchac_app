package com.wanchaqlimpio.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity

/** Base de datos local (offline-first, ver docs/diseno.md §5). */
@Database(entities = [ReporteEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reporteDao(): ReporteDao
}
