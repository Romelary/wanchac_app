package com.wanchaqlimpio.app.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.wanchaqlimpio.app.core.network.MonitorDeRed
import com.wanchaqlimpio.app.data.local.db.ReporteDao
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import com.wanchaqlimpio.app.data.mapper.aDominio
import com.wanchaqlimpio.app.data.mapper.aGeoJsonFeature
import com.wanchaqlimpio.app.data.remote.api.ReportesApi
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.MotivoFalloEnvio
import com.wanchaqlimpio.app.domain.model.TipoDeRed
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

private const val ETIQUETA_LOG = "SyncWorker"
private val MEDIA_TYPE_JSON = "application/json".toMediaType()
private val MEDIA_TYPE_IMAGEN = "image/jpeg".toMediaType()

/** Tras tantos fallos por una excepción imprevista de la app, el reporte deja de reintentarse (es un error nuestro). */
internal const val MAX_INTENTOS_ERROR_INTERNO = 5

/**
 * Sube al backend los reportes con estado PENDIENTE (offline-first, ver diseno.md §4.1).
 *
 * Cada reporte se procesa **por separado**: el fallo de uno no impide enviar los demás. Según cómo termine el intento
 * (ver [ResultadoEnvio]) queda `SINCRONIZADO`, `RECHAZADO` (definitivo: no se reintentará) o sigue `PENDIENTE` para
 * reintentarlo más tarde con espera (WorkManager, ver [ProgramadorSincronizacionWorkManager]). Cada intento deja
 * constancia en el reporte (cuántos, cuándo, en qué red y por qué falló): es lo que permite medir la confiabilidad
 * del envío en las pruebas de campo.
 */
@HiltWorker
class SyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val reporteDao: ReporteDao,
        private val reportesApi: ReportesApi,
        private val gson: Gson,
        private val monitorDeRed: MonitorDeRed,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            // Los que fallaron por algo pasajero EN ESTA corrida no se reintentan hasta la siguiente (con espera).
            val conFalloPasajero = mutableSetOf<Long>()
            while (true) {
                // La lista se vuelve a leer tras cada vuelta: un reporte guardado mientras se enviaba otro se envía en
                // esta misma corrida, en vez de quedar esperando hasta el siguiente reporte o reinicio de la app.
                val pendientes = pendientesSalvo(conFalloPasajero)
                if (pendientes.isEmpty()) break
                pendientes.forEach { if (!procesar(it)) conFalloPasajero += it.id }
            }
            return if (conFalloPasajero.isEmpty()) Result.success() else Result.retry()
        }

        private suspend fun pendientesSalvo(excluidos: Set<Long>): List<ReporteEntity> =
            reporteDao.obtenerPorEstado(EstadoReporte.PENDIENTE).filterNot { it.id in excluidos }

        /** Envía UN reporte y guarda cómo salió. Devuelve `true` si dejó de estar pendiente (enviado o rechazado). */
        private suspend fun procesar(entity: ReporteEntity): Boolean {
            val inicio = System.currentTimeMillis()
            val red = monitorDeRed.actual() // la condición en la que empieza el intento
            val resultado = enviar(entity)
            // El envío se anota al TERMINAR: con una red lenta la petición tarda decenas de segundos, y «enviado» debe
            // ser cuándo lo aceptó el servidor (de ahí sale «cuánto tardó»), no cuándo salió. Se vio en la tablet: un
            // envío de 25 s quedaba anotado 25 s antes de que el servidor contestara.
            val actualizado = entity.conResultado(resultado, inicio, System.currentTimeMillis(), red)
            reporteDao.actualizar(actualizado)
            return actualizado.estado != EstadoReporte.PENDIENTE
        }

        // Un `Exception` genérico a propósito: un fallo imprevisto con UN reporte no debe tumbar el envío de los demás.
        @Suppress("TooGenericExceptionCaught")
        private suspend fun enviar(entity: ReporteEntity): ResultadoEnvio {
            val foto = File(entity.fotoPath)
            // Sin la foto no hay evidencia documental válida (Ética, Cap. I): reintentar sin ella no sirve de nada.
            if (!foto.isFile || !foto.canRead() || foto.length() == 0L) {
                return ResultadoEnvio.Definitivo(MotivoFalloEnvio.FOTO_NO_DISPONIBLE)
            }
            return try {
                val geojson = gson.toJson(entity.aDominio().aGeoJsonFeature()).toRequestBody(MEDIA_TYPE_JSON)
                val parteFoto =
                    MultipartBody.Part.createFormData(
                        "foto",
                        foto.name,
                        foto.asRequestBody(MEDIA_TYPE_IMAGEN),
                    )
                clasificarCodigoHttp(reportesApi.subirReporte(parteFoto, geojson).code())
            } catch (cancelacion: CancellationException) {
                throw cancelacion // WorkManager detuvo el trabajo: no es un fallo del reporte
            } catch (fallo: IOException) {
                Log.w(ETIQUETA_LOG, "No se pudo enviar el reporte ${entity.id}, se reintentará", fallo)
                clasificarFalloDeRed(fallo)
            } catch (fallo: Exception) {
                Log.e(ETIQUETA_LOG, "Fallo imprevisto al enviar el reporte ${entity.id}", fallo)
                ResultadoEnvio.Reintentar(MotivoFalloEnvio.ERROR_INTERNO)
            }
        }
    }

/**
 * El reporte tal como queda tras un intento: siempre con un intento más y la red en la que se hizo. El intento se
 * anota con la hora en que EMPEZÓ ([inicioEpochMillis]); el envío, con la hora en que TERMINÓ ([finEpochMillis]).
 */
private fun ReporteEntity.conResultado(
    resultado: ResultadoEnvio,
    inicioEpochMillis: Long,
    finEpochMillis: Long,
    red: TipoDeRed,
): ReporteEntity {
    val intento =
        copy(intentosEnvio = intentosEnvio + 1, ultimoIntentoEpochMillis = inicioEpochMillis, redAlUltimoIntento = red)
    return when (resultado) {
        ResultadoEnvio.Exito ->
            intento.copy(
                estado = EstadoReporte.SINCRONIZADO,
                enviadoEpochMillis = finEpochMillis,
                motivoUltimoFallo = null,
                codigoHttpUltimoFallo = null,
            )

        is ResultadoEnvio.Definitivo ->
            intento.copy(
                estado = EstadoReporte.RECHAZADO,
                motivoUltimoFallo = resultado.motivo,
                codigoHttpUltimoFallo = resultado.codigoHttp,
            )

        is ResultadoEnvio.Reintentar -> {
            val sinRemedio =
                resultado.motivo == MotivoFalloEnvio.ERROR_INTERNO &&
                    intento.intentosEnvio >= MAX_INTENTOS_ERROR_INTERNO
            intento.copy(
                estado = if (sinRemedio) EstadoReporte.RECHAZADO else estado,
                motivoUltimoFallo = resultado.motivo,
                codigoHttpUltimoFallo = resultado.codigoHttp,
            )
        }
    }
}
