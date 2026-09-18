package com.wanchaqlimpio.app.ui.reporte

import com.wanchaqlimpio.app.core.location.ProveedorUbicacion
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.ConsentimientoEstado
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.Ubicacion
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val ID_ANONIMO = "device-1"

private class ReporteRepositoryFalso(
    private val resultado: ResultadoValidacionReporte,
) : ReporteRepository {
    override suspend fun crearReporte(
        idAnonimo: String,
        categoria: CategoriaResiduo,
        ubicacion: Ubicacion,
        fotoPath: String,
        ahoraEpochMillis: Long,
    ): ResultadoValidacionReporte = resultado

    override fun observarReportesDe(idAnonimo: String): Flow<List<Reporte>> = flowOf(emptyList())

    override suspend fun eliminarReporte(id: Long) = Unit
}

private class ConsentimientoRepositoryFalso(
    private val idAnonimo: String?,
) : ConsentimientoRepository {
    override val estado: Flow<ConsentimientoEstado> =
        flowOf(ConsentimientoEstado(idAnonimo, fechaConsentimientoEpochMillis = if (idAnonimo != null) 0L else null))

    override suspend fun aceptarConsentimiento() = Unit

    override suspend fun revocarConsentimiento() = Unit
}

private class ProveedorUbicacionFalso(
    private val ubicacion: Ubicacion?,
) : ProveedorUbicacion {
    override suspend fun obtenerUbicacionActual(): Ubicacion? = ubicacion
}

class ReporteViewModelTest {
    // viewModelScope usa Dispatchers.Main; en un test JVM puro no existe un
    // Main real, así que se reemplaza por uno de prueba (ver kotlinx-coroutines-test).
    @Before
    fun configurarDispatcherPrincipal() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restaurarDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial es Capturando`() {
        val viewModel = crearViewModel()

        assertEquals(ReporteUiState.Capturando, viewModel.uiState.value)
    }

    @Test
    fun `al capturar una foto valida el estado termina en Guardado`() =
        runTest {
            val viewModel =
                crearViewModel(
                    resultado = ResultadoValidacionReporte.Valido,
                    ubicacion = Ubicacion(-13.530, -71.950, precisionMetros = 5f),
                )

            viewModel.onFotoCapturada("foto.jpg")

            assertEquals(ReporteUiState.Guardado, viewModel.uiState.value)
        }

    @Test
    fun `GPS impreciso termina en Rechazado y reintentar vuelve a Capturando`() =
        runTest {
            val viewModel =
                crearViewModel(
                    resultado = ResultadoValidacionReporte.PrecisionInsuficiente(30f),
                    ubicacion = Ubicacion(-13.530, -71.950, precisionMetros = 30f),
                )

            viewModel.onFotoCapturada("foto.jpg")
            val estadoRechazado = viewModel.uiState.value
            assertTrue(estadoRechazado is ReporteUiState.Rechazado)

            viewModel.reintentar()

            assertEquals(ReporteUiState.Capturando, viewModel.uiState.value)
        }

    @Test
    fun `sin senal de GPS termina en Rechazado sin llegar a validar en el repositorio`() =
        runTest {
            val viewModel = crearViewModel(resultado = ResultadoValidacionReporte.Valido, ubicacion = null)

            viewModel.onFotoCapturada("foto.jpg")

            assertTrue(viewModel.uiState.value is ReporteUiState.Rechazado)
        }

    @Test
    fun `sin consentimiento aceptado no se intenta crear el reporte`() =
        runTest {
            val viewModel =
                crearViewModel(
                    resultado = ResultadoValidacionReporte.Valido,
                    ubicacion = Ubicacion(-13.530, -71.950, precisionMetros = 5f),
                    idAnonimo = null,
                )

            viewModel.onFotoCapturada("foto.jpg")

            assertTrue(viewModel.uiState.value is ReporteUiState.Rechazado)
        }

    private fun crearViewModel(
        resultado: ResultadoValidacionReporte = ResultadoValidacionReporte.Valido,
        ubicacion: Ubicacion? = Ubicacion(-13.530, -71.950, precisionMetros = 5f),
        idAnonimo: String? = ID_ANONIMO,
    ) = ReporteViewModel(
        reporteRepository = ReporteRepositoryFalso(resultado),
        consentimientoRepository = ConsentimientoRepositoryFalso(idAnonimo),
        proveedorUbicacion = ProveedorUbicacionFalso(ubicacion),
    )
}
