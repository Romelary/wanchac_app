package com.wanchaqlimpio.app.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.wanchaqlimpio.app.data.local.db.ReporteDao
import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import com.wanchaqlimpio.app.data.mapper.aDominio
import com.wanchaqlimpio.app.data.mapper.aGeoJsonFeature
import com.wanchaqlimpio.app.data.remote.api.ReportesApi
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

private const val ETIQUETA_LOG = "SyncWorker"
private val MEDIA_TYPE_JSON = "application/json".toMediaType()
private val MEDIA_TYPE_IMAGEN = "image/jpeg".toMediaType()

/**
 * Sube al backend los reportes con estado PENDIENTE (offline-first, ver
 * diseno.md §4.1). WorkManager reintenta con backoff cuando se devuelve
 * Result.retry() (ver ProgramadorSincronizacionWorkManager, que fija la
 * política de reintento al encolar este worker).
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
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val pendientes = reporteDao.obtenerPorEstado(EstadoReporte.PENDIENTE)
            if (pendientes.isEmpty()) return Result.success()

            var huboFallo = false
            for (entity in pendientes) {
                if (intentarSincronizar(entity)) {
                    reporteDao.actualizarEstado(entity.id, EstadoReporte.SINCRONIZADO)
                } else {
                    huboFallo = true
                }
            }
            return if (huboFallo) Result.retry() else Result.success()
        }

        private suspend fun intentarSincronizar(entity: ReporteEntity): Boolean {
            val archivoFoto = File(entity.fotoPath)
            // Sin la foto no hay evidencia documental válida (Ética, Cap. I) — no
            // tiene sentido reintentar un archivo que ya no existe.
            if (!archivoFoto.exists()) return false

            return try {
                val feature = entity.aDominio().aGeoJsonFeature()
                val geojsonBody = gson.toJson(feature).toRequestBody(MEDIA_TYPE_JSON)
                val fotoPart =
                    MultipartBody.Part.createFormData(
                        "foto",
                        archivoFoto.name,
                        archivoFoto.asRequestBody(MEDIA_TYPE_IMAGEN),
                    )
                reportesApi.subirReporte(fotoPart, geojsonBody).isSuccessful
            } catch (e: IOException) {
                Log.w(ETIQUETA_LOG, "No se pudo sincronizar el reporte ${entity.id}, se reintentará", e)
                false
            }
        }
    }
