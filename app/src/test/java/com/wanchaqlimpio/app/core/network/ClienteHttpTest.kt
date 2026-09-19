package com.wanchaqlimpio.app.core.network

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El motivo de estos valores está en `ClienteHttp.kt`: el plan gratuito de Render tarda ≈ 1 min en despertar y los 10 s
 * por defecto de OkHttp harían fallar SIEMPRE la primera petición tras un rato de inactividad.
 */
class ClienteHttpTest {
    private val cliente = crearClienteHttp(HttpLoggingInterceptor.Level.NONE)

    @Test
    fun `los tiempos de espera son los del diseno`() {
        assertEquals(30_000, cliente.connectTimeoutMillis)
        assertEquals(90_000, cliente.readTimeoutMillis)
        assertEquals(60_000, cliente.writeTimeoutMillis)
        assertEquals(150_000, cliente.callTimeoutMillis)
    }

    @Test
    fun `la lectura aguanta el arranque en frio de Render (aprox 1 minuto) con margen`() {
        val arranqueEnFrioRenderMillis = 60_000
        assertTrue(cliente.readTimeoutMillis >= arranqueEnFrioRenderMillis * 3 / 2)
    }

    @Test
    fun `el tope de la llamada cubre conexion y lectura para que nada se cuelgue para siempre`() {
        assertTrue(cliente.callTimeoutMillis > 0) // 0 significaría "sin tope"
        assertTrue(cliente.callTimeoutMillis >= cliente.connectTimeoutMillis + cliente.readTimeoutMillis)
    }

    @Test
    fun `el nivel de registro pedido llega al interceptor`() {
        listOf(HttpLoggingInterceptor.Level.NONE, HttpLoggingInterceptor.Level.BODY).forEach { nivel ->
            val interceptor =
                crearClienteHttp(nivel).interceptors.filterIsInstance<HttpLoggingInterceptor>().single()
            assertEquals(nivel, interceptor.level)
        }
    }

    @Test
    fun `el nivel NONE de publicacion no registra nada, ni identificadores ni fotos`() {
        val interceptor = cliente.interceptors.filterIsInstance<HttpLoggingInterceptor>().single()
        assertEquals(HttpLoggingInterceptor.Level.NONE, interceptor.level)
    }
}
