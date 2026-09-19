package com.wanchaqlimpio.app.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.math.roundToInt

private const val ETIQUETA_LOG = "ReductorDeFotos"

/** Lado largo máximo de la foto que se sube: sobra para ver un foco de residuos y pesa unos 300–600 KB. */
const val LADO_MAXIMO_FOTO_PIXELES = 1_600
const val CALIDAD_JPEG_FOTO = 85

/** Una foto que ya cabe en el lado máximo y pesa menos que esto se deja como está. */
private const val PESO_QUE_NO_HACE_FALTA_REDUCIR_BYTES = 1_500_000L

/**
 * El tamaño al que hay que reducir una imagen de [ancho] × [alto] para que su lado largo no pase de [ladoMaximo],
 * conservando la proporción; `null` si ya cabe (no se agranda nunca).
 */
internal fun calcularTamanoReducido(
    ancho: Int,
    alto: Int,
    ladoMaximo: Int,
): Pair<Int, Int>? {
    val ladoLargo = maxOf(ancho, alto)
    if (ladoLargo <= ladoMaximo || ancho <= 0 || alto <= 0) return null
    val factor = ladoMaximo.toDouble() / ladoLargo
    return maxOf(1, (ancho * factor).roundToInt()) to maxOf(1, (alto * factor).roundToInt())
}

/**
 * Reduce la foto [archivo] en su sitio (lado largo ≤ [ladoMaximo], JPEG de [calidadJpeg]) con la orientación EXIF ya
 * aplicada a los píxeles; el JPEG nuevo no lleva metadatos (tampoco ubicación). Escribe primero en un archivo
 * temporal y solo lo pone en lugar del original si todo salió bien. Devuelve `false`, con el original intacto, si no
 * se pudo (archivo ilegible, sin memoria, sin espacio…).
 */
@Suppress("TooGenericExceptionCaught")
fun reducirFotoEnDisco(
    archivo: File,
    ladoMaximo: Int = LADO_MAXIMO_FOTO_PIXELES,
    calidadJpeg: Int = CALIDAD_JPEG_FOTO,
): Boolean {
    val temporal = File(archivo.parentFile, "${archivo.name}.tmp")
    return try {
        val dimensiones = leerDimensiones(archivo)
        when {
            dimensiones == null -> false // no existe o no es una imagen legible
            yaEstaReducida(archivo, dimensiones, ladoMaximo) -> true
            else -> {
                val escrita = escribirReducida(archivo, temporal, ladoMaximo, calidadJpeg)
                // Files.move sustituye el original de una sola vez (en Android es un `rename` atómico: no queda a
                // medias) y, a diferencia de File.renameTo, dice POR QUÉ falla en vez de devolver solo `false`.
                if (escrita) Files.move(temporal.toPath(), archivo.toPath(), StandardCopyOption.REPLACE_EXISTING)
                escrita
            }
        }
    } catch (fallo: IOException) {
        Log.w(ETIQUETA_LOG, "No se pudo reducir la foto", fallo)
        false
    } catch (fallo: RuntimeException) {
        // p. ej. un formato que el decodificador no entiende: la foto original se envía tal cual.
        Log.w(ETIQUETA_LOG, "No se pudo reducir la foto", fallo)
        false
    } catch (fallo: OutOfMemoryError) {
        Log.w(ETIQUETA_LOG, "Sin memoria para reducir la foto", fallo)
        false
    } finally {
        if (temporal.exists()) temporal.delete()
    }
}

/** Ancho y alto en píxeles sin decodificar la imagen, o `null` si el archivo no existe o no es una imagen legible. */
private fun leerDimensiones(archivo: File): Pair<Int, Int>? {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    decodificarArchivo(archivo, limites)
    return if (limites.outWidth > 0 && limites.outHeight > 0) limites.outWidth to limites.outHeight else null
}

private fun yaEstaReducida(
    archivo: File,
    dimensiones: Pair<Int, Int>,
    ladoMaximo: Int,
): Boolean {
    val cabe = calcularTamanoReducido(dimensiones.first, dimensiones.second, ladoMaximo) == null
    return cabe && archivo.length() <= PESO_QUE_NO_HACE_FALTA_REDUCIR_BYTES
}

private fun escribirReducida(
    origen: File,
    destino: File,
    ladoMaximo: Int,
    calidadJpeg: Int,
): Boolean {
    // Se decodifica ya orientada y reducida por potencias de 2 (rápido, poca memoria) y luego se ajusta al tamaño.
    val orientada = decodificarFoto(origen.absolutePath, ladoMinimo = ladoMaximo * 3 / 4)
    if (orientada == null) {
        Log.w(ETIQUETA_LOG, "No se pudo decodificar la foto")
        return false
    }
    val tamano = calcularTamanoReducido(orientada.width, orientada.height, ladoMaximo)
    val paraGuardar =
        if (tamano == null) orientada else Bitmap.createScaledBitmap(orientada, tamano.first, tamano.second, true)
    val guardada =
        try {
            FileOutputStream(destino).use { salida ->
                paraGuardar.compress(Bitmap.CompressFormat.JPEG, calidadJpeg, salida)
            }
        } finally {
            if (paraGuardar !== orientada) paraGuardar.recycle()
            orientada.recycle()
        }
    if (!guardada) Log.w(ETIQUETA_LOG, "No se pudo comprimir la foto reducida")
    return guardada
}
