package com.wanchaqlimpio.app.core.sync

interface ProgramadorEliminacion {
    /**
     * Encola la solicitud de baja del reporte en el backend; WorkManager la
     * conserva y la reintenta hasta lograr enviarla, aunque el reporte ya no
     * exista en la base local.
     */
    fun programarEliminacionRemota(
        idAnonimo: String,
        idLocal: Long,
    )
}
