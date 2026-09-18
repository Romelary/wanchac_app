package com.wanchaqlimpio.app.ui.ajustes

import app.cash.turbine.test
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.ConsentimientoEstado
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.domain.model.Ubicacion
import com.wanchaqlimpio.app.domain.repository.AlmacenFotos
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import com.wanchaqlimpio.app.domain.usecase.BorrarTodosMisDatos
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

private class ReporteRepositoryFalso : ReporteRepository {
    override suspend fun crearReporte(
        idAnonimo: String,
        categoria: CategoriaResiduo,
        ubicacion: Ubicacion,
        fotoPath: String,
        ahoraEpochMillis: Long,
        sugerenciaIa: SugerenciaIa?,
    ): ResultadoValidacionReporte = ResultadoValidacionReporte.Valido

    override fun observarReportesDe(idAnonimo: String): Flow<List<Reporte>> = flowOf(emptyList())

    override suspend fun eliminarReporte(id: Long) = Unit
}

/** [compuerta] permite mantener "en curso" el borrado hasta que la prueba decida terminarlo. */
private class ConsentimientoRepositoryFalso(
    idAnonimo: String?,
    private val compuerta: CompletableDeferred<Unit>? = null,
) : ConsentimientoRepository {
    var revocaciones = 0
        private set

    override val estado: Flow<ConsentimientoEstado> =
        flowOf(ConsentimientoEstado(idAnonimo, fechaConsentimientoEpochMillis = if (idAnonimo != null) 0L else null))

    override suspend fun aceptarConsentimiento() = Unit

    override suspend fun revocarConsentimiento() {
        compuerta?.await()
        revocaciones++
    }
}

private class AlmacenFotosFalso : AlmacenFotos {
    override fun nuevaFoto(): File = File("nueva.jpg")

    override suspend fun borrar(fotoPath: String) = Unit

    override suspend fun borrarTodas() = Unit

    override suspend fun borrarHuerfanas(
        conservar: Set<String>,
        modificadasAntesDeEpochMillis: Long,
    ) = Unit
}

class AjustesViewModelTest {
    @Before
    fun configurarDispatcherPrincipal() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restaurarDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(consentimiento: ConsentimientoRepositoryFalso) =
        AjustesViewModel(
            consentimientoRepository = consentimiento,
            borrarTodosMisDatos = BorrarTodosMisDatos(ReporteRepositoryFalso(), consentimiento, AlmacenFotosFalso()),
        )

    @Test
    fun `muestra el codigo anonimo del consentimiento`() =
        runTest {
            val viewModel = crearViewModel(ConsentimientoRepositoryFalso("device-1"))

            viewModel.uiState.test {
                var estado = awaitItem()
                while (estado.idAnonimo == null) estado = awaitItem()
                assertEquals("device-1", estado.idAnonimo)
                assertFalse(estado.borrando)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `borrarTodo ejecuta el borrado completo y no queda en estado borrando`() =
        runTest {
            val consentimiento = ConsentimientoRepositoryFalso("device-1")
            val viewModel = crearViewModel(consentimiento)

            viewModel.uiState.test {
                var estado = awaitItem()
                while (estado.idAnonimo == null) estado = awaitItem()

                viewModel.borrarTodo()

                assertEquals(1, consentimiento.revocaciones)
                // Se lee .value (con un colector activo refleja el último estado) en vez de
                // depender de qué emisiones intermedias alcance a ver Turbine (StateFlow conflaciona).
                assertFalse(viewModel.uiState.value.borrando)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `mientras borra queda en estado borrando, un segundo toque se ignora y al terminar vuelve a false`() =
        runTest {
            val compuerta = CompletableDeferred<Unit>()
            val consentimiento = ConsentimientoRepositoryFalso("device-1", compuerta)
            val viewModel = crearViewModel(consentimiento)

            viewModel.uiState.test {
                var estado = awaitItem()
                while (estado.idAnonimo == null) estado = awaitItem()
                assertFalse(estado.borrando)

                viewModel.borrarTodo()
                viewModel.borrarTodo() // segundo toque con el primero aún en curso
                assertTrue(awaitItem().borrando)

                compuerta.complete(Unit)
                assertFalse(awaitItem().borrando)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals("solo debe ejecutarse un borrado", 1, consentimiento.revocaciones)
        }
}
