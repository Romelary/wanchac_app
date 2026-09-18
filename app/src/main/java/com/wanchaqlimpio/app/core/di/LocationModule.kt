package com.wanchaqlimpio.app.core.di

import com.wanchaqlimpio.app.core.location.ProveedorUbicacion
import com.wanchaqlimpio.app.core.location.ProveedorUbicacionFused
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    @Singleton
    abstract fun bindProveedorUbicacion(impl: ProveedorUbicacionFused): ProveedorUbicacion
}
