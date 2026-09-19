package com.wanchaqlimpio.app.domain.usecase

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.ConsentimientoEstado
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.domain.model.Ubicacion
import com.wanchaqlimpio.app.domain.repository.AlmacenFotos
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// Todos los fakes anotan en la misma lista lo que se les pide, para poder verificar el ORDEN.
private class ReporteRepositoryFalso(
    private val eventos: MutableList<String>,
    private val reportes: List<Reporte>,
    private val fallaAlEliminar: Long? = null,
) : ReporteRepository {
    override suspend fun crearReporte(
        idAnonimo: String,
        categoria: CategoriaResiduo,
        ubicacion: Ubicacion,
        fotoPath: String,
        ahoraEpochMillis: Long,
        sugerenciaIa: SugerenciaIa?,
    ): ResultadoValidacionReporte = ResultadoValidacionReporte.Valido

    override fun observarReportesDe(idAnonimo: String): Flow<List<Reporte>> {
        eventos += "listar:$idAnonimo"
        return flowOf(reportes)
    }

    override suspend fun eliminarReporte(id: Long) {
        check(id != fallaAlEliminar) { "fallo simulado al eliminar el reporte $id" }
        eventos += "eliminar:$id"
    }

    override fun enviarPendientesAhora() = Unit
}

private class ConsentimientoRepositoryFalso(
    idAnonimo: String?,
    private val eventos: MutableList<String>,
) : ConsentimientoRepository {
    override val estado: Flow<ConsentimientoEstado> =
        flowOf(ConsentimientoEstado(idAnonimo, fechaConsentimientoEpochMillis = if (idAnonimo != null) 0L else null))

    override suspend fun aceptarConsentimiento() = Unit

    override suspend fun revocarConsentimiento() {
        eventos += "revocar"
    }
}

private class AlmacenFotosFalso(
    private val eventos: MutableList<String>,
) : AlmacenFotos {
    override fun nuevaFoto(): File = File("nueva.jpg")

    override suspend fun borrar(fotoPath: String) = Unit

    override suspend fun borrarTodas() {
        eventos += "borrarTodasLasFotos"
    }

    override suspend fun borrarHuerfanas(
        conservar: Set<String>,
        modificadasAntesDeEpochMillis: Long,
    ) = Unit
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

class BorrarTodosMisDatosTest {
    private val eventos = mutableListOf<String>()

    private fun crearCasoDeUso(
        idAnonimo: String? = "device-1",
        reportes: List<Reporte> = listOf(reporte(1), reporte(2)),
        fallaAlEliminar: Long? = null,
    ) = BorrarTodosMisDatos(
        reporteRepository = ReporteRepositoryFalso(eventos, reportes, fallaAlEliminar),
        consentimientoRepository = ConsentimientoRepositoryFalso(idAnonimo, eventos),
        almacenFotos = AlmacenFotosFalso(eventos),
    )

    @Test
    fun `elimina cada reporte, limpia las fotos y revoca el consentimiento al final`() =
        runTest {
            crearCasoDeUso()()

            assertEquals(
                listOf("listar:device-1", "eliminar:1", "eliminar:2", "borrarTodasLasFotos", "revocar"),
                eventos,
            )
        }

    @Test
    fun `sin id anonimo no lista reportes pero igual limpia fotos y revoca`() =
        runTest {
            crearCasoDeUso(idAnonimo = null)()

            assertEquals(listOf("borrarTodasLasFotos", "revocar"), eventos)
        }

    @Test
    fun `si eliminar un reporte falla no se revoca el consentimiento para poder reintentar`() =
        runTest {
            val resultado = runCatching { crearCasoDeUso(fallaAlEliminar = 2L)() }

            assertTrue("el fallo debe propagarse, no silenciarse", resultado.isFailure)
            // Sin revocar, el id_anonimo sigue existiendo y el usuario puede volver a intentarlo.
            assertFalse("revocar" in eventos)
        }
}
