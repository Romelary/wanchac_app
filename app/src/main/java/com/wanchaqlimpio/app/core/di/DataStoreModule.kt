package com.wanchaqlimpio.app.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val PREFERENCES_NAME = "consentimiento_prefs"

private val Context.consentimientoDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_NAME,
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideConsentimientoDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.consentimientoDataStore
}
