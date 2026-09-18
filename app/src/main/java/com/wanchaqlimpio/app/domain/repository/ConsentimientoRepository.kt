package com.wanchaqlimpio.app.domain.repository

import com.wanchaqlimpio.app.domain.model.ConsentimientoEstado
import kotlinx.coroutines.flow.Flow

interface ConsentimientoRepository {
    val estado: Flow<ConsentimientoEstado>

    /** Genera el id_anonimo (si no existe) y registra la fecha de aceptación. */
    suspend fun aceptarConsentimiento()

    /** Borra el id_anonimo y la fecha, revocando el consentimiento (RNF-05). */
    suspend fun revocarConsentimiento()
}
