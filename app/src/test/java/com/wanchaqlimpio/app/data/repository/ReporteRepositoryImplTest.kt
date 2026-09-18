package com.wanchaqlimpio.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wanchaqlimpio.app.core.sync.ProgramadorSincronizacion
import com.wanchaqlimpio.app.data.local.db.AppDatabase
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Ubicacion
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import com.wanchaqlimpio.app.domain.usecase.ValidarNuevoReporte
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val LATITUD_DENTRO = -13.530
private const val LONGITUD_DENTRO = -71.950

private class ProgramadorSincronizacionFalso : ProgramadorSincronizacion {
    var vecesLlamado = 0
        private set

    override fun programarSincronizacion() {
        vecesLlamado++
    }
}

/** Room en memoria vía Robolectric (mismo motivo que ConsentimientoRepositoryImplTest, ver pruebas.md). */
@RunWith(RobolectricTestRunner::class)
class ReporteRepositoryImplTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: ReporteRepositoryImpl
    private lateinit var programadorSincronizacion: ProgramadorSincronizacionFalso

    @Before
    fun crearBaseDeDatos() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        programadorSincronizacion = ProgramadorSincronizacionFalso()
        repositorio = ReporteRepositoryImpl(db.reporteDao(), ValidarNuevoReporte(), programadorSincronizacion)
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
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
