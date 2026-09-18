package com.wanchaqlimpio.app.data.remote.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Backend aún no desplegado (ver docs/plan.md §4). El contrato completo y sus
 * garantías (idempotencia, lápida de eliminación) están en docs/diseno.md §4.2.
 * Las pruebas usan MockWebServer; en desarrollo, BuildConfig.BASE_URL apunta a
 * un mock local (ver core/di/NetworkModule.kt).
 */
interface ReportesApi {
    @Multipart
    @POST("reportes")
    suspend fun subirReporte(
        @Part foto: MultipartBody.Part,
        @Part("geojson") geojson: RequestBody,
    ): Response<Unit>

    /**
     * Solicita la baja de un reporte (RNF-05, ADR-0002). Sin login, la clave
     * (idAnonimo, idLocal) es la única forma de identificar el reporte en el servidor.
     */
    @DELETE("reportes/{idAnonimo}/{idLocal}")
    suspend fun eliminarReporte(
        @Path("idAnonimo") idAnonimo: String,
        @Path("idLocal") idLocal: Long,
    ): Response<Unit>
}
