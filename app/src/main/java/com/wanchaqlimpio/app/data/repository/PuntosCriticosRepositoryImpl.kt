package com.wanchaqlimpio.app.data.repository

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.wanchaqlimpio.app.data.mapper.aPuntosCriticos
import com.wanchaqlimpio.app.data.remote.api.PuntosCriticosApi
import com.wanchaqlimpio.app.data.remote.dto.PuntosCriticosDto
import com.wanchaqlimpio.app.domain.repository.MotivoFalloMapa
import com.wanchaqlimpio.app.domain.repository.PuntosCriticosRepository
import com.wanchaqlimpio.app.domain.repository.ResultadoPuntosCriticos
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import javax.inject.Inject

class PuntosCriticosRepositoryImpl
    @Inject
    constructor(
        private val api: PuntosCriticosApi,
    ) : PuntosCriticosRepository {
        override suspend fun obtenerConfirmados(): ResultadoPuntosCriticos =
            try {
                interpretar(api.obtenerConfirmados())
            } catch (ignored: JsonParseException) {
                // El cuerpo es JSON, pero no tiene la forma del contrato (p. ej. una lista donde debía ir un objeto).
                ResultadoPuntosCriticos.Fallo(MotivoFalloMapa.RESPUESTA_INVALIDA)
            } catch (falla: IOException) {
                ResultadoPuntosCriticos.Fallo(motivoDe(falla))
            }

        private fun interpretar(respuesta: Response<PuntosCriticosDto>): ResultadoPuntosCriticos {
            val cuerpo = respuesta.body()
            return when {
                !respuesta.isSuccessful -> ResultadoPuntosCriticos.Fallo(MotivoFalloMapa.SERVIDOR)
                cuerpo == null -> ResultadoPuntosCriticos.Fallo(MotivoFalloMapa.RESPUESTA_INVALIDA)
                else -> ResultadoPuntosCriticos.Exito(cuerpo.aPuntosCriticos())
            }
        }

        /**
         * Gson avisa de un cuerpo que no es JSON (una página HTML, texto suelto) o que llegó vacío o cortado con
         * excepciones de entrada/salida, igual que un corte de red. Se distinguen porque el mensaje al usuario es otro:
         * en un caso el servidor respondió algo inservible; en el otro no se pudo hablar con él (sin red, tiempo de
         * espera agotado, servidor dormido que no despertó a tiempo).
         */
        private fun motivoDe(falla: IOException): MotivoFalloMapa =
            when (falla) {
                is MalformedJsonException, is EOFException -> MotivoFalloMapa.RESPUESTA_INVALIDA
                else -> MotivoFalloMapa.SIN_CONEXION
            }
    }
