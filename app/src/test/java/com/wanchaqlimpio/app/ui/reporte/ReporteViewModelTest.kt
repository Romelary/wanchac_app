package com.wanchaqlimpio.app.ui.reporte

import com.wanchaqlimpio.app.core.location.ProveedorUbicacion
import com.wanchaqlimpio.app.core.location.RastreadorDeUbicacion
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.ConsentimientoEstado
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.domain.model.Ubicacion
import com.wanchaqlimpio.app.domain.repository.AlmacenFotos
import com.wanchaqlimpio.app.domain.repository.ClasificadorResiduo
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReductorDeFotos
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.time.TestTimeSource

private const val ID_ANONIMO = "device-1"
private const val FOTO = "foto.jpg"

private val SUGERENCIA_SEGURA =
    SugerenciaIa(CategoriaResiduo.PLASTICO, confianza = 0.92f, propuesta = CategoriaResiduo.PLASTICO)

// El modelo cree que es cartón, pero con poca confianza: la app propone MIXTO y conserva lo que el modelo dijo.
private val SUGERENCIA_DUDOSA =
    SugerenciaIa(CategoriaResiduo.CARTON, confianza = 0.41f, propuesta = CategoriaResiduo.MIXTO)

private class ReporteRepositoryFalso(
    private val resultado: ResultadoValidacionReporte,
    private val reportesExistentes: List<Reporte> = emptyList(),
) : ReporteRepository {
    /** Todo lo que se le pidió guardar, para verificar categoría final y sugerencia de la IA. */
    val creados = mutableListOf<Triple<CategoriaResiduo, SugerenciaIa?, String>>()

    /** La ubicación con la que se intentó guardar cada reporte. */
    val ubicaciones = mutableListOf<Ubicacion>()

    override suspend fun crearReporte(
        idAnonimo: String,
        categoria: CategoriaResiduo,
        ubicacion: Ubicacion,
        fotoPath: String,
        ahoraEpochMillis: Long,
        sugerenciaIa: SugerenciaIa?,
    ): ResultadoValidacionReporte {
        creados += Triple(categoria, sugerenciaIa, fotoPath)
        ubicaciones += ubicacion
        return resultado
    }

    override fun observarReportesDe(idAnonimo: String): Flow<List<Reporte>> = flowOf(reportesExistentes)

    override suspend fun eliminarReporte(id: Long) = Unit

    override fun enviarPendientesAhora() = Unit

    override suspend fun reintentarEnvio(id: Long) = Unit
}

private class ConsentimientoRepositoryFalso(
    private val idAnonimo: String?,
) : ConsentimientoRepository {
    override val estado: Flow<ConsentimientoEstado> =
        flowOf(ConsentimientoEstado(idAnonimo, fechaConsentimientoEpochMillis = if (idAnonimo != null) 0L else null))

    override suspend fun aceptarConsentimiento() = Unit

    override suspend fun revocarConsentimiento() = Unit
}

/** Cada vez que se le piden lecturas entrega el siguiente guion; el último se repite (y sin guion: sin lecturas). */
private class ProveedorUbicacionFalso(
    private val guiones: List<Flow<Ubicacion>>,
) : ProveedorUbicacion {
    constructor(ubicacion: Ubicacion?) : this(listOf(if (ubicacion == null) emptyFlow() else flowOf(ubicacion)))

    /** Cuántas veces se le pidieron lecturas. */
    var pedidas = 0
        private set

    override fun lecturas(): Flow<Ubicacion> = guiones.getOrElse(pedidas++) { guiones.lastOrNull() ?: emptyFlow() }
}

private class ReductorDeFotosFalso(
    private val eventos: MutableList<String>? = null,
) : ReductorDeFotos {
    val reducidas = mutableListOf<String>()

    override suspend fun reducir(fotoPath: String): Boolean {
        reducidas += fotoPath
        eventos?.add("reducir:$fotoPath")
        return true
    }
}

private class AlmacenFotosFalso : AlmacenFotos {
    val borradas = mutableListOf<String>()
    var limpiezasDeHuerfanas = mutableListOf<Pair<Set<String>, Long>>()

    override fun nuevaFoto(): File = File("nueva.jpg")

    override suspend fun borrar(fotoPath: String) {
        borradas += fotoPath
    }

    override suspend fun borrarTodas() = Unit

    override suspend fun borrarHuerfanas(
        conservar: Set<String>,
        modificadasAntesDeEpochMillis: Long,
    ) {
        limpiezasDeHuerfanas += conservar to modificadasAntesDeEpochMillis
    }
}

/** [compuerta] mantiene "analizando" al modelo hasta que la prueba decida terminarlo. */
private class ClasificadorFalso(
    private val sugerencia: SugerenciaIa?,
    private val compuerta: CompletableDeferred<Unit>? = null,
    private val eventos: MutableList<String>? = null,
) : ClasificadorResiduo {
    val fotosClasificadas = mutableListOf<String>()

    override suspend fun clasificar(fotoPath: String): SugerenciaIa? {
        fotosClasificadas += fotoPath
        eventos?.add("clasificar:$fotoPath")
        compuerta?.await()
        return sugerencia
    }
}

private fun lectura(precisionMetros: Float) = Ubicacion(-13.530, -71.950, precisionMetros)

class ReporteViewModelTest {
    private val almacenFotos = AlmacenFotosFalso()

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
        assertEquals(ReporteUiState.Capturando, crearViewModel().uiState.value)
    }

    // --- Sugerencia de la IA y confirmación -------------------------------------------------------------

    @Test
    fun `al capturar la foto se analiza y se pide confirmar con la categoria que propone el modelo`() =
        runTest {
            val clasificador = ClasificadorFalso(SUGERENCIA_SEGURA)
            val viewModel = crearViewModel(clasificador = clasificador)

            viewModel.onFotoCapturada(FOTO)

            assertEquals(listOf(FOTO), clasificador.fotosClasificadas)
            assertEquals(
                ReporteUiState.Confirmando(FOTO, SUGERENCIA_SEGURA, seleccionada = CategoriaResiduo.PLASTICO),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `mientras el modelo analiza el estado es Clasificando`() =
        runTest {
            val compuerta = CompletableDeferred<Unit>()
            val viewModel = crearViewModel(clasificador = ClasificadorFalso(SUGERENCIA_SEGURA, compuerta))

            viewModel.onFotoCapturada(FOTO)
            assertEquals(ReporteUiState.Clasificando, viewModel.uiState.value)

            compuerta.complete(Unit)
            assertTrue(viewModel.uiState.value is ReporteUiState.Confirmando)
        }

    @Test
    fun `si el modelo duda se preselecciona MIXTO pero se conserva lo que el modelo dijo`() =
        runTest {
            val viewModel = crearViewModel(clasificador = ClasificadorFalso(SUGERENCIA_DUDOSA))

            viewModel.onFotoCapturada(FOTO)

            val estado = viewModel.uiState.value as ReporteUiState.Confirmando
            assertEquals(CategoriaResiduo.MIXTO, estado.seleccionada)
            assertEquals(SUGERENCIA_DUDOSA, estado.sugerencia)
        }

    @Test
    fun `si el modelo no esta disponible el reporte sigue funcionando con eleccion manual`() =
        runTest {
            val viewModel = crearViewModel(clasificador = ClasificadorFalso(sugerencia = null))

            viewModel.onFotoCapturada(FOTO)

            val estado = viewModel.uiState.value as ReporteUiState.Confirmando
            assertNull(estado.sugerencia)
            assertEquals(CategoriaResiduo.MIXTO, estado.seleccionada)
        }

    @Test
    fun `el ciudadano puede corregir la categoria sugerida`() =
        runTest {
            val viewModel = crearViewModel(clasificador = ClasificadorFalso(SUGERENCIA_SEGURA))
            viewModel.onFotoCapturada(FOTO)

            viewModel.seleccionarCategoria(CategoriaResiduo.ESCOMBROS)

            val estado = viewModel.uiState.value as ReporteUiState.Confirmando
            assertEquals(CategoriaResiduo.ESCOMBROS, estado.seleccionada)
        }

    @Test
    fun `seleccionar una categoria fuera de la confirmacion no cambia nada`() {
        val viewModel = crearViewModel()

        viewModel.seleccionarCategoria(CategoriaResiduo.VIDRIO)

        assertEquals(ReporteUiState.Capturando, viewModel.uiState.value)
    }

    @Test
    fun `confirmar guarda la categoria del ciudadano y la sugerencia cruda del modelo por separado`() =
        runTest {
            val repositorio = ReporteRepositoryFalso(ResultadoValidacionReporte.Valido)
            val clasificador = ClasificadorFalso(SUGERENCIA_SEGURA)
            val viewModel = crearViewModel(repositorio = repositorio, clasificador = clasificador)
            viewModel.onFotoCapturada(FOTO)
            viewModel.seleccionarCategoria(CategoriaResiduo.VIDRIO) // el ciudadano corrige al modelo

            viewModel.confirmar()

            assertEquals(ReporteUiState.Guardado, viewModel.uiState.value)
            // categoria = la del ciudadano; sugerencia = lo que dijo el modelo (para medir su exactitud, Anexo 2).
            val esperado = Triple(CategoriaResiduo.VIDRIO, SUGERENCIA_SEGURA as SugerenciaIa?, FOTO)
            assertEquals(listOf(esperado), repositorio.creados)
        }

    @Test
    fun `sin modelo el reporte se guarda sin sugerencia`() =
        runTest {
            val repositorio = ReporteRepositoryFalso(ResultadoValidacionReporte.Valido)
            val clasificador = ClasificadorFalso(sugerencia = null)
            val viewModel = crearViewModel(repositorio = repositorio, clasificador = clasificador)
            viewModel.onFotoCapturada(FOTO)
            viewModel.seleccionarCategoria(CategoriaResiduo.ORGANICO)

            viewModel.confirmar()

            assertEquals(listOf(Triple(CategoriaResiduo.ORGANICO, null as SugerenciaIa?, FOTO)), repositorio.creados)
        }

    @Test
    fun `confirmar antes de tomar una foto no hace nada`() {
        val repositorio = ReporteRepositoryFalso(ResultadoValidacionReporte.Valido)
        val viewModel = crearViewModel(repositorio = repositorio)

        viewModel.confirmar()

        assertEquals(ReporteUiState.Capturando, viewModel.uiState.value)
        assertTrue(repositorio.creados.isEmpty())
    }

    @Test
    fun `un doble toque en guardar crea un solo reporte`() =
        runTest {
            val repositorio = ReporteRepositoryFalso(ResultadoValidacionReporte.Valido)
            val clasificador = ClasificadorFalso(SUGERENCIA_SEGURA)
            val viewModel = crearViewModel(repositorio = repositorio, clasificador = clasificador)
            viewModel.onFotoCapturada(FOTO)

            viewModel.confirmar()
            viewModel.confirmar()

            assertEquals(1, repositorio.creados.size)
        }

    @Test
    fun `descartar la foto la borra del almacenamiento y vuelve a Capturando`() =
        runTest {
            val viewModel = crearViewModel(clasificador = ClasificadorFalso(SUGERENCIA_SEGURA))
            viewModel.onFotoCapturada(FOTO)

            viewModel.descartarFoto()

            assertEquals(ReporteUiState.Capturando, viewModel.uiState.value)
            assertEquals(listOf(FOTO), almacenFotos.borradas)
        }

    // --- Resultado del guardado (igual que antes, ahora tras confirmar) ---------------------------------

    @Test
    fun `un GPS impreciso conserva la foto y deja reintentar la ubicacion sin repetir todo`() =
        runTest {
            val repositorio = ReporteRepositoryFalso(ResultadoValidacionReporte.PrecisionInsuficiente(30f))
            val viewModel = crearViewModel(repositorio = repositorio, ubicacion = lectura(30f))
            viewModel.onFotoCapturada(FOTO)

            viewModel.confirmar()

            val estado = viewModel.uiState.value as ReporteUiState.GpsImpreciso
            assertEquals(30f, estado.precisionMetros)
            assertEquals(FOTO, estado.confirmacion.fotoPath)
            assertEquals("la foto no se borra", emptyList<String>(), almacenFotos.borradas)

            viewModel.reintentarUbicacion()

            assertEquals("se reintentó con la MISMA foto", listOf(FOTO, FOTO), repositorio.creados.map { it.third })
        }

    @Test
    fun `sin senal de GPS se avisa, se conserva la foto y no se llega a validar en el repositorio`() =
        runTest {
            val repositorio = ReporteRepositoryFalso(ResultadoValidacionReporte.Valido)
            val viewModel = crearViewModel(repositorio = repositorio, ubicacion = null)
            viewModel.onFotoCapturada(FOTO)

            viewModel.confirmar()

            val estado = viewModel.uiState.value as ReporteUiState.GpsImpreciso
            assertNull("sin lecturas no hay precisión que mostrar", estado.precisionMetros)
            assertTrue(repositorio.creados.isEmpty())
            assertEquals(emptyList<String>(), almacenFotos.borradas)
        }

    @Test
    fun `tras un GPS impreciso, tomar otra foto borra la actual y vuelve a Capturando`() =
        runTest {
            val viewModel = crearViewModel(ubicacion = null)
            viewModel.onFotoCapturada(FOTO)
            viewModel.confirmar()

            viewModel.descartarFoto()

            assertEquals(ReporteUiState.Capturando, viewModel.uiState.value)
            assertEquals(listOf(FOTO), almacenFotos.borradas)
        }

    @Test
    fun `reintentar la ubicacion fuera de un GPS impreciso no hace nada`() {
        val viewModel = crearViewModel()

        viewModel.reintentarUbicacion()

        assertEquals(ReporteUiState.Capturando, viewModel.uiState.value)
    }

    // --- GPS: esperar una buena lectura en vez de rendirse a la primera --------------------------------------

    @Test
    fun `la lectura del GPS de la foto viaja con la confirmacion y se usa al guardar sin esperar`() =
        runTest {
            val proveedor = ProveedorUbicacionFalso(listOf(flowOf(lectura(8f))))
            val repositorio = ReporteRepositoryFalso(ResultadoValidacionReporte.Valido)
            val viewModel = crearViewModel(repositorio = repositorio, proveedor = proveedor)
            viewModel.vigilarGps() // la cámara estuvo abierta: el GPS ya tenía una lectura buena
            viewModel.onFotoCapturada(FOTO)

            assertEquals(lectura(8f), (viewModel.uiState.value as ReporteUiState.Confirmando).ubicacionAlCapturar)
            viewModel.confirmar()

            assertEquals(listOf(lectura(8f)), repositorio.ubicaciones)
            assertEquals("no se volvió a pedir el GPS", 1, proveedor.pedidas)
        }

    @Test
    fun `si la lectura de la foto es imprecisa se espera una buena antes de guardar`() =
        runTest {
            val proveedor = ProveedorUbicacionFalso(listOf(flowOf(lectura(40f)), flowOf(lectura(9f))))
            val repositorio = ReporteRepositoryFalso(ResultadoValidacionReporte.Valido)
            val viewModel = crearViewModel(repositorio = repositorio, proveedor = proveedor)
            viewModel.vigilarGps() // en la cámara solo hubo una lectura de 40 m
            viewModel.onFotoCapturada(FOTO)

            viewModel.confirmar()

            assertEquals("se guardó con la buena, no con la de la foto", listOf(lectura(9f)), repositorio.ubicaciones)
            assertEquals(ReporteUiState.Guardado, viewModel.uiState.value)
        }

    @Test
    fun `mientras se espera al GPS se ve la mejor precision conseguida`() =
        runTest {
            val compuerta = CompletableDeferred<Unit>()
            val guion =
                flow {
                    emit(lectura(40f))
                    compuerta.await() // el GPS sigue buscando...
                    emit(lectura(9f))
                }
            val viewModel = crearViewModel(proveedor = ProveedorUbicacionFalso(listOf(guion)))
            viewModel.onFotoCapturada(FOTO)

            viewModel.confirmar()
            assertEquals(ReporteUiState.ProcesandoUbicacion(precisionActualMetros = 40f), viewModel.uiState.value)

            compuerta.complete(Unit)
            assertEquals(ReporteUiState.Guardado, viewModel.uiState.value)
        }

    @Test
    fun `la precision del GPS con la camara abierta se expone a la pantalla`() =
        runTest {
            val viewModel = crearViewModel(proveedor = ProveedorUbicacionFalso(listOf(flowOf(lectura(12f)))))
            assertNull("antes de la primera lectura no hay nada que mostrar", viewModel.precisionGps.value)

            viewModel.vigilarGps()

            assertEquals(12f, viewModel.precisionGps.value)
        }

    // --- Fallos de la cámara y de la foto ------------------------------------------------------------------

    @Test
    fun `al tomar la foto se reduce antes de analizarla`() =
        runTest {
            val eventos = mutableListOf<String>()
            val viewModel =
                crearViewModel(
                    clasificador = ClasificadorFalso(SUGERENCIA_SEGURA, eventos = eventos),
                    reductor = ReductorDeFotosFalso(eventos),
                )

            viewModel.onFotoCapturada(FOTO)

            assertEquals(listOf("reducir:$FOTO", "clasificar:$FOTO"), eventos)
        }

    @Test
    fun `un error de la camara se le dice al ciudadano y reintentar vuelve a la camara`() {
        val viewModel = crearViewModel()

        viewModel.onErrorDeCaptura()

        assertTrue(viewModel.uiState.value is ReporteUiState.Rechazado)
        viewModel.reintentar()
        assertEquals(ReporteUiState.Capturando, viewModel.uiState.value)
    }

    @Test
    fun `sin consentimiento aceptado no se intenta crear el reporte`() =
        runTest {
            val repositorio = ReporteRepositoryFalso(ResultadoValidacionReporte.Valido)
            val viewModel = crearViewModel(repositorio = repositorio, idAnonimo = null)
            viewModel.onFotoCapturada(FOTO)

            viewModel.confirmar()

            assertTrue(viewModel.uiState.value is ReporteUiState.Rechazado)
            assertTrue(repositorio.creados.isEmpty())
        }

    // --- Privacidad: ninguna foto debe quedar huérfana --------------------------------------------------

    @Test
    fun `un reporte guardado conserva su foto`() =
        runTest {
            val viewModel = crearViewModel(resultado = ResultadoValidacionReporte.Valido)
            viewModel.onFotoCapturada(FOTO)

            viewModel.confirmar()

            assertEquals(ReporteUiState.Guardado, viewModel.uiState.value)
            assertEquals(emptyList<String>(), almacenFotos.borradas)
        }

    @Test
    fun `si el reporte es rechazado por la validacion su foto se borra del almacenamiento`() =
        runTest {
            val viewModel = crearViewModel(resultado = ResultadoValidacionReporte.FueraDelLimiteDistrital)
            viewModel.onFotoCapturada(FOTO)

            viewModel.confirmar()

            assertTrue(viewModel.uiState.value is ReporteUiState.Rechazado)
            assertEquals(listOf(FOTO), almacenFotos.borradas)
        }

    @Test
    fun `si falta el consentimiento la foto tambien se borra`() =
        runTest {
            val viewModel = crearViewModel(idAnonimo = null)
            viewModel.onFotoCapturada(FOTO)

            viewModel.confirmar()

            assertEquals(listOf(FOTO), almacenFotos.borradas)
        }

    @Test
    fun `al iniciar se limpian las fotos huerfanas conservando las de los reportes existentes`() =
        runTest {
            val antes = System.currentTimeMillis()
            val existentes = listOf(reporteConFoto("a.jpg"), reporteConFoto("b.jpg"))

            crearViewModel(reportesExistentes = existentes)

            val (conservadas, limite) = almacenFotos.limpiezasDeHuerfanas.single()
            assertEquals(setOf("a.jpg", "b.jpg"), conservadas)
            // El límite de tiempo evita borrar una foto tomada mientras la limpieza corre.
            assertTrue(limite >= antes)
        }

    @Test
    fun `sin consentimiento no se limpia nada al iniciar`() {
        crearViewModel(idAnonimo = null)

        assertTrue(almacenFotos.limpiezasDeHuerfanas.isEmpty())
    }

    private fun reporteConFoto(fotoPath: String) =
        Reporte(
            idAnonimo = ID_ANONIMO,
            categoria = CategoriaResiduo.PLASTICO,
            estado = EstadoReporte.PENDIENTE,
            latitud = -13.530,
            longitud = -71.950,
            fotoPath = fotoPath,
            fechaHoraEpochMillis = 1L,
        )

    private fun crearViewModel(
        resultado: ResultadoValidacionReporte = ResultadoValidacionReporte.Valido,
        ubicacion: Ubicacion? = Ubicacion(-13.530, -71.950, precisionMetros = 5f),
        idAnonimo: String? = ID_ANONIMO,
        clasificador: ClasificadorResiduo = ClasificadorFalso(SUGERENCIA_SEGURA),
        repositorio: ReporteRepositoryFalso = ReporteRepositoryFalso(resultado),
        reportesExistentes: List<Reporte> = emptyList(),
        proveedor: ProveedorUbicacionFalso = ProveedorUbicacionFalso(ubicacion),
        reductor: ReductorDeFotos = ReductorDeFotosFalso(),
    ) = ReporteViewModel(
        reporteRepository =
            if (reportesExistentes.isEmpty()) repositorio else ReporteRepositoryFalso(resultado, reportesExistentes),
        consentimientoRepository = ConsentimientoRepositoryFalso(idAnonimo),
        rastreador = RastreadorDeUbicacion(proveedor, TestTimeSource()),
        almacenFotos = almacenFotos,
        clasificador = clasificador,
        reductorDeFotos = reductor,
    )
}
