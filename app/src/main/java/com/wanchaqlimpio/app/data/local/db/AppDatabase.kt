package com.wanchaqlimpio.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity

/**
 * Base de datos local (offline-first, ver docs/diseno.md §5).
 * Versión 2: agrega la sugerencia cruda de la IA a `reporte` (ver [MIGRACION_1_2]).
 * Versión 3: agrega el seguimiento del envío (ver [MIGRACION_2_3]).
 */
@Database(entities = [ReporteEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reporteDao(): ReporteDao
}
