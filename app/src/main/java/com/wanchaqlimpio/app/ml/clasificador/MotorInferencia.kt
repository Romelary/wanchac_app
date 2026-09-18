package com.wanchaqlimpio.app.ml.clasificador

import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer

/**
 * Ejecuta el modelo. Existe como interfaz para que la lógica del clasificador (umbral, validación de la
 * salida, manejo de errores) se pueda probar en la JVM sin las librerías nativas de LiteRT, que solo
 * corren en Android.
 */
interface MotorInferencia {
    /** Devuelve una probabilidad por clase. Lanza excepción si el modelo falla. */
    fun inferir(entrada: ByteBuffer): FloatArray
}

/**
 * Motor real sobre LiteRT (el runtime que antes se llamaba TensorFlow Lite; ver ADR-0003). El `Interpreter`
 * NO es seguro entre hilos, así que las inferencias se serializan.
 */
class MotorLiteRt(
    private val interprete: Interpreter,
    private val numClases: Int,
) : MotorInferencia {
    @Synchronized
    override fun inferir(entrada: ByteBuffer): FloatArray {
        val salida = Array(1) { FloatArray(numClases) }
        entrada.rewind()
        interprete.run(entrada, salida)
        return salida[0]
    }
}
