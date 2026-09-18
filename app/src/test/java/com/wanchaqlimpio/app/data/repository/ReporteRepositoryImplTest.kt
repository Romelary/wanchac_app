package com.wanchaqlimpio.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wanchaqlimpio.app.core.sync.ProgramadorEliminacion
import com.wanchaqlimpio.app.core.sync.ProgramadorSincronizacion
import com.wanchaqlimpio.app.data.local.db.AppDatabase
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.domain.model.Ubicacion
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import com.wanchaqlimpio.app.domain.usecase.ValidarNuevoReporte
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

private const val LATITUD_DENTRO = -13.530
private const val LONGITUD_DENTRO = -71.950

private class ProgramadorSincronizacionFalso : ProgramadorSincronizacion {
    var vecesLlamado = 0
        private set

    override fun programarSincronizacion() {
        vecesLlamado++
    }
}

private class ProgramadorEliminacionFalso : ProgramadorEliminacion {
    val solicitudes = mutableListOf<Pair<String, Long>>()

    override fun programarEliminacionRemota(
        idAnonimo: String,
        idLocal: Long,
    ) {
        solicitudes += idAnonimo to idLocal
    }
}

/** Room en memoria vía Robolectric (mismo motivo que ConsentimientoRepositoryImplTest, ver pruebas.md). */
@RunWith(RobolectricTestRunner::class)
class ReporteRepositoryImplTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: ReporteRepositoryImpl
    private lateinit var programadorSincronizacion: ProgramadorSincronizacionFalso
    private lateinit var programadorEliminacion: ProgramadorEliminacionFalso
    private lateinit var carpetaFotos: File

    @Before
    fun crearBaseDeDatos() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(contexto, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        programadorSincronizacion = ProgramadorSincronizacionFalso()
        programadorEliminacion = ProgramadorEliminacionFalso()
        repositorio =
            ReporteRepositoryImpl(
                db.reporteDao(),
                ValidarNuevoReporte(),
                programadorSincronizacion,
                programadorEliminacion,
                // Almacén real (no un fake): así se comprueba que la foto se borra del disco de verdad.
                AlmacenFotosImpl(contexto),
            )
        carpetaFotos = File(contexto.cacheDir, "repo-test").apply { mkdirs() }
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
        carpetaFotos.deleteRecursively()
    }

    private suspend fun insertarReporteConFoto(estado: EstadoReporte = EstadoReporte.PENDIENTE): Pair<Long, File> {
        val foto = File(carpetaFotos, "foto-${System.nanoTime()}.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val id =
            db.reporteDao().insertar(
                ReporteEntity(
                    idAnonimo = "device-1",
                    categoria = CategoriaResiduo.PLASTICO,
                    estado = estado,
                    latitud = LATITUD_DENTRO,
                    longitud = LONGITUD_DENTRO,
                    fotoPath = foto.absolutePath,
                    fechaHoraEpochMillis = 0L,
                ),
            )
        return id to foto
    }

    @Test
    fun `eliminar borra el reporte, su foto del disco y solicita la baja remota`() =
        runTest {
            val (id, foto) = insertarReporteConFoto()

            repositorio.eliminarReporte(id)

            assertNull(db.reporteDao().obtenerPorId(id))
            assertFalse("la foto debe borrarse del almacenamiento de la app", foto.exists())
            assertEquals(listOf("device-1" to id), programadorEliminacion.solicitudes)
        }

    @Test
    fun `eliminar solicita la baja remota tambien si el reporte figuraba como PENDIENTE`() =
        runTest {
            // El servidor pudo recibirlo y la respuesta perderse: el cliente no puede saberlo.
            val (id, _) = insertarReporteConFoto(estado = EstadoReporte.PENDIENTE)

            repositorio.eliminarReporte(id)

            assertEquals(1, programadorEliminacion.solicitudes.size)
        }

    @Test
    fun `eliminar un reporte que no existe no hace nada`() =
        runTest {
            repositorio.eliminarReporte(id = 999L)

            assertEquals(0, programadorEliminacion.solicitudes.size)
        }

    @Test
    fun `eliminar tolera que la foto ya no exista en disco`() =
        runTest {
            val (id, foto) = insertarReporteConFoto()
            foto.delete()

            repositorio.eliminarReporte(id)

            assertNull(db.reporteDao().obtenerPorId(id))
            assertEquals(1, programadorEliminacion.solicitudes.size)
        }

    @Test
    fun `un reporte valido se guarda como PENDIENTE y aparece al observar`() =
        runTest {
            val resultado =
                repositorio.crearReporte(
                    idAnonimo = "device-1",
                    categoria = CategoriaResiduo.PLASTICO,
                    ubicacion = Ubicacion(LATITUD_DENTRO, LONGITUD_DENTRO, precisionMetros = 5f),
                    fotoPath = "foto.jpg",
                    ahoraEpochMillis = 0L,
                )

            assertEquals(ResultadoValidacionReporte.Valido, resultado)
            val guardados = repositorio.observarReportesDe("device-1").first()
            assertEquals(1, guardados.size)
            assertEquals(EstadoReporte.PENDIENTE, guardados.first().estado)
            assertEquals(1, programadorSincronizacion.vecesLlamado)
        }

    @Test
    fun `la sugerencia cruda de la IA se guarda aparte de la categoria que confirmo el ciudadano`() =
        runTest {
            repositorio.crearReporte(
                idAnonimo = "device-1",
                categoria = CategoriaResiduo.VIDRIO, // el ciudadano corrigió al modelo
                ubicacion = Ubicacion(LATITUD_DENTRO, LONGITUD_DENTRO, precisionMetros = 5f),
                fotoPath = "foto.jpg",
                ahoraEpochMillis = 0L,
                sugerenciaIa =
                    SugerenciaIa(CategoriaResiduo.PLASTICO, confianza = 0.87f, propuesta = CategoriaResiduo.PLASTICO),
            )

            val guardado = repositorio.observarReportesDe("device-1").first().single()
            assertEquals(CategoriaResiduo.VIDRIO, guardado.categoria)
            assertEquals(CategoriaResiduo.PLASTICO, guardado.categoriaIa)
            assertEquals(0.87f, guardado.confianzaIa)
        }

    @Test
    fun `sin sugerencia de la IA el reporte se guarda con esos campos vacios`() =
        runTest {
            repositorio.crearReporte(
                idAnonimo = "device-1",
                categoria = CategoriaResiduo.ORGANICO,
                ubicacion = Ubicacion(LATITUD_DENTRO, LONGITUD_DENTRO, precisionMetros = 5f),
                fotoPath = "foto.jpg",
                ahoraEpochMillis = 0L,
            )

            val guardado = repositorio.observarReportesDe("device-1").first().single()
            assertNull(guardado.categoriaIa)
            assertNull(guardado.confianzaIa)
        }

    @Test
    fun `un reporte rechazado no se persiste`() =
        runTest {
            val resultado =
                repositorio.crearReporte(
                    idAnonimo = "device-1",
                    categoria = CategoriaResiduo.PLASTICO,
                    ubicacion = Ubicacion(LATITUD_DENTRO, LONGITUD_DENTRO, precisionMetros = 20f),
                    fotoPath = "foto.jpg",
                    ahoraEpochMillis = 0L,
                )

            assert(resultado is ResultadoValidacionReporte.PrecisionInsuficiente)
            val guardados = repositorio.observarReportesDe("device-1").first()
            assertEquals(0, guardados.size)
            assertEquals(0, programadorSincronizacion.vecesLlamado)
        }

    @Test
    fun `un segundo reporte del mismo dispositivo cerca y rapido se detecta como duplicado y no se guarda`() =
        runTest {
            repositorio.crearReporte(
                idAnonimo = "device-1",
                categoria = CategoriaResiduo.PLASTICO,
                ubicacion = Ubicacion(LATITUD_DENTRO, LONGITUD_DENTRO, precisionMetros = 5f),
                fotoPath = "foto1.jpg",
                ahoraEpochMillis = 0L,
            )

            val resultado =
                repositorio.crearReporte(
                    idAnonimo = "device-1",
                    categoria = CategoriaResiduo.PLASTICO,
                    ubicacion = Ubicacion(LATITUD_DENTRO, LONGITUD_DENTRO, precisionMetros = 5f),
                    fotoPath = "foto2.jpg",
                    ahoraEpochMillis = 60_000L,
                )

            assertEquals(ResultadoValidacionReporte.Duplicado, resultado)
            val guardados = repositorio.observarReportesDe("device-1").first()
            assertEquals(1, guardados.size)
        }
}
