package com.wanchaqlimpio.app.core.sync

import com.wanchaqlimpio.app.domain.model.MotivoFalloEnvio
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Cómo terminó un intento de enviar un reporte. La diferencia entre [Reintentar] y [Definitivo] es lo que evita que
 * un reporte que el servidor nunca aceptará se reenvíe para siempre (gastando batería y datos), y a la vez que un
 * fallo pasajero de la red (lo normal en la calle) haga perder un reporte válido.
 */
sealed interface ResultadoEnvio {
    /** El servidor lo aceptó (201) o ya lo tenía (200, idempotente). */
    data object Exito : ResultadoEnvio

    /** Algo pasajero: se vuelve a intentar más tarde. */
    data class Reintentar(
        val motivo: MotivoFalloEnvio,
        val codigoHttp: Int? = null,
    ) : ResultadoEnvio

    /** No se aceptará nunca tal como está: no se reintenta y el reporte queda como `RECHAZADO`. */
    data class Definitivo(
        val motivo: MotivoFalloEnvio,
        val codigoHttp: Int? = null,
    ) : ResultadoEnvio
}

private const val HTTP_SOLICITUD_INVALIDA = 400
private const val HTTP_TIEMPO_DE_ESPERA = 408
private const val HTTP_YA_NO_EXISTE = 410
private const val HTTP_FOTO_DEMASIADO_GRANDE = 413
private const val HTTP_TIPO_NO_SOPORTADO = 415
private const val HTTP_NO_PROCESABLE = 422
private const val HTTP_DEMASIADAS_SOLICITUDES = 429
private const val HTTP_PRIMER_ERROR_DEL_SERVIDOR = 500

/**
 * Los códigos del contrato de `POST /reportes` (docs/diseno.md §4.2):
 *  - 2xx: aceptado. 400, 410, 413, 415 y 422: **este** reporte no se puede aceptar (mal formado, dado de baja, foto
 *    enorme, no es JPEG, fuera del distrito): reintentar no cambia nada.
 *  - 408, 429 y 5xx: el servidor está ocupado o falló: se reintenta.
 *  - Todo lo demás (404, 401, 403, redirecciones…) **no dice nada sobre el reporte**: es casi siempre una
 *    configuración (dirección equivocada, servicio recién desplegado, un proxy). Se reintenta en vez de marcar como
 *    rechazado un reporte que en realidad está bien.
 */
fun clasificarCodigoHttp(codigo: Int): ResultadoEnvio =
    when {
        codigo in 200..299 -> ResultadoEnvio.Exito
        codigo in CODIGOS_DEFINITIVOS -> ResultadoEnvio.Definitivo(MotivoFalloEnvio.RECHAZADO_POR_EL_SERVIDOR, codigo)
        esDelServidor(codigo) -> ResultadoEnvio.Reintentar(MotivoFalloEnvio.SERVIDOR_OCUPADO, codigo)
        else -> ResultadoEnvio.Reintentar(MotivoFalloEnvio.RESPUESTA_INESPERADA, codigo)
    }

private fun esDelServidor(codigo: Int): Boolean =
    codigo == HTTP_TIEMPO_DE_ESPERA || codigo == HTTP_DEMASIADAS_SOLICITUDES || codigo >= HTTP_PRIMER_ERROR_DEL_SERVIDOR

private val CODIGOS_DEFINITIVOS =
    setOf(
        HTTP_SOLICITUD_INVALIDA,
        HTTP_YA_NO_EXISTE,
        HTTP_FOTO_DEMASIADO_GRANDE,
        HTTP_TIPO_NO_SOPORTADO,
        HTTP_NO_PROCESABLE,
    )

/**
 * Una excepción al hablar con el servidor. Un tiempo de espera agotado (`SocketTimeoutException` y el tope de toda
 * la llamada de OkHttp son `InterruptedIOException`) se distingue de una falta de conexión porque el remedio es
 * otro: en el primer caso la red existe pero es lenta (o el servidor gratuito estaba despertando).
 */
fun clasificarFalloDeRed(fallo: IOException): ResultadoEnvio.Reintentar =
    ResultadoEnvio.Reintentar(
        if (fallo is InterruptedIOException) MotivoFalloEnvio.TIEMPO_AGOTADO else MotivoFalloEnvio.SIN_CONEXION,
    )
