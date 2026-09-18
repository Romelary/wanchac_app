package com.wanchaqlimpio.app.ml.clasificador

import android.graphics.Bitmap
import com.wanchaqlimpio.app.core.util.decodificarFoto
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val CANALES = 3

/** Convierte la foto guardada en la entrada exacta que espera el modelo (contrato en `ml/README.md`). */
interface PreparadorEntrada {
    /** `float32 [1, lado, lado, 3]`, RGB, píxeles 0–255 sin normalizar; `null` si la foto no se puede leer. */
    fun preparar(fotoPath: String): ByteBuffer?
}

/**
 * Misma preparación que el entrenamiento: la imagen se redimensiona a `lado x lado` SIN conservar la
 * proporción (igual que `tf.image.resize` en el cuaderno) y los píxeles se entregan tal cual, en 0–255:
 * la normalización a [-1, 1] va DENTRO del modelo, así que aquí no debe repetirse.
 */
class PreprocesadorImagen(
    private val lado: Int,
) : PreparadorEntrada {
    override fun preparar(fotoPath: String): ByteBuffer? {
        val orientada = decodificarFoto(fotoPath, ladoMinimo = lado) ?: return null
        val escalada = Bitmap.createScaledBitmap(orientada, lado, lado, true)
        val pixeles = IntArray(lado * lado)
        escalada.getPixels(pixeles, 0, lado, 0, 0, lado, lado)
        if (escalada !== orientada) escalada.recycle()
        orientada.recycle()
        return aBufferFloat(pixeles)
    }
}

/**
 * Píxeles ARGB de Android → `float32` R, G, B (en ese orden, fila por fila) en 0–255. El búfer es
 * "directo" y en el orden de bytes nativo, como exige el intérprete.
 */
internal fun aBufferFloat(pixelesArgb: IntArray): ByteBuffer {
    val buffer = ByteBuffer.allocateDirect(pixelesArgb.size * CANALES * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
    for (pixel in pixelesArgb) {
        buffer.putFloat(((pixel shr 16) and 0xFF).toFloat())
        buffer.putFloat(((pixel shr 8) and 0xFF).toFloat())
        buffer.putFloat((pixel and 0xFF).toFloat())
    }
    buffer.rewind()
    return buffer
}
