package com.wanchaqlimpio.app.ml.clasificador

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import kotlin.math.abs

/** Una salida softmax suma 1; se tolera algo de error numérico (el modelo va cuantizado a float16). */
private const val TOLERANCIA_SUMA = 0.05f

/**
 * Convierte las probabilidades del modelo en una [SugerenciaIa], o `null` si no parecen una distribución
 * de probabilidad válida. Esa comprobación protege de un error silencioso: si alguien cambia el modelo por
 * otro cuya salida no es softmax (p. ej. logits), la "confianza" ya no significaría lo que el umbral asume.
 */
internal fun interpretarSalida(
    probabilidades: FloatArray,
    clases: List<CategoriaResiduo>,
    umbral: Float,
): SugerenciaIa? {
    val esDistribucion =
        probabilidades.size == clases.size &&
            probabilidades.all { it.isFinite() && it in 0f..1f } &&
            abs(probabilidades.sum() - 1f) <= TOLERANCIA_SUMA
    if (!esDistribucion) return null

    val indice = probabilidades.indices.maxBy { probabilidades[it] }
    val categoria = clases[indice]
    val confianza = probabilidades[indice]
    return SugerenciaIa(
        categoria = categoria,
        confianza = confianza,
        propuesta = if (confianza >= umbral) categoria else CategoriaResiduo.MIXTO,
    )
}
