package com.wanchaqlimpio.app.core.di

import com.wanchaqlimpio.app.core.sync.ProgramadorEliminacion
import com.wanchaqlimpio.app.core.sync.ProgramadorEliminacionWorkManager
import com.wanchaqlimpio.app.core.sync.ProgramadorSincronizacion
import com.wanchaqlimpio.app.core.sync.ProgramadorSincronizacionWorkManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    @Singleton
    abstract fun bindProgramadorSincronizacion(impl: ProgramadorSincronizacionWorkManager): ProgramadorSincronizacion

    @Binds
    @Singleton
    abstract fun bindProgramadorEliminacion(impl: ProgramadorEliminacionWorkManager): ProgramadorEliminacion
}
