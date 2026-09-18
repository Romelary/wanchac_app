package com.wanchaqlimpio.app.ml.clasificador

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo

/**
 * Lo que la app necesita saber del modelo, leído de `labels.txt` y `metadata.json` (assets/clasificador/).
 * El contrato completo, y por qué cada dato está ahí, vive en `ml/README.md`.
 *
 * [desde] valida TODO el contrato al cargar y falla con un mensaje claro: si el modelo se cambia por otro
 * (p. ej. uno reentrenado con otro orden de clases) y este archivo no se actualiza, la app etiquetaría mal
 * TODAS las predicciones sin dar ningún error. Es mejor que el clasificador quede "no disponible" (y el
 * ciudadano elija a mano) a que sugiera categorías equivocadas.
 */
data class ConfiguracionModelo(
    /** Categorías que el modelo predice, en el orden EXACTO de su salida. */
    val clases: List<CategoriaResiduo>,
    /** Confianza mínima para proponer la clase del modelo; por debajo la app propone `MIXTO`. */
    val umbralConfianza: Float,
    /** Lado (px) de la imagen cuadrada de entrada. */
    val lado: Int,
) {
    companion object {
        fun desde(
            labels: String,
            metadataJson: String,
        ): ConfiguracionModelo {
            val etiquetas = labels.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val dto = Gson().fromJson(metadataJson, MetadataModeloDto::class.java)
            val metadata = requireNotNull(dto) { "metadata.json está vacío" }

            val clases = etiquetas.map(::categoriaDe)
            require(clases.isNotEmpty()) { "labels.txt no tiene ninguna clase" }
            require(clases.toSet().size == clases.size) { "labels.txt repite una clase: $etiquetas" }
            require(metadata.clases == etiquetas) {
                "metadata.json y labels.txt no coinciden en las clases o su orden: ${metadata.clases} vs $etiquetas"
            }

            val manuales = metadata.categoriasManuales.orEmpty().map(::categoriaDe)
            require(clases.none { it in manuales }) {
                "Una categoría no puede ser a la vez del modelo y manual: $etiquetas / $manuales"
            }

            val entrada = metadata.entrada?.forma.orEmpty()
            val esCuadrada = entrada.size == FORMA_ENTRADA && entrada[0] == 1 && entrada[1] == entrada[2]
            require(esCuadrada && entrada[3] == CANALES) {
                "La entrada del modelo debe ser [1, N, N, 3]; metadata.json dice $entrada"
            }
            require(metadata.salida?.forma == listOf(1, clases.size)) {
                "La salida del modelo debe ser [1, ${clases.size}]; metadata.json dice ${metadata.salida?.forma}"
            }

            val umbral = metadata.umbralApp ?: metadata.umbralSugerido
            require(umbral != null && umbral > 0f && umbral <= 1f) {
                "El umbral de confianza debe estar en (0, 1]: $umbral"
            }

            return ConfiguracionModelo(clases = clases, umbralConfianza = umbral, lado = entrada[1])
        }

        private fun categoriaDe(etiqueta: String): CategoriaResiduo =
            requireNotNull(CategoriaResiduo.entries.firstOrNull { it.name.equals(etiqueta, ignoreCase = true) }) {
                "Etiqueta desconocida en el modelo: '$etiqueta'"
            }

        private const val FORMA_ENTRADA = 4
        private const val CANALES = 3
    }
}

/** Solo los campos de metadata.json que la app usa; el resto (métricas, dataset, etc.) es para humanos. */
private data class MetadataModeloDto(
    @SerializedName("clases") val clases: List<String>?,
    @SerializedName("categorias_manuales") val categoriasManuales: List<String>?,
    @SerializedName("umbral_confianza_app") val umbralApp: Float?,
    @SerializedName("umbral_confianza_sugerido") val umbralSugerido: Float?,
    @SerializedName("entrada") val entrada: FormaDto?,
    @SerializedName("salida") val salida: FormaDto?,
)

private data class FormaDto(
    @SerializedName("forma") val forma: List<Int>?,
)
