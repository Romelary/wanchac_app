package com.wanchaqlimpio.app.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.gson.Gson
import com.wanchaqlimpio.app.core.network.MonitorDeRed
import com.wanchaqlimpio.app.data.local.db.AppDatabase
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import com.wanchaqlimpio.app.data.remote.api.ReportesApi
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.MotivoFalloEnvio
import com.wanchaqlimpio.app.domain.model.TipoDeRed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.work.ListenableWorker.Result as WorkResult

private class MonitorDeRedFalso(
    var red: TipoDeRed = TipoDeRed.WIFI,
) : MonitorDeRed {
    override fun actual(): TipoDeRed = red
}

/** Una API que hace lo que la prueba diga en cada llamada (para lo que MockWebServer no puede simular: excepciones). */
private class ReportesApiGuionada(
    private val alLlamar: suspend (numeroDeLlamada: Int) -> Response<Unit>,
) : ReportesApi {
    var llamadas = 0
        private set

    override suspend fun subirReporte(
        foto: MultipartBody.Part,
        geojson: RequestBody,
    ): Response<Unit> = alLlamar(++llamadas)

    override suspend fun eliminarReporte(
        idAnonimo: String,
        idLocal: Long,
    ): Response<Unit> = Response.success(Unit)
}

/**
 * El envío de reportes (Módulo 4) tal como corre en el teléfono: Room real (en memoria) y HTTP real contra un servidor
 * de mentira. Verifica qué pasa con cada reporte según cómo responda la red (ver [ResultadoEnvioTest] para la tabla
 * de códigos) y que cada intento deje constancia para medir la confiabilidad.
 */
@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {
    private lateinit var db: AppDatabase
    private lateinit var mockWebServer: MockWebServer
    private lateinit var reportesApi: ReportesApi
    private lateinit var carpetaFotos: File
    private val monitorDeRed = MonitorDeRedFalso()
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun preparar() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        mockWebServer = MockWebServer()
        mockWebServer.start()
        reportesApi = crearApi(OkHttpClient())
        carpetaFotos = File(context.cacheDir, "sync-test").apply { mkdirs() }
    }

    @After
    fun limpiar() {
        db.close()
        mockWebServer.close()
        carpetaFotos.deleteRecursively()
    }

    private fun crearApi(cliente: OkHttpClient): ReportesApi =
        Retrofit
            .Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReportesApi::class.java)

    private suspend fun insertarReportePendiente(
        nombreFoto: String = "foto.jpg",
        crearArchivo: Boolean = true,
        fechaHora: Long = 0L,
        contenidoFoto: ByteArray = byteArrayOf(1, 2, 3),
    ): Long {
        val archivoFoto = File(carpetaFotos, nombreFoto)
        if (crearArchivo) archivoFoto.writeBytes(contenidoFoto)
        return db.reporteDao().insertar(
            ReporteEntity(
                idAnonimo = "device-1",
                categoria = CategoriaResiduo.PLASTICO,
                estado = EstadoReporte.PENDIENTE,
                latitud = -13.530,
                longitud = -71.950,
                fotoPath = archivoFoto.absolutePath,
                fechaHoraEpochMillis = fechaHora,
            ),
        )
    }

    private fun crearWorker(api: ReportesApi = reportesApi): SyncWorker =
        TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = SyncWorker(appContext, workerParameters, db.reporteDao(), api, Gson(), monitorDeRed)
                },
            ).build()

    private suspend fun reporte(id: Long) = db.reporteDao().obtenerPorId(id)!!

    // --- Lo que debe pasar cuando todo sale bien ----------------------------------------------------------

    @Test
    fun `sin reportes pendientes el worker termina exitoso sin llamar a la red`() =
        runTest {
            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.success(), resultado)
            assertEquals(0, mockWebServer.requestCount)
        }

    @Test
    fun `un reporte que el servidor acepta queda SINCRONIZADO y anota cuando, cuantos intentos y con que red`() =
        runTest {
            val id = insertarReportePendiente()
            monitorDeRed.red = TipoDeRed.DATOS_MOVILES
            mockWebServer.enqueue(MockResponse(code = 201))
            val antes = System.currentTimeMillis()

            val resultado = crearWorker().doWork()

            val despues = System.currentTimeMillis()
            assertEquals(WorkResult.success(), resultado)
            val enviado = reporte(id)
            assertEquals(EstadoReporte.SINCRONIZADO, enviado.estado)
            assertEquals(1, enviado.intentosEnvio)
            assertTrue(enviado.enviadoEpochMillis!! in antes..despues)
            assertEquals(enviado.enviadoEpochMillis, enviado.ultimoIntentoEpochMillis)
            assertEquals(TipoDeRed.DATOS_MOVILES, enviado.redAlUltimoIntento)
            assertNull(enviado.motivoUltimoFallo)
        }

    @Test
    fun `un 200 (el servidor ya lo tenia) tambien cuenta como enviado`() =
        runTest {
            val id = insertarReportePendiente()
            mockWebServer.enqueue(MockResponse(code = 200))

            crearWorker().doWork()

            assertEquals(EstadoReporte.SINCRONIZADO, reporte(id).estado)
        }

    // --- Fallos pasajeros: el reporte NO se pierde y se reintenta -------------------------------------------

    @Test
    fun `si el servidor falla el reporte sigue PENDIENTE, se anota el fallo y el worker pide reintentar`() =
        runTest {
            val id = insertarReportePendiente()
            mockWebServer.enqueue(MockResponse(code = 503))

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.retry(), resultado)
            val pendiente = reporte(id)
            assertEquals(EstadoReporte.PENDIENTE, pendiente.estado)
            assertEquals(1, pendiente.intentosEnvio)
            assertEquals(MotivoFalloEnvio.SERVIDOR_OCUPADO, pendiente.motivoUltimoFallo)
            assertEquals(503, pendiente.codigoHttpUltimoFallo)
            assertNull(pendiente.enviadoEpochMillis)
        }

    @Test
    fun `un 404 no rechaza el reporte porque suele ser una direccion equivocada o un servicio recien desplegado`() =
        runTest {
            val id = insertarReportePendiente()
            mockWebServer.enqueue(MockResponse(code = 404))

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.retry(), resultado)
            assertEquals(EstadoReporte.PENDIENTE, reporte(id).estado)
            assertEquals(MotivoFalloEnvio.RESPUESTA_INESPERADA, reporte(id).motivoUltimoFallo)
        }

    @Test
    fun `sin servidor al otro lado el reporte sigue PENDIENTE con motivo SIN_CONEXION`() =
        runTest {
            val id = insertarReportePendiente()
            monitorDeRed.red = TipoDeRed.SIN_RED
            mockWebServer.close() // nadie escucha: la conexión es rechazada

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.retry(), resultado)
            val pendiente = reporte(id)
            assertEquals(EstadoReporte.PENDIENTE, pendiente.estado)
            assertEquals(MotivoFalloEnvio.SIN_CONEXION, pendiente.motivoUltimoFallo)
            assertEquals(TipoDeRed.SIN_RED, pendiente.redAlUltimoIntento)
        }

    @Test
    fun `una red lenta agota el tiempo y se anota como TIEMPO_AGOTADO, no como falta de conexion`() =
        runTest {
            val id = insertarReportePendiente()
            val clienteImpaciente = OkHttpClient.Builder().readTimeout(100, TimeUnit.MILLISECONDS).build()
            mockWebServer.enqueue(
                MockResponse
                    .Builder()
                    .code(201)
                    .headersDelay(1, TimeUnit.SECONDS)
                    .build(),
            )

            val resultado = crearWorker(api = crearApi(clienteImpaciente)).doWork()

            assertEquals(WorkResult.retry(), resultado)
            assertEquals(MotivoFalloEnvio.TIEMPO_AGOTADO, reporte(id).motivoUltimoFallo)
            assertEquals(EstadoReporte.PENDIENTE, reporte(id).estado)
        }

    @Test
    fun `cada nuevo intento suma uno y borra el fallo anterior cuando por fin sale bien`() =
        runTest {
            val id = insertarReportePendiente()
            mockWebServer.enqueue(MockResponse(code = 500))
            mockWebServer.enqueue(MockResponse(code = 502))
            mockWebServer.enqueue(MockResponse(code = 201))

            crearWorker().doWork()
            crearWorker().doWork()
            assertEquals(2, reporte(id).intentosEnvio)
            assertEquals(502, reporte(id).codigoHttpUltimoFallo)
            crearWorker().doWork()

            val enviado = reporte(id)
            assertEquals(EstadoReporte.SINCRONIZADO, enviado.estado)
            assertEquals(3, enviado.intentosEnvio)
            assertNull("un envío correcto ya no tiene fallo que mostrar", enviado.motivoUltimoFallo)
            assertNull(enviado.codigoHttpUltimoFallo)
        }

    // --- Rechazos definitivos: NO se reintentan para siempre -----------------------------------------------

    @Test
    fun `un rechazo definitivo del servidor deja el reporte RECHAZADO y no se reintenta`() =
        runTest {
            listOf(400, 410, 413, 415, 422).forEachIndexed { indice, codigo ->
                val id = insertarReportePendiente(nombreFoto = "foto-$codigo.jpg", fechaHora = indice.toLong())
                mockWebServer.enqueue(MockResponse(code = codigo))

                val resultado = crearWorker().doWork()

                assertEquals("codigo $codigo", WorkResult.success(), resultado) // success = no reintentar
                val rechazado = reporte(id)
                assertEquals("codigo $codigo", EstadoReporte.RECHAZADO, rechazado.estado)
                assertEquals(MotivoFalloEnvio.RECHAZADO_POR_EL_SERVIDOR, rechazado.motivoUltimoFallo)
                assertEquals(codigo, rechazado.codigoHttpUltimoFallo)
                assertEquals(1, rechazado.intentosEnvio)
            }
            assertEquals("una petición por reporte, ninguna repetida", 5, mockWebServer.requestCount)
        }

    @Test
    fun `si la foto ya no existe el reporte queda RECHAZADO sin llamar a la red`() =
        runTest {
            val id = insertarReportePendiente(crearArchivo = false)

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.success(), resultado)
            assertEquals(0, mockWebServer.requestCount)
            assertEquals(EstadoReporte.RECHAZADO, reporte(id).estado)
            assertEquals(MotivoFalloEnvio.FOTO_NO_DISPONIBLE, reporte(id).motivoUltimoFallo)
        }

    @Test
    fun `una foto vacia tampoco es evidencia y queda RECHAZADO`() =
        runTest {
            val id = insertarReportePendiente(contenidoFoto = byteArrayOf())

            crearWorker().doWork()

            assertEquals(0, mockWebServer.requestCount)
            assertEquals(MotivoFalloEnvio.FOTO_NO_DISPONIBLE, reporte(id).motivoUltimoFallo)
        }

    // --- Un reporte no debe afectar a los demás --------------------------------------------------------------

    @Test
    fun `el fallo de un reporte no impide enviar los demas y el orden es el de captura`() =
        runTest {
            // Se insertan desordenados a propósito: deben enviarse del más antiguo al más nuevo.
            val tercero = insertarReportePendiente("c.jpg", fechaHora = 300)
            val primero = insertarReportePendiente("a.jpg", fechaHora = 100)
            val segundo = insertarReportePendiente("b.jpg", fechaHora = 200)
            mockWebServer.enqueue(MockResponse(code = 500)) // el primero falla
            mockWebServer.enqueue(MockResponse(code = 201))
            mockWebServer.enqueue(MockResponse(code = 201))

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.retry(), resultado)
            assertEquals(EstadoReporte.PENDIENTE, reporte(primero).estado)
            assertEquals(EstadoReporte.SINCRONIZADO, reporte(segundo).estado)
            assertEquals(EstadoReporte.SINCRONIZADO, reporte(tercero).estado)
            val enOrden = (1..3).map { idLocalDe(mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!) }
            assertEquals(listOf(primero, segundo, tercero), enOrden)
        }

    @Test
    fun `una excepcion imprevista con un reporte no tumba el envio de los otros`() =
        runTest {
            val malo = insertarReportePendiente("malo.jpg", fechaHora = 1)
            val bueno = insertarReportePendiente("bueno.jpg", fechaHora = 2)
            val api =
                ReportesApiGuionada { llamada ->
                    if (llamada == 1) error("fallo imprevisto de prueba") else Response.success(Unit)
                }

            val resultado = crearWorker(api).doWork()

            assertEquals(WorkResult.retry(), resultado)
            assertEquals(MotivoFalloEnvio.ERROR_INTERNO, reporte(malo).motivoUltimoFallo)
            assertEquals(EstadoReporte.PENDIENTE, reporte(malo).estado)
            assertEquals(EstadoReporte.SINCRONIZADO, reporte(bueno).estado)
        }

    @Test
    fun `un error interno que se repite deja de reintentarse tras cinco intentos`() =
        runTest {
            val id = insertarReportePendiente()
            val api = ReportesApiGuionada { error("siempre falla") }

            repeat(MAX_INTENTOS_ERROR_INTERNO - 1) {
                crearWorker(api).doWork()
                assertEquals("aún se reintenta", EstadoReporte.PENDIENTE, reporte(id).estado)
            }
            val resultado = crearWorker(api).doWork()

            assertEquals(WorkResult.success(), resultado)
            assertEquals(EstadoReporte.RECHAZADO, reporte(id).estado)
            assertEquals(MotivoFalloEnvio.ERROR_INTERNO, reporte(id).motivoUltimoFallo)
            assertEquals(MAX_INTENTOS_ERROR_INTERNO, reporte(id).intentosEnvio)
        }

    @Test
    fun `un reporte guardado mientras se envia otro se envia en la misma corrida`() =
        runTest {
            val primero = insertarReportePendiente("a.jpg", fechaHora = 1)
            var segundo = 0L
            // Al llegar la primera petición el ciudadano guarda otro reporte: justo el hueco en que antes se perdía.
            mockWebServer.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        if (segundo == 0L) segundo = runBlocking { insertarReportePendiente("b.jpg", fechaHora = 2) }
                        return MockResponse(code = 201)
                    }
                }

            val resultado = crearWorker().doWork()

            assertEquals(WorkResult.success(), resultado)
            assertEquals(2, mockWebServer.requestCount)
            assertEquals(EstadoReporte.SINCRONIZADO, reporte(primero).estado)
            assertEquals(EstadoReporte.SINCRONIZADO, reporte(segundo).estado)
        }

    @Test
    fun `si el reporte se elimina mientras se envia no se vuelve a crear`() =
        runTest {
            val id = insertarReportePendiente()
            val api =
                ReportesApiGuionada {
                    db.reporteDao().eliminar(id) // el ciudadano lo borra con la subida en vuelo
                    Response.success(Unit)
                }

            crearWorker(api).doWork()

            assertNull("no debe resucitar un reporte eliminado", db.reporteDao().obtenerPorId(id))
        }

    @Test
    fun `cancelar el trabajo no cuenta como un fallo del reporte`() =
        runTest {
            val id = insertarReportePendiente()
            val api = ReportesApiGuionada { throw CancellationException("WorkManager detuvo el trabajo") }

            try {
                crearWorker(api).doWork()
                fail("la cancelación debe propagarse")
            } catch (esperada: CancellationException) {
                assertNotNull(esperada)
            }

            assertEquals(EstadoReporte.PENDIENTE, reporte(id).estado)
            assertEquals("no se anotó ningún intento", 0, reporte(id).intentosEnvio)
        }

    /** El `idLocal` que la app mandó en el GeoJSON de esa petición. */
    private fun idLocalDe(peticion: RecordedRequest): Long =
        Regex("\"idLocal\":(\\d+)").find(peticion.body!!.utf8())!!.groupValues[1].toLong()
}
