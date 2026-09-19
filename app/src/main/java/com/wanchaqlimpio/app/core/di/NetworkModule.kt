package com.wanchaqlimpio.app.core.di

import android.content.Context
import com.google.gson.Gson
import com.wanchaqlimpio.app.BuildConfig
import com.wanchaqlimpio.app.core.network.MonitorDeRed
import com.wanchaqlimpio.app.core.network.MonitorDeRedAndroid
import com.wanchaqlimpio.app.core.network.crearClienteHttp
import com.wanchaqlimpio.app.data.remote.api.PuntosCriticosApi
import com.wanchaqlimpio.app.data.remote.api.ReportesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideMonitorDeRed(
        @ApplicationContext context: Context,
    ): MonitorDeRed = MonitorDeRedAndroid(context)

    /**
     * BASIC en depuración (línea de petición y código de respuesta, nada más): con BODY el registro copia enteras las
     * peticiones —con la foto y las coordenadas— y falsearía las mediciones de tiempo de las pruebas de campo.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        crearClienteHttp(
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE,
        )

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideReportesApi(retrofit: Retrofit): ReportesApi = retrofit.create(ReportesApi::class.java)

    @Provides
    @Singleton
    fun providePuntosCriticosApi(retrofit: Retrofit): PuntosCriticosApi = retrofit.create(PuntosCriticosApi::class.java)
}
