package com.wanchaqlimpio.app.core.di

import com.wanchaqlimpio.app.data.repository.AlmacenFotosImpl
import com.wanchaqlimpio.app.domain.repository.AlmacenFotos
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    @Binds
    @Singleton
    abstract fun bindAlmacenFotos(impl: AlmacenFotosImpl): AlmacenFotos
}
