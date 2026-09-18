package com.wanchaqlimpio.app.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import java.io.IOException

private const val ETIQUETA_LOG = "DecodificadorFotos"

/**
 * Decodifica una foto de reporte como el ojo humano la vería: reducida (para no gastar memoria con las
 * decenas de megapíxeles de una cámara) y con la ORIENTACIÓN de su metadato EXIF aplicada.
 *
 * La orientación importa: CameraX no gira los píxeles al guardar el JPEG, solo escribe en EXIF cuánto hay que
 * girarlos. `BitmapFactory` ignora ese dato, así que sin este paso el modelo de IA vería la foto acostada
 * (y la pantalla de confirmación también). Lo usan el clasificador y esa pantalla, para que ambos vean lo mismo.
 *
 * [ladoMinimo] es el lado menor que debe conservar la imagen tras reducirla. Devuelve `null` si el archivo no
 * existe o no es una imagen legible.
 */
fun decodificarFoto(
    fotoPath: String,
    ladoMinimo: Int,
): Bitmap? {
    val original = decodificarReducida(fotoPath, ladoMinimo) ?: return null
    return orientar(original, transformacionExif(leerOrientacion(fotoPath)))
}

/** Decodifica la imagen ya reducida (ver [calcularMuestreo]), o `null` si el archivo no es una imagen legible. */
private fun decodificarReducida(
    fotoPath: String,
    ladoMinimo: Int,
): Bitmap? {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(fotoPath, limites)
    val esLegible = limites.outWidth > 0 && limites.outHeight > 0
    val opciones =
        BitmapFactory.Options().apply {
            inSampleSize = calcularMuestreo(limites.outWidth, limites.outHeight, ladoMinimo)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    return if (esLegible) BitmapFactory.decodeFile(fotoPath, opciones) else null
}

private fun orientar(
    original: Bitmap,
    transformacion: TransformacionExif,
): Bitmap {
    if (transformacion == TransformacionExif.NINGUNA) return original

    val matriz =
        Matrix().apply {
            if (transformacion.gradosHorario != 0) postRotate(transformacion.gradosHorario.toFloat())
            if (transformacion.espejoHorizontal) postScale(-1f, 1f)
        }
    val orientada = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
    if (orientada !== original) original.recycle()
    return orientada
}

private fun leerOrientacion(fotoPath: String): Int =
    try {
        ExifInterface(fotoPath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: IOException) {
        // Best-effort: sin EXIF legible se asume la orientación normal en vez de descartar la foto.
        Log.w(ETIQUETA_LOG, "No se pudo leer la orientación EXIF", e)
        ExifInterface.ORIENTATION_NORMAL
    }

/**
 * Mayor potencia de 2 con la que se puede reducir la imagen al decodificarla sin que su lado MENOR baje de
 * [ladoMinimo] (`inSampleSize` solo admite potencias de 2). Nunca menos de 1.
 */
internal fun calcularMuestreo(
    ancho: Int,
    alto: Int,
    ladoMinimo: Int,
): Int {
    val lado = minOf(ancho, alto)
    var muestreo = 1
    while (lado / (muestreo * 2) >= ladoMinimo) muestreo *= 2
    return muestreo
}

/**
 * Cómo llevar la imagen tal como se guardó a como se ve: primero girar [gradosHorario] y, después, si
 * [espejoHorizontal], reflejarla de izquierda a derecha (mismo convenio que las librerías de imágenes usuales).
 */
internal data class TransformacionExif(
    val gradosHorario: Int,
    val espejoHorizontal: Boolean,
) {
    companion object {
        val NINGUNA = TransformacionExif(0, false)
    }
}

/** Traduce el valor de la etiqueta EXIF Orientation (1 a 8) a una [TransformacionExif]. */
internal fun transformacionExif(orientacion: Int): TransformacionExif =
    when (orientacion) {
        ExifInterface.ORIENTATION_ROTATE_90 -> TransformacionExif(90, false)
        ExifInterface.ORIENTATION_ROTATE_180 -> TransformacionExif(180, false)
        ExifInterface.ORIENTATION_ROTATE_270 -> TransformacionExif(270, false)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> TransformacionExif(0, true)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> TransformacionExif(180, true)
        ExifInterface.ORIENTATION_TRANSPOSE -> TransformacionExif(90, true)
        ExifInterface.ORIENTATION_TRANSVERSE -> TransformacionExif(270, true)
        else -> TransformacionExif.NINGUNA
    }
