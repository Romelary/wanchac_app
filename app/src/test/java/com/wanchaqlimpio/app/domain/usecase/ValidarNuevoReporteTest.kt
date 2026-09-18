package com.wanchaqlimpio.app.domain.usecase

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.Ubicacion
import org.junit.Assert.assertEquals
import org.junit.Test

// Un punto real dentro del bounding box aproximado de Wanchaq (ver
// LimiteDistritalWanchaq) y otro claramente fuera (centro de Lima).
private const val LATITUD_DENTRO = -13.530
private const val LONGITUD_DENTRO = -71.950
private const val LATITUD_FUERA = -12.046
private const val LONGITUD_FUERA = -77.043

class ValidarNuevoReporteTest {
    private val validar = ValidarNuevoReporte()

    private fun reportePrevio(
        latitud: Double = LATITUD_DENTRO,
        longitud: Double = LONGITUD_DENTRO,
        fechaHoraEpochMillis: Long,
    ) = Reporte(
        idAnonimo = "device-1",
        categoria = CategoriaResiduo.MIXTO,
        estado = EstadoReporte.PENDIENTE,
        latitud = latitud,
        longitud = longitud,
        fotoPath = "irrelevante.jpg",
        fechaHoraEpochMillis = fechaHoraEpochMillis,
    )

    @Test
    fun `ubicacion dentro del limite y sin previos es valida`() {
        val ubicacion = Ubicacion(LATITUD_DENTRO, LONGITUD_DENTRO, precisionMetros = 10f)

        val resultado = validar(ubicacion, reportesPreviosDelMismoDispositivo = emptyList(), ahoraEpochMillis = 0L)

        assertEquals(ResultadoValidacionReporte.Valido, resultado)
    }

    @Test
    fun `precision mayor a 15 metros se rechaza`() {
        val ubicacion = Ubicacion(LATITUD_DENTRO, LONGITUD_DENTRO, precisionMetros = 15.1f)

        val resultado = validar(ubicacion, emptyList(), ahoraEpochMillis = 0L)

        assertEquals(ResultadoValidacionReporte.PrecisionInsuficiente(15.1f), resultado)
    }

    @Test
    fun `precision de exactamente 15 metros se acepta`() {
        val ubicacion = Ubicacion(LATITUD_DENTRO, LONGITUD_DENTRO, precisionMetros = 15.0f)

        val resultado = validar(ubicacion, emptyList(), ahoraEpochMillis = 0L)

        assertEquals(ResultadoValidacionReporte.Valido, resultado)
    }

    @Test
    fun `coordenada fuera del distrito de Wanchaq se rechaza`() {
        val ubicacion = Ubicacion(LATITUD_FUERA, LONGITUD_FUERA, precisionMetros = 5f)

        val resultado = validar(ubicacion, emptyList(), ahoraEpochMillis = 0L)

        assertEquals(ResultadoValidacionReporte.FueraDelLimiteDistrital, resultado)
    }

    @Test
    fun `reporte a menos de 20 metros y 30 minutos del mismo dispositivo es duplicado`() {
        val previo = reportePrevio(fechaHoraEpochMillis = 0L)
        // ~11 m al norte del punto previo.
        val ubicacion = Ubicacion(LATITUD_DENTRO + 0.0001, LONGITUD_DENTRO, precisionMetros = 5f)

        val resultado =
            validar(
                ubicacion,
                reportesPreviosDelMismoDispositivo = listOf(previo),
                ahoraEpochMillis = 10 * 60_000L, // 10 minutos despues
            )

        assertEquals(ResultadoValidacionReporte.Duplicado, resultado)
    }

    @Test
    fun `mismo lugar pero despues de 30 minutos no es duplicado`() {
        val previo = reportePrevio(fechaHoraEpochMillis = 0L)
        val ubicacion = Ubicacion(LATITUD_DENTRO, LONGITUD_DENTRO, precisionMetros = 5f)

        val resultado =
            validar(
                ubicacion,
                reportesPreviosDelMismoDispositivo = listOf(previo),
                ahoraEpochMillis = 31 * 60_000L,
            )

        assertEquals(ResultadoValidacionReporte.Valido, resultado)
    }

    @Test
    fun `dentro de la ventana de tiempo pero a mas de 20 metros no es duplicado`() {
        val previo = reportePrevio(fechaHoraEpochMillis = 0L)
        // ~111 m al norte (0.001 grados de latitud) - claramente fuera del radio de 20 m.
        val ubicacion = Ubicacion(LATITUD_DENTRO + 0.001, LONGITUD_DENTRO, precisionMetros = 5f)

        val resultado =
            validar(
                ubicacion,
                reportesPreviosDelMismoDispositivo = listOf(previo),
                ahoraEpochMillis = 10 * 60_000L,
            )

        assertEquals(ResultadoValidacionReporte.Valido, resultado)
    }
}
