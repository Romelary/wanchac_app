package com.wanchaqlimpio.app.core.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.wanchaqlimpio.app.domain.model.Ubicacion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * El permiso de ubicación se verifica en la UI (RF-02/Módulo 2) antes de
 * llamar a esta clase; @SuppressLint porque el linter no puede ver esa
 * verificación externa.
 */
class ProveedorUbicacionFused
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ProveedorUbicacion {
        @SuppressLint("MissingPermission")
        override suspend fun obtenerUbicacionActual(): Ubicacion? =
            suspendCancellableCoroutine { continuacion ->
                val cliente = LocationServices.getFusedLocationProviderClient(context)
                val tokenCancelacion = CancellationTokenSource()

                cliente
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenCancelacion.token)
                    .addOnSuccessListener { location ->
                        val ubicacion = location?.let { Ubicacion(it.latitude, it.longitude, it.accuracy) }
                        continuacion.resume(ubicacion)
                    }.addOnFailureListener {
                        continuacion.resume(null)
                    }

                continuacion.invokeOnCancellation { tokenCancelacion.cancel() }
            }
    }
