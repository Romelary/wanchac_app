package com.wanchaqlimpio.app.domain.usecase

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.FalloDeEnvio
import com.wanchaqlimpio.app.domain.model.MotivoFalloEnvio
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.SeguimientoDeEnvio
import com.wanchaqlimpio.app.domain.model.TipoDeRed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * El registro de envíos es lo que el investigador lleva del teléfono a la hoja de cálculo para las pruebas de
 * confiabilidad: cada columna y cada hora tienen que salir tal cual, y nada sensible debe colarse.
 */
class RegistroDeEnviosTest {
    private val encabezado =
        "id_local,capturado_utc,categoria,categoria_ia,confianza_ia,estado,intentos,ultimo_intento_utc," +
            "enviado_utc,segundos_hasta_envio,motivo_ultimo_fallo,codigo_http,red_al_guardar,red_ultimo_intento"

    /** 1_700_000_000_000 ms = 2023-11-14T22:13:20Z (una referencia fácil de comprobar a mano). */
    private val capturado = 1_700_000_000_000L

    private fun reporte(
        id: Long = 1,
        estado: EstadoReporte = EstadoReporte.PENDIENTE,
        fechaHora: Long = capturado,
        categoriaIa: CategoriaResiduo? = null,
        confianzaIa: Float? = null,
        redAlGuardar: TipoDeRed? = null,
        envio: SeguimientoDeEnvio = SeguimientoDeEnvio(),
    ) = Reporte(
        id = id,
        idAnonimo = "uuid-del-dispositivo",
        categoria = CategoriaResiduo.PLASTICO,
        estado = estado,
        latitud = -13.5309,
        longitud = -71.9512,
        fotoPath = "/data/user/0/com.wanchaqlimpio.app/files/reportes/reporte_1.jpg",
        fechaHoraEpochMillis = fechaHora,
        categoriaIa = categoriaIa,
        confianzaIa = confianzaIa,
        redAlGuardar = redAlGuardar,
        envio = envio,
    )

    @Test
    fun `sin reportes solo sale el encabezado`() {
        assertEquals("$encabezado\n", emptyList<Reporte>().aCsvDeEnvios())
    }

    @Test
    fun `un reporte enviado lleva sus horas en UTC y los segundos que tardo en llegar`() {
        val enviado =
            reporte(
                id = 7,
                estado = EstadoReporte.SINCRONIZADO,
                fechaHora = capturado + 500, // los milisegundos no salen: la hora va al segundo
                categoriaIa = CategoriaResiduo.VIDRIO,
                confianzaIa = 0.756f,
                redAlGuardar = TipoDeRed.DATOS_MOVILES,
                envio =
                    SeguimientoDeEnvio(
                        intentos = 3,
                        ultimoIntentoEpochMillis = capturado + 42_100,
                        enviadoEpochMillis = capturado + 42_100,
                        redAlUltimoIntento = TipoDeRed.WIFI,
                    ),
            )

        val filas = listOf(enviado).aCsvDeEnvios().lines()

        assertEquals(encabezado, filas[0])
        assertEquals(
            "7,2023-11-14T22:13:20Z,PLASTICO,VIDRIO,0.76,SINCRONIZADO,3,2023-11-14T22:14:02Z,2023-11-14T22:14:02Z," +
                "41,,,DATOS_MOVILES,WIFI",
            filas[1],
        )
    }

    @Test
    fun `un pendiente con fallo dice el motivo y el codigo HTTP y deja vacio lo que aun no hay`() {
        val pendiente =
            reporte(
                envio =
                    SeguimientoDeEnvio(
                        intentos = 2,
                        ultimoIntentoEpochMillis = capturado + 100_000,
                        ultimoFallo = FalloDeEnvio(MotivoFalloEnvio.SERVIDOR_OCUPADO, codigoHttp = 503),
                        redAlUltimoIntento = TipoDeRed.SIN_INTERNET,
                    ),
            )

        val fila = listOf(pendiente).aCsvDeEnvios().lines()[1]

        assertEquals(
            "1,2023-11-14T22:13:20Z,PLASTICO,,,PENDIENTE,2,2023-11-14T22:15:00Z,,,SERVIDOR_OCUPADO,503,,SIN_INTERNET",
            fila,
        )
    }

    @Test
    fun `un fallo sin respuesta del servidor deja vacio el codigo HTTP`() {
        val fallo = FalloDeEnvio(MotivoFalloEnvio.SIN_CONEXION)
        val sinRed = reporte(envio = SeguimientoDeEnvio(intentos = 1, ultimoFallo = fallo))

        val columnas = listOf(sinRed).aCsvDeEnvios().lines()[1].split(",")

        assertEquals("SIN_CONEXION", columnas[10])
        assertEquals("", columnas[11])
    }

    @Test
    fun `un reporte rechazado se distingue por su estado y por el codigo`() {
        val rechazado =
            reporte(
                estado = EstadoReporte.RECHAZADO,
                envio =
                    SeguimientoDeEnvio(
                        intentos = 1,
                        ultimoFallo = FalloDeEnvio(MotivoFalloEnvio.RECHAZADO_POR_EL_SERVIDOR, codigoHttp = 422),
                    ),
            )

        val columnas = listOf(rechazado).aCsvDeEnvios().lines()[1].split(",")

        assertEquals("RECHAZADO", columnas[5])
        assertEquals("RECHAZADO_POR_EL_SERVIDOR", columnas[10])
        assertEquals("422", columnas[11])
    }

    @Test
    fun `los renglones salen en el orden en que se capturaron`() {
        val desordenados = listOf(reporte(id = 3), reporte(id = 1), reporte(id = 2))

        val renglones =
            desordenados
                .aCsvDeEnvios()
                .lines()
                .drop(1)
                .filter { it.isNotEmpty() }
        val ids = renglones.map { it.substringBefore(",") }

        assertEquals(listOf("1", "2", "3"), ids)
    }

    @Test
    fun `si el reloj del telefono estaba atrasado los segundos salen negativos y no se esconden`() {
        val anomalo =
            reporte(
                estado = EstadoReporte.SINCRONIZADO,
                envio = SeguimientoDeEnvio(intentos = 1, enviadoEpochMillis = capturado - 90_000),
            )

        val columnas = listOf(anomalo).aCsvDeEnvios().lines()[1].split(",")

        assertEquals("-90", columnas[9])
    }

    @Test
    fun `no lleva coordenadas ni la ruta de la foto ni el codigo anonimo`() {
        val csv = listOf(reporte(envio = SeguimientoDeEnvio(intentos = 1))).aCsvDeEnvios(comentarios = listOf("v1"))

        listOf("13.53", "71.95", "uuid-del-dispositivo", "reporte_1.jpg", "/data/").forEach {
            assertFalse("el registro no debe contener «$it»", csv.contains(it))
        }
    }

    @Test
    fun `los comentarios van al principio con almohadilla y sin saltos de linea`() {
        val csv = emptyList<Reporte>().aCsvDeEnvios(comentarios = listOf("Wanchaq Limpio 1.0", "dos\nlineas\r\njuntas"))

        assertEquals(
            listOf("# Wanchaq Limpio 1.0", "# dos lineas juntas", encabezado, ""),
            csv.lines(),
        )
    }

    @Test
    fun `una celda con coma comillas o salto de linea se escapa como manda el CSV`() {
        assertEquals("simple", escaparCsv("simple"))
        assertEquals("\"a,b\"", escaparCsv("a,b"))
        assertEquals("\"dice \"\"hola\"\"\"", escaparCsv("dice \"hola\""))
        assertEquals("\"una\nlinea\"", escaparCsv("una\nlinea"))
    }
}
