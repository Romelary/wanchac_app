package com.wanchaqlimpio.app.domain.repository

import com.wanchaqlimpio.app.domain.model.SugerenciaIa

/**
 * Clasificación automática del residuo fotografiado (RF-04). La implementación corre EN el teléfono: la
 * foto no sale del dispositivo para clasificarse (ADR-0001).
 */
interface ClasificadorResiduo {
    /**
     * Devuelve la opinión del modelo sobre la foto, o `null` si no se pudo obtener: el modelo no cargó, la
     * foto no se pudo leer o la salida del modelo no fue válida. Un `null` NUNCA debe impedir reportar: el
     * ciudadano elige la categoría a mano.
     */
    suspend fun clasificar(fotoPath: String): SugerenciaIa?
}
