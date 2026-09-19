package com.wanchaqlimpio.app.domain.repository

/**
 * Reduce una foto ya guardada para que se pueda subir con una red mala. Las cámaras de hoy dan JPEG de 5 a 15 MB;
 * el servidor rechaza más de 10 MB (413, definitivo) y, aunque no llegara al tope, subir varios MB por datos
 * móviles débiles hace fallar el envío justo donde se mide su confiabilidad.
 */
interface ReductorDeFotos {
    /**
     * Devuelve `true` si la foto quedó lista (reducida o ya era pequeña) y `false` si no se pudo reducir: en ese
     * caso la foto original se conserva intacta y se envía tal cual.
     */
    suspend fun reducir(fotoPath: String): Boolean
}
