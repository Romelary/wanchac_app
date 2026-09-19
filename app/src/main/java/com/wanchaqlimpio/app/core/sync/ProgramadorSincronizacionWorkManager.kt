package com.wanchaqlimpio.app.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val NOMBRE_TRABAJO_SINCRONIZACION = "sincronizar_reportes"

/**
 * Espera entre reintentos: LINEAL (30 s, 60 s, 90 s…) y no exponencial. Con la exponencial (10 s, 20 s, 40 s…) tras una
 * racha de fallos la espera llega a horas, justo cuando la conexión vuelve; con la lineal, tras 20 intentos son 10 min.
 */
private const val ESPERA_BASE_SEGUNDOS = 30L

class ProgramadorSincronizacionWorkManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ProgramadorSincronizacion {
        /**
         * APPEND_OR_REPLACE, no KEEP: `SyncWorker` lee los pendientes al empezar; con KEEP, un reporte guardado
         * mientras el trabajo está corriendo no encolaba nada y quedaba sin enviar hasta el siguiente reporte.
         * Encadenado detrás del que corre, se envía en cuanto este termine; si el anterior falló o se canceló,
         * empieza uno nuevo.
         */
        override fun programarSincronizacion() = encolar(ExistingWorkPolicy.APPEND_OR_REPLACE)

        /** KEEP: al abrir la app (y al girarla) no se apilan envíos; solo se asegura que haya uno. */
        override fun sincronizarAlIniciar() = encolar(ExistingWorkPolicy.KEEP)

        override fun sincronizarAhora() = encolar(ExistingWorkPolicy.REPLACE)

        private fun encolar(politica: ExistingWorkPolicy) {
            val restricciones =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val solicitud =
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(restricciones)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, ESPERA_BASE_SEGUNDOS, TimeUnit.SECONDS)
                    .build()

            // SyncWorker procesa TODOS los pendientes en cada corrida, así que varios trabajos encolados no duplican
            // envíos: los que sobran encuentran la lista vacía y terminan al instante.
            WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE_TRABAJO_SINCRONIZACION, politica, solicitud)
        }
    }
