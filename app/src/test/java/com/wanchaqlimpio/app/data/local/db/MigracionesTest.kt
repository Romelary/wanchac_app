package com.wanchaqlimpio.app.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val NOMBRE_BD = "migracion-test.db"

/**
 * La migración 1 → 2 no debe perder los reportes que ya estén guardados en un teléfono. Se construye a mano una
 * base con el esquema EXACTO de la versión 1 (tal como la generó Room), se le mete un reporte y se abre con la
 * versión 2 de la app. Room, al abrir, además comprueba que el esquema resultante coincide con las entidades.
 */
@RunWith(RobolectricTestRunner::class)
class MigracionesTest {
    private lateinit var contexto: Context

    @Before
    fun crearBaseDeLaVersion1() {
        contexto = ApplicationProvider.getApplicationContext()
        contexto.deleteDatabase(NOMBRE_BD)
        contexto.openOrCreateDatabase(NOMBRE_BD, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `reporte` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`idAnonimo` TEXT NOT NULL, `categoria` TEXT NOT NULL, `estado` TEXT NOT NULL, " +
                    "`latitud` REAL NOT NULL, `longitud` REAL NOT NULL, `fotoPath` TEXT NOT NULL, " +
                    "`fechaHoraEpochMillis` INTEGER NOT NULL)",
            )
            db.execSQL(
                "INSERT INTO reporte (idAnonimo, categoria, estado, latitud, longitud, fotoPath, " +
                    "fechaHoraEpochMillis) " +
                    "VALUES ('device-1', 'PLASTICO', 'PENDIENTE', -13.53, -71.95, 'vieja.jpg', 111)",
            )
            db.version = 1
        }
    }

    @After
    fun limpiar() {
        contexto.deleteDatabase(NOMBRE_BD)
    }

    /** `RoomDatabase` no es `Closeable`, así que no sirve `use`: se cierra a mano aunque falle la prueba. */
    private inline fun <T> conLaVersion2(bloque: (AppDatabase) -> T): T {
        val db = abrirConLaVersion2()
        try {
            return bloque(db)
        } finally {
            db.close()
        }
    }

    private fun abrirConLaVersion2() =
        Room
            .databaseBuilder(contexto, AppDatabase::class.java, NOMBRE_BD)
            .addMigrations(MIGRACION_1_2)
            .allowMainThreadQueries()
            .build()

    @Test
    fun `los reportes de la version 1 se conservan y quedan sin sugerencia de IA`() =
        runBlocking {
            conLaVersion2 { db ->
                val reporte = db.reporteDao().obtenerPorDispositivo("device-1").single()

                assertEquals("vieja.jpg", reporte.fotoPath)
                assertEquals(CategoriaResiduo.PLASTICO, reporte.categoria)
                assertEquals(111L, reporte.fechaHoraEpochMillis)
                assertNull("no hubo IA cuando se hizo ese reporte", reporte.categoriaIa)
                assertNull(reporte.confianzaIa)
            }
        }

    @Test
    fun `tras migrar se pueden guardar reportes con la sugerencia de IA`() =
        runBlocking {
            conLaVersion2 { db ->
                val id =
                    db.reporteDao().insertar(
                        ReporteEntity(
                            idAnonimo = "device-1",
                            categoria = CategoriaResiduo.VIDRIO,
                            estado = EstadoReporte.PENDIENTE,
                            latitud = -13.530,
                            longitud = -71.950,
                            fotoPath = "nueva.jpg",
                            fechaHoraEpochMillis = 222L,
                            categoriaIa = CategoriaResiduo.PLASTICO,
                            confianzaIa = 0.87f,
                        ),
                    )

                val guardado = db.reporteDao().obtenerPorId(id)!!
                assertEquals(CategoriaResiduo.VIDRIO, guardado.categoria)
                assertEquals(CategoriaResiduo.PLASTICO, guardado.categoriaIa)
                assertEquals(0.87f, guardado.confianzaIa)
            }
        }
}
