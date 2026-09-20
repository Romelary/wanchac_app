package com.wanchaqlimpio.app.domain.usecase

import com.wanchaqlimpio.app.domain.model.Reporte
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val MILISEGUNDOS_POR_SEGUNDO = 1_000L
private val SALTOS_DE_LINEA = Regex("[\r\n]+")

/** Las columnas del registro, en su orden. `id_local` y `capturado_utc` equivalen a las de la base de Neon. */
internal val COLUMNAS_DEL_REGISTRO =
    listOf(
        "id_local",
        "capturado_utc",
        "categoria",
        "categoria_ia",
        "confianza_ia",
        "precision_gps_m",
        "estado",
        "intentos",
        "ultimo_intento_utc",
        "enviado_utc",
        "segundos_hasta_envio",
        "motivo_ultimo_fallo",
        "codigo_http",
        "red_al_guardar",
        "red_ultimo_intento",
    )

/**
 * El «registro de envíos»: una tabla CSV con un renglón por reporte que cuenta qué pasó con su envío (cuántos
 * intentos, cuándo se capturó y se envió, en qué red y por qué falló). Es lo que el investigador recoge del teléfono
 * en las pruebas de confiabilidad (Anexo 2) para cotejarlo con lo que hay en el servidor; ver
 * `docs/pruebas-de-confiabilidad.md`.
 *
 * **No lleva** la foto, las coordenadas ni el código anónimo del dispositivo: el registro se comparte por correo o por
 * chat, y con `id_local` basta para cotejar cada renglón con el servidor.
 *
 * Las horas van en UTC (ISO 8601, al segundo), como las de Neon. Los reportes salen en el orden en que se capturaron.
 * [comentarios] son líneas que se anteponen con `# ` (versión de la app, modelo del teléfono…): los lectores usuales
 * de CSV las saltan (`comment='#'` en pandas, `comment.char` en R).
 */
fun List<Reporte>.aCsvDeEnvios(comentarios: List<String> = emptyList()): String {
    val lineas = mutableListOf<String>()
    comentarios.mapTo(lineas) { "# " + it.replace(SALTOS_DE_LINEA, " ") }
    lineas += COLUMNAS_DEL_REGISTRO.joinToString(",")
    sortedBy { it.id }.mapTo(lineas) { reporte -> reporte.aRenglon().joinToString(",") { escaparCsv(it) } }
    return lineas.joinToString(separator = "\n", postfix = "\n")
}

private fun Reporte.aRenglon(): List<String> =
    listOf(
        id.toString(),
        instante(fechaHoraEpochMillis),
        categoria.name,
        categoriaIa?.name.orEmpty(),
        confianzaIa?.let { "%.2f".format(Locale.ROOT, it) }.orEmpty(),
        precisionMetros?.let { "%.1f".format(Locale.ROOT, it) }.orEmpty(),
        estado.name,
        envio.intentos.toString(),
        envio.ultimoIntentoEpochMillis?.let(::instante).orEmpty(),
        envio.enviadoEpochMillis?.let(::instante).orEmpty(),
        // Sin ocultar los negativos: un reloj del teléfono mal puesto debe verse en el registro, no esconderse.
        envio.enviadoEpochMillis?.let { ((it - fechaHoraEpochMillis) / MILISEGUNDOS_POR_SEGUNDO).toString() }.orEmpty(),
        envio.ultimoFallo
            ?.motivo
            ?.name
            .orEmpty(),
        envio.ultimoFallo
            ?.codigoHttp
            ?.toString()
            .orEmpty(),
        redAlGuardar?.name.orEmpty(),
        envio.redAlUltimoIntento?.name.orEmpty(),
    )

private fun instante(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).truncatedTo(ChronoUnit.SECONDS).toString()

private val CARACTERES_QUE_OBLIGAN_A_ENTRECOMILLAR = charArrayOf(',', '"', '\n', '\r')

/** Una celda de CSV (RFC 4180): va entre comillas, duplicándolas, si lleva coma, comillas o salto de línea. */
internal fun escaparCsv(celda: String): String =
    if (celda.indexOfAny(CARACTERES_QUE_OBLIGAN_A_ENTRECOMILLAR) >= 0) {
        "\"" + celda.replace("\"", "\"\"") + "\""
    } else {
        celda
    }
