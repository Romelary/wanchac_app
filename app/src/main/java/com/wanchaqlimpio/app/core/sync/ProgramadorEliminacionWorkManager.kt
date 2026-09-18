package com.wanchaqlimpio.app.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ProgramadorEliminacionWorkManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ProgramadorEliminacion {
        override fun programarEliminacionRemota(
            idAnonimo: String,
            idLocal: Long,
        ) {
            val solicitud =
                OneTimeWorkRequestBuilder<EliminarReporteWorker>()
                    .setInputData(
                        workDataOf(
                            EliminarReporteWorker.CLAVE_ID_ANONIMO to idAnonimo,
                            EliminarReporteWorker.CLAVE_ID_LOCAL to idLocal,
                        ),
                    ).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS,
                    ).build()

            // Un trabajo único por reporte: si el usuario repite la acción no se duplica la solicitud.
            WorkManager.getInstance(context).enqueueUniqueWork(
                "eliminar_reporte_${idAnonimo}_$idLocal",
                ExistingWorkPolicy.KEEP,
                solicitud,
            )
        }
    }
