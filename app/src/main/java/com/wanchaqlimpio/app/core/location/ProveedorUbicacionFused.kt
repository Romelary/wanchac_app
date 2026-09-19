package com.wanchaqlimpio.app.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wanchaqlimpio.app.domain.model.Ubicacion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

private const val ETIQUETA_LOG = "ProveedorUbicacion"
private const val INTERVALO_MILLIS = 1_000L
private const val INTERVALO_MINIMO_MILLIS = 500L

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
        override fun lecturas(): Flow<Ubicacion> =
            callbackFlow {
                val cliente = LocationServices.getFusedLocationProviderClient(context)
                val peticion =
                    LocationRequest
                        .Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVALO_MILLIS)
                        .setMinUpdateIntervalMillis(INTERVALO_MINIMO_MILLIS)
                        .build()
                val escucha =
                    object : LocationCallback() {
                        override fun onLocationResult(resultado: LocationResult) {
                            // Una lectura sin precisión declarada vale 0 m en Android: se descarta, porque pasaría
                            // por la más exacta del mundo en el filtro de 15 m.
                            resultado.locations
                                .filter { it.hasAccuracy() }
                                .forEach { trySend(Ubicacion(it.latitude, it.longitude, it.accuracy)) }
                        }
                    }
                try {
                    cliente
                        .requestLocationUpdates(peticion, escucha, Looper.getMainLooper())
                        .addOnFailureListener { fallo ->
                            Log.w(ETIQUETA_LOG, "No se pudo pedir la ubicación", fallo)
                            close()
                        }
                } catch (ignored: SecurityException) {
                    // Sin permiso de ubicación: el flujo termina sin lecturas y la pantalla lo explica.
                    Log.w(ETIQUETA_LOG, "Falta el permiso de ubicación")
                    close()
                }
                awaitClose { cliente.removeLocationUpdates(escucha) }
            }
    }
