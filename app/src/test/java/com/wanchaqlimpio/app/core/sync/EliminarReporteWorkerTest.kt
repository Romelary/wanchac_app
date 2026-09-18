package com.wanchaqlimpio.app.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.wanchaqlimpio.app.data.remote.api.ReportesApi
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
import java.util.concurrent.TimeUnit
import androidx.work.ListenableWorker.Result as WorkResult

@RunWith(RobolectricTestRunner::class)
class EliminarReporteWorkerTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var reportesApi: ReportesApi
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun preparar() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        reportesApi =
            Retrofit
                .Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ReportesApi::class.java)
    }

    @After
    fun limpiar() {
        mockWebServer.close()
    }

    private fun crearWorker(
        idAnonimo: String? = "device-1",
        idLocal: Long? = 42L,
    ): EliminarReporteWorker {
        val datos =
            workDataOf(
                *listOfNotNull(
                    idAnonimo?.let { EliminarReporteWorker.CLAVE_ID_ANONIMO to it },
                    idLocal?.let { EliminarReporteWorker.CLAVE_ID_LOCAL to it },
                ).toTypedArray(),
            )
        return TestListenableWorkerBuilder<EliminarReporteWorker>(context)
            .setInputData(datos)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = EliminarReporteWorker(appContext, workerParameters, reportesApi)
                },
            ).build()
    }

    @Test
    fun `envia un DELETE a reportes idAnonimo idLocal y termina exitoso si el servidor confirma`() =
        runTest {
            mockWebServer.enqueue(MockResponse(code = 204))

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.success(), resultado)
            val peticion = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
            assertEquals("DELETE", peticion?.method)
            assertEquals("/reportes/device-1/42", peticion?.target)
        }

    @Test
    fun `un 404 cuenta como exito porque el dato ya no existe en el servidor`() =
        runTest {
            mockWebServer.enqueue(MockResponse(code = 404))

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.success(), resultado)
        }

    @Test
    fun `si el servidor falla se pide reintentar para no perder la solicitud de baja`() =
        runTest {
            mockWebServer.enqueue(MockResponse(code = 500))

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.retry(), resultado)
        }

    @Test
    fun `una solicitud sin datos de entrada falla sin llamar a la red`() =
        runTest {
            val resultado = crearWorker(idAnonimo = null, idLocal = null).doWork()

            assertEquals(WorkResult.failure(), resultado)
            assertEquals(0, mockWebServer.requestCount)
        }
}
