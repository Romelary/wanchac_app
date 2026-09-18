package com.wanchaqlimpio.app.data.remote.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Backend aún no desplegado (ver docs/plan.md §4) — este contrato es el
 * acordado en diseno.md §3.6.4 (multipart: foto + metadata GeoJSON).
 * Las pruebas usan MockWebServer; en desarrollo, BuildConfig.BASE_URL
 * apunta a un mock local (ver core/di/NetworkModule.kt).
 */
interface ReportesApi {
    @Multipart
    @POST("reportes")
    suspend fun subirReporte(
        @Part foto: MultipartBody.Part,
        @Part("geojson") geojson: RequestBody,
    ): Response<Unit>
}
