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

/**
 * Versión 2 → 3: seguimiento del envío de cada reporte (intentos, cuándo lo aceptó el servidor, último fallo y la
 * conexión que había), para explicar el estado al ciudadano y medir la confiabilidad. Los reportes ya guardados
 * quedan con 0 intentos y el resto en `NULL` ("no se registró"), que es lo que realmente ocurrió: ninguno se pierde
 * ni cambia de estado.
 */
val MIGRACION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reporte ADD COLUMN intentosEnvio INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE reporte ADD COLUMN ultimoIntentoEpochMillis INTEGER")
            db.execSQL("ALTER TABLE reporte ADD COLUMN enviadoEpochMillis INTEGER")
            db.execSQL("ALTER TABLE reporte ADD COLUMN motivoUltimoFallo TEXT")
            db.execSQL("ALTER TABLE reporte ADD COLUMN codigoHttpUltimoFallo INTEGER")
            db.execSQL("ALTER TABLE reporte ADD COLUMN redAlGuardar TEXT")
            db.execSQL("ALTER TABLE reporte ADD COLUMN redAlUltimoIntento TEXT")
        }
    }
