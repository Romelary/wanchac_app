package com.wanchaqlimpio.app.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val NOMBRE_TRABAJO_SINCRONIZACION = "sincronizar_reportes"

class ProgramadorSincronizacionWorkManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ProgramadorSincronizacion {
        override fun programarSincronizacion() {
            val restricciones =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val solicitud =
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(restricciones)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS,
                    ).build()

            // KEEP: SyncWorker procesa TODOS los pendientes en cada corrida, así
            // que si ya hay una sincronización encolada no hace falta duplicarla.
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOMBRE_TRABAJO_SINCRONIZACION,
                ExistingWorkPolicy.KEEP,
                solicitud,
            )
        }
    }
