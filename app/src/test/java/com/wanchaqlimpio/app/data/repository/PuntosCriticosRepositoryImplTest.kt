package com.wanchaqlimpio.app.data.repository

import com.wanchaqlimpio.app.data.remote.api.PuntosCriticosApi
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.repository.MotivoFalloMapa
import com.wanchaqlimpio.app.domain.repository.ResultadoPuntosCriticos
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** JSON con la forma exacta del contrato `GET /puntos-criticos` (docs/diseno.md §4.2). */
private const val DOS_FOCOS =
    """{"type":"FeatureCollection","features":[
      {"type":"Feature","geometry":{"type":"Point","coordinates":[-71.9475,-13.53]},
       "properties":{"id":11,"numReportes":7,"categoriaPredominante":"PLASTICO","fechaDeteccionEpochMillis":1790000000000}},
      {"type":"Feature","geometry":{"type":"Point","coordinates":[-71.95,-13.52]},
       "properties":{"id":12,"numReportes":4,"categoriaPredominante":"ORGANICO","fechaDeteccionEpochMillis":1790100000000}}
    ]}"""

/** Habla con un servidor de mentira por HTTP real: comprueba Retrofit + Gson + mapper + manejo de errores juntos. */
class PuntosCriticosRepositoryImplTest {
    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: PuntosCriticosRepositoryImpl

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        val api =
            Retrofit
                .Builder()
                .baseUrl(servidor.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PuntosCriticosApi::class.java)
        repositorio = PuntosCriticosRepositoryImpl(api)
    }

    @After
    fun limpiar() {
        servidor.close()
    }

    @Test
    fun `dos focos confirmados llegan como dos puntos con todos sus datos`() =
        runTest {
            servidor.enqueue(MockResponse(code = 200, body = DOS_FOCOS))

            val resultado = repositorio.obtenerConfirmados() as ResultadoPuntosCriticos.Exito

            assertEquals(listOf(11L, 12L), resultado.puntos.map { it.id })
            val primero = resultado.puntos.first()
            assertEquals(-13.53, primero.latitud, 0.0)
            assertEquals(-71.9475, primero.longitud, 0.0)
            assertEquals(7, primero.numReportes)
            assertEquals(CategoriaResiduo.PLASTICO, primero.categoriaPredominante)
            assertEquals(CategoriaResiduo.ORGANICO, resultado.puntos.last().categoriaPredominante)
        }

    @Test
    fun `pide GET a puntos-criticos y no envia ningun identificador de dispositivo`() =
        runTest {
            servidor.enqueue(MockResponse(code = 200, body = DOS_FOCOS))

            repositorio.obtenerConfirmados()

            val peticion = servidor.takeRequest(1, TimeUnit.SECONDS)!!
            assertEquals("GET", peticion.method)
            assertEquals("/puntos-criticos", peticion.url.encodedPath)
            assertEquals(null, peticion.url.query) // sin ?estado=…: el servidor SOLO devuelve confirmados
            // los focos son públicos: la petición no identifica al dispositivo
            assertEquals(null, peticion.headers["X-Id-Anonimo"])
        }

    @Test
    fun `una coleccion vacia es un exito con cero puntos, no un error`() =
        runTest {
            servidor.enqueue(MockResponse(code = 200, body = """{"type":"FeatureCollection","features":[]}"""))

            val resultado = repositorio.obtenerConfirmados()

            assertEquals(ResultadoPuntosCriticos.Exito(emptyList()), resultado)
        }

    @Test
    fun `un foco mal formado se descarta pero los buenos se muestran`() =
        runTest {
            val cuerpo =
                """{"type":"FeatureCollection","features":[
                  {"type":"Feature","geometry":{"type":"Point","coordinates":[500,500]},"properties":{"id":1}},
                  {"type":"Feature","geometry":{"type":"Point","coordinates":[-71.95,-13.52]},"properties":{"id":2}}
                ]}"""
            servidor.enqueue(MockResponse(code = 200, body = cuerpo))

            val resultado = repositorio.obtenerConfirmados() as ResultadoPuntosCriticos.Exito

            assertEquals(listOf(2L), resultado.puntos.map { it.id })
        }

    @Test
    fun `un error 5xx o 4xx del servidor es Fallo SERVIDOR`() =
        runTest {
            listOf(500, 502, 503, 404, 401).forEach { codigo ->
                servidor.enqueue(MockResponse(code = codigo))
                assertEquals(
                    "codigo $codigo",
                    ResultadoPuntosCriticos.Fallo(MotivoFalloMapa.SERVIDOR),
                    repositorio.obtenerConfirmados(),
                )
            }
        }

    @Test
    fun `un cuerpo que no es JSON es Fallo RESPUESTA_INVALIDA`() =
        runTest {
            servidor.enqueue(MockResponse(code = 200, body = "<html>esto no es json</html>"))

            assertEquals(
                ResultadoPuntosCriticos.Fallo(MotivoFalloMapa.RESPUESTA_INVALIDA),
                repositorio.obtenerConfirmados(),
            )
        }

    @Test
    fun `un cuerpo vacio o cortado a medias es Fallo RESPUESTA_INVALIDA`() =
        runTest {
            val cuerposInservibles =
                listOf(
                    "", // vacío
                    """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":""", // cortado a medias
                )
            cuerposInservibles.forEach { cuerpo ->
                servidor.enqueue(MockResponse(code = 200, body = cuerpo))
                assertEquals(
                    "cuerpo «$cuerpo»",
                    ResultadoPuntosCriticos.Fallo(MotivoFalloMapa.RESPUESTA_INVALIDA),
                    repositorio.obtenerConfirmados(),
                )
            }
        }

    @Test
    fun `un JSON con otra forma que la del contrato es Fallo RESPUESTA_INVALIDA`() =
        runTest {
            val cuerposConOtraForma =
                listOf(
                    "[1,2,3]", // una lista donde debía ir un objeto
                    """{"type":"FeatureCollection","features":"hola"}""", // texto donde debía ir una lista
                    """{"type":"FeatureCollection","features":[{"properties":{"id":"no es un numero"}}]}""",
                )
            cuerposConOtraForma.forEach { cuerpo ->
                servidor.enqueue(MockResponse(code = 200, body = cuerpo))
                assertEquals(
                    "cuerpo $cuerpo",
                    ResultadoPuntosCriticos.Fallo(MotivoFalloMapa.RESPUESTA_INVALIDA),
                    repositorio.obtenerConfirmados(),
                )
            }
        }

    @Test
    fun `un 204 sin cuerpo es Fallo RESPUESTA_INVALIDA, no un mapa vacio`() =
        runTest {
            servidor.enqueue(MockResponse(code = 204))

            assertEquals(
                ResultadoPuntosCriticos.Fallo(MotivoFalloMapa.RESPUESTA_INVALIDA),
                repositorio.obtenerConfirmados(),
            )
        }

    @Test
    fun `sin servidor al otro lado es Fallo SIN_CONEXION`() =
        runTest {
            servidor.close() // ya nadie escucha en esa dirección: la conexión es rechazada

            assertEquals(ResultadoPuntosCriticos.Fallo(MotivoFalloMapa.SIN_CONEXION), repositorio.obtenerConfirmados())
        }
}
