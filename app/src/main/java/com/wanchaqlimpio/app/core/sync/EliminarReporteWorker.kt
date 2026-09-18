package com.wanchaqlimpio.app.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wanchaqlimpio.app.data.remote.api.ReportesApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

private const val ETIQUETA_LOG = "EliminarReporteWorker"
private const val HTTP_NO_ENCONTRADO = 404
private const val ID_LOCAL_AUSENTE = -1L

/**
 * Envía al backend la solicitud de baja de un reporte que el usuario ya
 * eliminó del dispositivo (RNF-05, ADR-0002). Se encola aunque el reporte
 * nunca haya llegado a sincronizarse: si el servidor lo recibió pero la
 * respuesta se perdió (red inestable), el cliente no puede saberlo, y así
 * no queda un dato huérfano en el servidor.
 *
 * Es idempotente: un 404 (el servidor no tiene ese reporte) cuenta como
 * éxito, porque el resultado buscado — que el dato no exista — ya se cumple.
 */
@HiltWorker
class EliminarReporteWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val reportesApi: ReportesApi,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val idAnonimo = inputData.getString(CLAVE_ID_ANONIMO)
            val idLocal = inputData.getLong(CLAVE_ID_LOCAL, ID_LOCAL_AUSENTE)
            // Una solicitud mal formada no mejora reintentando.
            if (idAnonimo == null || idLocal == ID_LOCAL_AUSENTE) return Result.failure()

            return try {
                val respuesta = reportesApi.eliminarReporte(idAnonimo, idLocal)
                val baja = respuesta.isSuccessful || respuesta.code() == HTTP_NO_ENCONTRADO
                if (baja) Result.success() else Result.retry()
            } catch (e: IOException) {
                // No se registra idAnonimo: es la clave que identifica al dispositivo.
                Log.w(ETIQUETA_LOG, "No se pudo enviar la baja del reporte $idLocal, se reintentará", e)
                Result.retry()
            }
        }

        companion object {
            const val CLAVE_ID_ANONIMO = "idAnonimo"
            const val CLAVE_ID_LOCAL = "idLocal"
        }
    }
