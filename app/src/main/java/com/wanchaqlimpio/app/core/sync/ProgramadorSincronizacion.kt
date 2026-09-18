package com.wanchaqlimpio.app.core.sync

interface ProgramadorSincronizacion {
    /** Encola un intento de sincronización; WorkManager espera a que haya red. */
    fun programarSincronizacion()
}
