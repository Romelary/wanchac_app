package com.wanchaqlimpio.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versión 1 → 2: guarda la sugerencia cruda del modelo de IA (Módulo 3b) junto a la categoría confirmada.
 * Las columnas son opcionales: los reportes ya guardados quedan con `NULL` ("sin sugerencia"), que es lo que
 * realmente ocurrió, y no se pierde ninguno.
 */
val MIGRACION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reporte ADD COLUMN categoriaIa TEXT")
            db.execSQL("ALTER TABLE reporte ADD COLUMN confianzaIa REAL")
        }
    }
