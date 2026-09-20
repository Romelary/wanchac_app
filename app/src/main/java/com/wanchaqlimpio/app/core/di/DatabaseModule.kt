package com.wanchaqlimpio.app.core.di

import android.content.Context
import androidx.room.Room
import com.wanchaqlimpio.app.data.local.db.AppDatabase
import com.wanchaqlimpio.app.data.local.db.MIGRACION_1_2
import com.wanchaqlimpio.app.data.local.db.MIGRACION_2_3
import com.wanchaqlimpio.app.data.local.db.MIGRACION_3_4
import com.wanchaqlimpio.app.data.local.db.ReporteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "wanchaq_limpio.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4)
            .build()

    @Provides
    fun provideReporteDao(database: AppDatabase): ReporteDao = database.reporteDao()
}
