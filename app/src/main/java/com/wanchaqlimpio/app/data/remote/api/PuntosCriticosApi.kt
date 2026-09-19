package com.wanchaqlimpio.app.data.remote.api

import com.wanchaqlimpio.app.data.remote.dto.PuntosCriticosDto
import retrofit2.Response
import retrofit2.http.GET

/**
 * Lectura del mapa público (docs/diseno.md §4.2). Es de solo lectura y **no lleva identificador de dispositivo**: los
 * focos son públicos. El servidor devuelve únicamente los confirmados en campo.
 */
interface PuntosCriticosApi {
    @GET("puntos-criticos")
    suspend fun obtenerConfirmados(): Response<PuntosCriticosDto>
}
