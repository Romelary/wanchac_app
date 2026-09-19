package com.wanchaqlimpio.app.ui.misreportes

import app.cash.turbine.test
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.ConsentimientoEstado
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.domain.model.Ubicacion
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class ReporteRepositoryFalso(
    reportesIniciales: List<Reporte> = emptyList(),
) : ReporteRepository {
    private val reportes = MutableStateFlow(reportesIniciales)
    val idsEliminados = mutableListOf<Long>()
    var idAnonimoConsultado: String? = null
        private set

    override suspend fun crearReporte(
        idAnonimo: String,
        categoria: CategoriaResiduo,
        ubicacion: Ubicacion,
        fotoPath: String,
        ahoraEpochMillis: Long,
        sugerenciaIa: SugerenciaIa?,
    ): ResultadoValidacionReporte = ResultadoValidacionReporte.Valido

    override fun observarReportesDe(idAnonimo: String): Flow<List<Reporte>> {
        idAnonimoConsultado = idAnonimo
        return reportes
    }

    override suspend fun eliminarReporte(id: Long) {
        idsEliminados += id
        reportes.value = reportes.value.filterNot { it.id == id }
    }

    var enviosAhora = 0
        private set

    override fun enviarPendientesAhora() {
        enviosAhora++
    }
}

private class ConsentimientoRepositoryFalso(
    idAnonimo: String?,
) : ConsentimientoRepository {
    override val estado: Flow<ConsentimientoEstado> =
        flowOf(ConsentimientoEstado(idAnonimo, fechaConsentimientoEpochMillis = if (idAnonimo != null) 0L else null))

    override suspend fun aceptarConsentimiento() = Unit

    override suspend fun revocarConsentimiento() = Unit
}

private fun reporte(id: Long) =
    Reporte(
        id = id,
        idAnonimo = "device-1",
        categoria = CategoriaResiduo.PLASTICO,
        estado = EstadoReporte.PENDIENTE,
        latitud = -13.530,
        longitud = -71.950,
        fotoPath = "foto-$id.jpg",
        fechaHoraEpochMillis = id,
    )

class MisReportesViewModelTest {
    @Before
    fun configurarDispatcherPrincipal() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restaurarDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

    @Test
    fun `muestra los reportes del dispositivo identificado por su id anonimo`() =
        runTest {
            val repositorio = ReporteRepositoryFalso(listOf(reporte(1), reporte(2)))
            val viewModel = MisReportesViewModel(repositorio, ConsentimientoRepositoryFalso("device-1"))

            viewModel.uiState.test {
                var estado = awaitItem()
                if (estado == MisReportesUiState.Cargando) estado = awaitItem()
                assertEquals(MisReportesUiState.Lista(listOf(reporte(1), reporte(2))), estado)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals("device-1", repositorio.idAnonimoConsultado)
        }

    @Test
    fun `sin consentimiento aceptado no hay reportes que mostrar ni se consulta el repositorio`() =
        runTest {
            val repositorio = ReporteRepositoryFalso(listOf(reporte(1)))
            val viewModel = MisReportesViewModel(repositorio, ConsentimientoRepositoryFalso(idAnonimo = null))

            viewModel.uiState.test {
                var estado = awaitItem()
                if (estado == MisReportesUiState.Cargando) estado = awaitItem()
                assertEquals(MisReportesUiState.Lista(emptyList()), estado)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(null, repositorio.idAnonimoConsultado)
        }

    @Test
    fun `eliminar delega en el repositorio y el reporte desaparece de la lista`() =
        runTest {
            val repositorio = ReporteRepositoryFalso(listOf(reporte(1), reporte(2)))
            val viewModel = MisReportesViewModel(repositorio, ConsentimientoRepositoryFalso("device-1"))

            viewModel.uiState.test {
                var estado = awaitItem()
                if (estado == MisReportesUiState.Cargando) estado = awaitItem()
                assertEquals(2, (estado as MisReportesUiState.Lista).reportes.size)

                viewModel.eliminar(1L)

                val siguiente = awaitItem() as MisReportesUiState.Lista
                assertEquals(listOf(reporte(2)), siguiente.reportes)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf(1L), repositorio.idsEliminados)
        }

    @Test
    fun `enviar ahora pide al repositorio reintentar el envio de los pendientes`() {
        val repositorio = ReporteRepositoryFalso(listOf(reporte(1)))
        val viewModel = MisReportesViewModel(repositorio, ConsentimientoRepositoryFalso("device-1"))

        viewModel.enviarAhora()
        viewModel.enviarAhora()

        assertEquals(2, repositorio.enviosAhora)
    }
}
