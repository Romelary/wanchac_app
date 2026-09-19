package com.wanchaqlimpio.app.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.MotivoFalloEnvio
import com.wanchaqlimpio.app.domain.model.TipoDeRed
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val NOMBRE_BD = "migracion-test.db"

// El esquema EXACTO de cada versión, tal como lo generó Room (no se reescribe con las migraciones actuales).
private const val COLUMNAS_V1 =
    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`idAnonimo` TEXT NOT NULL, `categoria` TEXT NOT NULL, `estado` TEXT NOT NULL, " +
        "`latitud` REAL NOT NULL, `longitud` REAL NOT NULL, `fotoPath` TEXT NOT NULL, " +
        "`fechaHoraEpochMillis` INTEGER NOT NULL"
private const val COLUMNAS_V2 = "$COLUMNAS_V1, `categoriaIa` TEXT, `confianzaIa` REAL"

/**
 * Las migraciones no deben perder ni cambiar de estado los reportes que ya estén guardados en un teléfono (en campo
 * habrá reportes pendientes de envío al actualizar la app). Se construye a mano una base con el esquema EXACTO de la
 * versión anterior, se le mete un reporte y se abre con la versión actual. Room, al abrir, además comprueba que el
 * esquema resultante coincide con las entidades.
 */
@RunWith(RobolectricTestRunner::class)
class MigracionesTest {
    private lateinit var contexto: Context

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        contexto.deleteDatabase(NOMBRE_BD)
    }

    @After
    fun limpiar() {
        contexto.deleteDatabase(NOMBRE_BD)
    }

    private fun crearBaseAntigua(
        version: Int,
        columnas: String,
        insercion: String,
    ) {
        contexto.openOrCreateDatabase(NOMBRE_BD, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `reporte` ($columnas)")
            db.execSQL(insercion)
            db.version = version
        }
    }

    private fun crearBaseDeLaVersion1() =
        crearBaseAntigua(
            version = 1,
            columnas = COLUMNAS_V1,
            insercion =
                "INSERT INTO reporte (idAnonimo, categoria, estado, latitud, longitud, fotoPath, " +
                    "fechaHoraEpochMillis) " +
                    "VALUES ('device-1', 'PLASTICO', 'PENDIENTE', -13.53, -71.95, 'vieja.jpg', 111)",
        )

    private fun crearBaseDeLaVersion2(estado: String) =
        crearBaseAntigua(
            version = 2,
            columnas = COLUMNAS_V2,
            insercion =
                "INSERT INTO reporte (idAnonimo, categoria, estado, latitud, longitud, fotoPath, " +
                    "fechaHoraEpochMillis, categoriaIa, confianzaIa) " +
                    "VALUES ('device-1', 'VIDRIO', '$estado', -13.53, -71.95, 'v2.jpg', 222, 'PLASTICO', 0.87)",
        )

    /** `RoomDatabase` no es `Closeable`, así que no sirve `use`: se cierra a mano aunque falle la prueba. */
    private inline fun <T> conLaVersionActual(bloque: (AppDatabase) -> T): T {
        val db =
            Room
                .databaseBuilder(contexto, AppDatabase::class.java, NOMBRE_BD)
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                .allowMainThreadQueries()
                .build()
        try {
            return bloque(db)
        } finally {
            db.close()
        }
    }

    @Test
    fun `los reportes de la version 1 se conservan, sin IA y sin datos de envio`() =
        runBlocking {
            crearBaseDeLaVersion1()

            conLaVersionActual { db ->
                val reporte = db.reporteDao().obtenerPorDispositivo("device-1").single()

                assertEquals("vieja.jpg", reporte.fotoPath)
                assertEquals(CategoriaResiduo.PLASTICO, reporte.categoria)
                assertEquals(EstadoReporte.PENDIENTE, reporte.estado)
                assertEquals(111L, reporte.fechaHoraEpochMillis)
                assertNull("no hubo IA cuando se hizo ese reporte", reporte.categoriaIa)
                assertNull(reporte.confianzaIa)
                assertEquals("ningún envío se había anotado", 0, reporte.intentosEnvio)
                assertNull(reporte.motivoUltimoFallo)
                assertNull(reporte.redAlGuardar)
            }
        }

    @Test
    fun `los reportes de la version 2 se conservan con su sugerencia de IA y sin datos de envio`() =
        runBlocking {
            crearBaseDeLaVersion2(estado = "PENDIENTE")

            conLaVersionActual { db ->
                val reporte = db.reporteDao().obtenerPorDispositivo("device-1").single()

                assertEquals("v2.jpg", reporte.fotoPath)
                assertEquals(CategoriaResiduo.PLASTICO, reporte.categoriaIa)
                assertEquals(0.87f, reporte.confianzaIa)
                assertEquals(0, reporte.intentosEnvio)
                assertNull(reporte.enviadoEpochMillis)
                assertNull(reporte.ultimoIntentoEpochMillis)
            }
        }

    @Test
    fun `un reporte ya enviado antes de la migracion sigue enviado aunque no se sepa cuando`() =
        runBlocking {
            crearBaseDeLaVersion2(estado = "SINCRONIZADO")

            conLaVersionActual { db ->
                val reporte = db.reporteDao().obtenerPorDispositivo("device-1").single()

                assertEquals(EstadoReporte.SINCRONIZADO, reporte.estado)
                assertNull("no se registró cuándo se envió", reporte.enviadoEpochMillis)
            }
        }

    @Test
    fun `tras migrar se guarda y se lee el seguimiento completo del envio`() =
        runBlocking {
            crearBaseDeLaVersion2(estado = "PENDIENTE")

            conLaVersionActual { db ->
                val id =
                    db.reporteDao().insertar(
                        ReporteEntity(
                            idAnonimo = "device-1",
                            categoria = CategoriaResiduo.ORGANICO,
                            estado = EstadoReporte.RECHAZADO,
                            latitud = -13.530,
                            longitud = -71.950,
                            fotoPath = "nueva.jpg",
                            fechaHoraEpochMillis = 333L,
                            intentosEnvio = 4,
                            ultimoIntentoEpochMillis = 444L,
                            enviadoEpochMillis = null,
                            motivoUltimoFallo = MotivoFalloEnvio.RECHAZADO_POR_EL_SERVIDOR,
                            codigoHttpUltimoFallo = 422,
                            redAlGuardar = TipoDeRed.DATOS_MOVILES,
                            redAlUltimoIntento = TipoDeRed.WIFI,
                        ),
                    )

                val guardado = db.reporteDao().obtenerPorId(id)!!
                assertEquals(EstadoReporte.RECHAZADO, guardado.estado)
                assertEquals(4, guardado.intentosEnvio)
                assertEquals(444L, guardado.ultimoIntentoEpochMillis)
                assertEquals(MotivoFalloEnvio.RECHAZADO_POR_EL_SERVIDOR, guardado.motivoUltimoFallo)
                assertEquals(422, guardado.codigoHttpUltimoFallo)
                assertEquals(TipoDeRed.DATOS_MOVILES, guardado.redAlGuardar)
                assertEquals(TipoDeRed.WIFI, guardado.redAlUltimoIntento)
            }
        }
}
