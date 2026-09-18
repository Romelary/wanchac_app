package com.wanchaqlimpio.app.ml.clasificador

import android.util.Log
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.domain.repository.ClasificadorResiduo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ETIQUETA_LOG = "ClasificadorResiduo"

private fun registrarEnLogcat(
    mensaje: String,
    causa: Throwable,
) {
    Log.w(ETIQUETA_LOG, mensaje, causa)
}

/**
 * Prepara la foto, ejecuta el modelo e interpreta su salida. Cualquier fallo se REGISTRA y se convierte en
 * `null` (sin sugerencia): la IA es una ayuda, nunca puede impedir que el ciudadano reporte.
 */
class ClasificadorResiduoImpl(
    private val preparador: PreparadorEntrada,
    private val motor: MotorInferencia,
    private val configuracion: ConfiguracionModelo,
    private val registrarError: (String, Throwable) -> Unit = ::registrarEnLogcat,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ClasificadorResiduo {
    override suspend fun clasificar(fotoPath: String): SugerenciaIa? =
        withContext(dispatcher) {
            runCatching {
                val entrada = preparador.preparar(fotoPath) ?: return@runCatching null
                interpretarSalida(motor.inferir(entrada), configuracion.clases, configuracion.umbralConfianza)
            }.getOrElse { causa ->
                if (causa is CancellationException) throw causa
                registrarError("No se pudo clasificar la foto", causa)
                null
            }
        }
}

/** Se usa cuando el modelo no pudo cargarse: el reporte sigue funcionando con elección manual. */
object ClasificadorNoDisponible : ClasificadorResiduo {
    override suspend fun clasificar(fotoPath: String): SugerenciaIa? = null
}

/**
 * Carga el modelo la primera vez que se usa (y en un hilo de fondo), no al abrir la app: cargar las librerías
 * nativas y mapear el modelo cuesta decenas de milisegundos que no deben caer en el hilo principal. Si la carga
 * falla (asset dañado, contrato roto, librería nativa no disponible en ese teléfono) se registra UNA vez y el
 * clasificador queda como [ClasificadorNoDisponible].
 */
class ClasificadorPerezoso(
    crear: () -> ClasificadorResiduo,
    registrarError: (String, Throwable) -> Unit = ::registrarEnLogcat,
) : ClasificadorResiduo {
    private val delegado: ClasificadorResiduo by lazy {
        runCatching(crear).getOrElse { causa ->
            registrarError("El modelo de IA no se pudo cargar: la app sigue con elección manual", causa)
            ClasificadorNoDisponible
        }
    }

    override suspend fun clasificar(fotoPath: String): SugerenciaIa? =
        withContext(Dispatchers.Default) { delegado.clasificar(fotoPath) }
}
