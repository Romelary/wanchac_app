package com.wanchaqlimpio.app.core.di

import android.content.Context
import android.util.Log
import com.wanchaqlimpio.app.BuildConfig
import com.wanchaqlimpio.app.core.location.ProveedorUbicacion
import com.wanchaqlimpio.app.core.location.ProveedorUbicacionFused
import com.wanchaqlimpio.app.core.location.ProveedorUbicacionSimulado
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.TimeSource

private const val ETIQUETA_LOG = "ProveedorUbicacion"

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    /**
     * La ubicación real (Google Play Services), salvo en una compilación de PRUEBA con `-PubicacionSimulada=true`.
     * `UBICACION_SIMULADA` es siempre `false` en release (ver `app/build.gradle.kts`).
     */
    @Provides
    @Singleton
    fun provideProveedorUbicacion(
        @ApplicationContext context: Context,
    ): ProveedorUbicacion =
        if (BuildConfig.UBICACION_SIMULADA) {
            Log.w(
                ETIQUETA_LOG,
                "UBICACIÓN SIMULADA activa: compilación de prueba, los reportes llevan coordenadas falsas",
            )
            ProveedorUbicacionSimulado()
        } else {
            ProveedorUbicacionFused(context)
        }

    /** El reloj con el que se mide la edad de una lectura del GPS: monótono (no lo altera un cambio de hora). */
    @Provides
    fun provideFuenteDeTiempo(): TimeSource = TimeSource.Monotonic
}
