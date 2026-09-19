package com.wanchaqlimpio.app.ui.mapa

import app.cash.turbine.test
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.PuntoCritico
import com.wanchaqlimpio.app.domain.repository.MotivoFalloMapa
import com.wanchaqlimpio.app.domain.repository.PuntosCriticosRepository
import com.wanchaqlimpio.app.domain.repository.ResultadoPuntosCriticos
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private fun punto(id: Long) = PuntoCritico(id, -13.53, -71.9475, 5, CategoriaResiduo.PLASTICO, 1_790_000_000_000)

/**
 * Repositorio de mentira: cada llamada espera a que la prueba le entregue su resultado, así se puede observar el
 * estado intermedio (cargando) y el orden en que llegan las respuestas.
 */
private class RepositorioFalso : PuntosCriticosRepository {
    val respuestas = mutableListOf<CompletableDeferred<ResultadoPuntosCriticos>>()

    override suspend fun obtenerConfirmados(): ResultadoPuntosCriticos {
        val respuesta = CompletableDeferred<ResultadoPuntosCriticos>()
        respuestas += respuesta
        return respuesta.await()
    }

    /** Completa la llamada número [indice] (0 = la primera). */
    fun responder(
        indice: Int,
        resultado: ResultadoPuntosCriticos,
    ) {
        respuestas[indice].complete(resultado)
    }
}

class MapaViewModelTest {
    private lateinit var repositorio: RepositorioFalso
    private lateinit var viewModel: MapaViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repositorio = RepositorioFalso()
        viewModel = MapaViewModel(repositorio)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    @Test
    fun `antes de recargar el estado es Cargando y no se ha pedido nada`() {
        assertEquals(MapaUiState.Cargando, viewModel.uiState.value)
        assertEquals(0, repositorio.respuestas.size)
    }

    @Test
    fun `con focos confirmados el estado es ConPuntos`() {
        viewModel.recargar()
        assertEquals(MapaUiState.Cargando, viewModel.uiState.value) // mientras espera la respuesta
        repositorio.responder(0, ResultadoPuntosCriticos.Exito(listOf(punto(1), punto(2))))

        assertEquals(MapaUiState.ConPuntos(listOf(punto(1), punto(2))), viewModel.uiState.value)
    }

    @Test
    fun `sin ningun foco confirmado el estado es SinPuntos, no un error`() {
        viewModel.recargar()
        repositorio.responder(0, ResultadoPuntosCriticos.Exito(emptyList()))

        assertEquals(MapaUiState.SinPuntos, viewModel.uiState.value)
    }

    @Test
    fun `cada motivo de fallo llega a la pantalla`() {
        MotivoFalloMapa.entries.forEachIndexed { indice, motivo ->
            viewModel.recargar()
            repositorio.responder(indice, ResultadoPuntosCriticos.Fallo(motivo))

            assertEquals(MapaUiState.Error(motivo), viewModel.uiState.value)
        }
    }

    @Test
    fun `tras un error, reintentar vuelve a Cargando y luego al resultado`() =
        runTest {
            viewModel.recargar()
            repositorio.responder(0, ResultadoPuntosCriticos.Fallo(MotivoFalloMapa.SIN_CONEXION))

            viewModel.uiState.test {
                assertEquals(MapaUiState.Error(MotivoFalloMapa.SIN_CONEXION), awaitItem())
                viewModel.recargar()
                assertEquals(MapaUiState.Cargando, awaitItem())
                repositorio.responder(1, ResultadoPuntosCriticos.Exito(listOf(punto(9))))
                assertEquals(MapaUiState.ConPuntos(listOf(punto(9))), awaitItem())
            }
        }

    @Test
    fun `recargar con focos ya en pantalla los conserva mientras espera (no parpadea)`() {
        viewModel.recargar()
        repositorio.responder(0, ResultadoPuntosCriticos.Exito(listOf(punto(1))))
        val enPantalla = viewModel.uiState.value

        viewModel.recargar() // segunda petición, aún sin respuesta

        assertEquals(enPantalla, viewModel.uiState.value)
        repositorio.responder(1, ResultadoPuntosCriticos.Exito(listOf(punto(1), punto(2))))
        assertEquals(MapaUiState.ConPuntos(listOf(punto(1), punto(2))), viewModel.uiState.value)
    }

    @Test
    fun `una recarga nueva anula la anterior y una respuesta lenta no pisa a la mas reciente`() {
        viewModel.recargar() // 0: lenta
        viewModel.recargar() // 1: rápida (anula la 0)
        repositorio.responder(1, ResultadoPuntosCriticos.Exito(listOf(punto(2))))
        repositorio.responder(0, ResultadoPuntosCriticos.Exito(listOf(punto(1)))) // llega tarde

        assertEquals(MapaUiState.ConPuntos(listOf(punto(2))), viewModel.uiState.value)
    }
}
