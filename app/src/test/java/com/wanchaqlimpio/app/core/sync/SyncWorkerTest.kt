package com.wanchaqlimpio.app.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.gson.Gson
import com.wanchaqlimpio.app.data.local.db.AppDatabase
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import com.wanchaqlimpio.app.data.remote.api.ReportesApi
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import androidx.work.ListenableWorker.Result as WorkResult

@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {
    private lateinit var db: AppDatabase
    private lateinit var mockWebServer: MockWebServer
    private lateinit var reportesApi: ReportesApi
    private lateinit var carpetaFotos: File
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun preparar() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

        mockWebServer = MockWebServer()
        mockWebServer.start()
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        reportesApi = retrofit.create(ReportesApi::class.java)

        carpetaFotos = File(context.cacheDir, "sync-test").apply { mkdirs() }
    }

    @After
    fun limpiar() {
        db.close()
        mockWebServer.close()
        carpetaFotos.deleteRecursively()
    }

    private suspend fun insertarReportePendiente(
        nombreFoto: String = "foto.jpg",
        crearArchivo: Boolean = true,
    ): Long {
        val archivoFoto = File(carpetaFotos, nombreFoto)
        if (crearArchivo) archivoFoto.writeBytes(byteArrayOf(1, 2, 3))
        return db.reporteDao().insertar(
            ReporteEntity(
                idAnonimo = "device-1",
                categoria = CategoriaResiduo.PLASTICO,
                estado = EstadoReporte.PENDIENTE,
                latitud = -13.530,
                longitud = -71.950,
                fotoPath = archivoFoto.absolutePath,
                fechaHoraEpochMillis = 0L,
            ),
        )
    }

    private fun crearWorker(): SyncWorker =
        TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = SyncWorker(appContext, workerParameters, db.reporteDao(), reportesApi, Gson())
                },
            ).build()

    @Test
    fun `sin reportes pendientes el worker termina exitoso sin llamar a la red`() =
        runTest {
            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.success(), resultado)
            assertEquals(0, mockWebServer.requestCount)
        }

    @Test
    fun `un reporte pendiente que el servidor acepta queda SINCRONIZADO`() =
        runTest {
            insertarReportePendiente()
            mockWebServer.enqueue(MockResponse(code = 200))

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.success(), resultado)
            val sincronizados = db.reporteDao().obtenerPorEstado(EstadoReporte.SINCRONIZADO)
            assertEquals(1, sincronizados.size)
        }

    @Test
    fun `si el servidor falla el reporte queda PENDIENTE y el worker pide reintentar`() =
        runTest {
            insertarReportePendiente()
            mockWebServer.enqueue(MockResponse(code = 500))

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.retry(), resultado)
            assertEquals(1, db.reporteDao().obtenerPorEstado(EstadoReporte.PENDIENTE).size)
        }

    @Test
    fun `si la foto ya no existe en disco se pide reintentar - limitacion conocida, ver progreso md`() =
        runTest {
            insertarReportePendiente(crearArchivo = false)

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.retry(), resultado)
            assertEquals(0, mockWebServer.requestCount)
        }
}
