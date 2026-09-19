package com.wanchaqlimpio.app.domain.repository

import com.wanchaqlimpio.app.domain.model.PuntoCritico

interface PuntosCriticosRepository {
    /**
     * Pide al servidor los focos **confirmados** (docs/diseno.md §4.2, `GET /puntos-criticos`). Los fallos de red o del
     * servidor no lanzan: se devuelven como [ResultadoPuntosCriticos.Fallo] para que la pantalla los muestre.
     */
    suspend fun obtenerConfirmados(): ResultadoPuntosCriticos
}

sealed interface ResultadoPuntosCriticos {
    /** [puntos] puede ser vacío: es el estado normal mientras ningún foco haya sido confirmado en campo. */
    data class Exito(
        val puntos: List<PuntoCritico>,
    ) : ResultadoPuntosCriticos

    data class Fallo(
        val motivo: MotivoFalloMapa,
    ) : ResultadoPuntosCriticos
}

enum class MotivoFalloMapa {
    /** No hubo respuesta: sin red, servidor dormido que no despertó a tiempo, o tiempo de espera agotado. */
    SIN_CONEXION,

    /** El servidor respondió con un error (5xx, o 4xx inesperado). */
    SERVIDOR,

    /** El servidor respondió 2xx pero el cuerpo no es un GeoJSON interpretable. */
    RESPUESTA_INVALIDA,
}
