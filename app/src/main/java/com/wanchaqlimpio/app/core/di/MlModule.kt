package com.wanchaqlimpio.app.core.di

import android.content.Context
import com.wanchaqlimpio.app.domain.repository.ClasificadorResiduo
import com.wanchaqlimpio.app.ml.clasificador.ClasificadorPerezoso
import com.wanchaqlimpio.app.ml.clasificador.ClasificadorResiduoImpl
import com.wanchaqlimpio.app.ml.clasificador.ConfiguracionModelo
import com.wanchaqlimpio.app.ml.clasificador.MotorLiteRt
import com.wanchaqlimpio.app.ml.clasificador.PreprocesadorImagen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Singleton

private const val CARPETA_ASSETS = "clasificador"
private const val HILOS_INFERENCIA = 2
private const val CANALES = 3

@Module
@InstallIn(SingletonComponent::class)
object MlModule {
    /** Perezoso: el modelo se carga al clasificar la primera foto, no al abrir la app. */
    @Provides
    @Singleton
    fun provideClasificadorResiduo(
        @ApplicationContext context: Context,
    ): ClasificadorResiduo = ClasificadorPerezoso(crear = { crearClasificador(context) })
}

private fun crearClasificador(context: Context): ClasificadorResiduo {
    val configuracion = cargarConfiguracion(context)
    return ClasificadorResiduoImpl(
        preparador = PreprocesadorImagen(configuracion.lado),
        motor = MotorLiteRt(cargarInterprete(context, configuracion), configuracion.clases.size),
        configuracion = configuracion,
    )
}

/**
 * Lee y VALIDA el contrato del modelo (`labels.txt` + `metadata.json`).
 * Es `internal` para que la prueba en dispositivo lo reutilice.
 */
internal fun cargarConfiguracion(context: Context): ConfiguracionModelo =
    ConfiguracionModelo.desde(
        labels = leerAsset(context, "labels.txt"),
        metadataJson = leerAsset(context, "metadata.json"),
    )

/** Crea el intérprete LiteRT y comprueba que el `.tflite` sea el par del contrato. `internal` por la misma razón. */
internal fun cargarInterprete(
    context: Context,
    configuracion: ConfiguracionModelo,
): Interpreter {
    val opciones = Interpreter.Options().setNumThreads(HILOS_INFERENCIA)
    val interprete = Interpreter(mapearModelo(context, "$CARPETA_ASSETS/clasificador_residuos.tflite"), opciones)

    // Cinturón y tirantes: además de validar metadata.json, se comprueba lo que el propio .tflite declara.
    // Si no coincide, el modelo y su contrato no son el mismo par y es mejor no sugerir nada.
    val entradaEsperada = intArrayOf(1, configuracion.lado, configuracion.lado, CANALES)
    val salidaEsperada = intArrayOf(1, configuracion.clases.size)
    check(interprete.getInputTensor(0).shape().contentEquals(entradaEsperada)) {
        "La entrada del .tflite no coincide con metadata.json"
    }
    check(interprete.getOutputTensor(0).shape().contentEquals(salidaEsperada)) {
        "La salida del .tflite no coincide con labels.txt"
    }
    return interprete
}

private fun leerAsset(
    context: Context,
    nombre: String,
): String =
    context.assets
        .open("$CARPETA_ASSETS/$nombre")
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

/** Requiere que el asset no vaya comprimido en el APK (`noCompress += "tflite"` en build.gradle.kts). */
private fun mapearModelo(
    context: Context,
    ruta: String,
): MappedByteBuffer =
    context.assets.openFd(ruta).use { descriptor ->
        FileInputStream(descriptor.fileDescriptor).use { entrada ->
            entrada.channel.map(FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength)
        }
    }
