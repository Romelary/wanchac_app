package com.wanchaqlimpio.app.core.sync

import com.wanchaqlimpio.app.domain.model.MotivoFalloEnvio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * La tabla que decide qué pasa con un reporte tras cada respuesta del servidor (docs/diseno.md §4.2, «Códigos y qué
 * hace la app»). Un error aquí tiene dos formas de dañar la confiabilidad: reintentar para siempre un reporte que el
 * servidor nunca aceptará, o marcar como rechazado un reporte válido por un fallo pasajero.
 */
class ResultadoEnvioTest {
    @Test
    fun `cualquier 2xx es exito, tanto el 201 (creado) como el 200 (ya lo tenia)`() {
        listOf(200, 201, 204, 299).forEach { codigo ->
            assertEquals("codigo $codigo", ResultadoEnvio.Exito, clasificarCodigoHttp(codigo))
        }
    }

    @Test
    fun `los codigos que dicen que ESTE reporte no se puede aceptar son definitivos`() {
        listOf(400, 410, 413, 415, 422).forEach { codigo ->
            assertEquals(
                "codigo $codigo",
                ResultadoEnvio.Definitivo(MotivoFalloEnvio.RECHAZADO_POR_EL_SERVIDOR, codigo),
                clasificarCodigoHttp(codigo),
            )
        }
    }

    @Test
    fun `un servidor ocupado o caido (408, 429 y todo 5xx) se reintenta`() {
        listOf(408, 429, 500, 502, 503, 504, 599).forEach { codigo ->
            assertEquals(
                "codigo $codigo",
                ResultadoEnvio.Reintentar(MotivoFalloEnvio.SERVIDOR_OCUPADO, codigo),
                clasificarCodigoHttp(codigo),
            )
        }
    }

    @Test
    fun `lo que no dice nada del reporte (404, 401, 403, redirecciones) se reintenta y no lo rechaza`() {
        // Un 404 suele ser una dirección equivocada o un servicio recién desplegado: el reporte está bien.
        listOf(301, 302, 401, 403, 404, 405, 409, 418).forEach { codigo ->
            assertEquals(
                "codigo $codigo",
                ResultadoEnvio.Reintentar(MotivoFalloEnvio.RESPUESTA_INESPERADA, codigo),
                clasificarCodigoHttp(codigo),
            )
        }
    }

    @Test
    fun `ningun codigo HTTP queda sin clasificar y solo cinco son definitivos`() {
        val resultados = (100..599).associateWith(::clasificarCodigoHttp)

        assertEquals((200..299).toList(), resultados.filterValues { it == ResultadoEnvio.Exito }.keys.toList())
        assertEquals(
            listOf(400, 410, 413, 415, 422),
            resultados.filterValues { it is ResultadoEnvio.Definitivo }.keys.toList(),
        )
    }

    @Test
    fun `un tiempo de espera agotado se distingue de la falta de conexion`() {
        val agotados = listOf(SocketTimeoutException("read timed out"), InterruptedIOException("timeout"))

        agotados.forEach { fallo ->
            assertEquals(ResultadoEnvio.Reintentar(MotivoFalloEnvio.TIEMPO_AGOTADO), clasificarFalloDeRed(fallo))
        }
    }

    @Test
    fun `sin red, sin servidor o con un fallo de TLS es falta de conexion y se reintenta`() {
        val sinConexion =
            listOf(
                UnknownHostException("sin DNS"),
                ConnectException("rechazada"),
                SSLHandshakeException("portal cautivo"),
                EOFException("se cerro a medias"),
                IOException("otra cosa"),
            )

        sinConexion.forEach { fallo ->
            val resultado = clasificarFalloDeRed(fallo)
            assertTrue(resultado.toString(), resultado.motivo == MotivoFalloEnvio.SIN_CONEXION)
        }
    }
}
