package com.wanchaqlimpio.app.core.di

import com.wanchaqlimpio.app.data.repository.ConsentimientoRepositoryImpl
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindConsentimientoRepository(impl: ConsentimientoRepositoryImpl): ConsentimientoRepository
}
