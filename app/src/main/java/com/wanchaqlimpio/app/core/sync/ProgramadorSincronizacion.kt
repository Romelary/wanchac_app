package com.wanchaqlimpio.app.core.sync

interface ProgramadorSincronizacion {
    /**
     * Encola un envío tras guardar un reporte; WorkManager espera a que haya red. Si ya hay uno en marcha o esperando,
     * el nuevo se ENCADENA detrás: así un reporte guardado mientras otro se está enviando no queda sin enviar.
     */
    fun programarSincronizacion()

    /** Al abrir la app: asegura que haya un envío encolado si quedaron pendientes, sin duplicar el que ya haya. */
    fun sincronizarAlIniciar()

    /**
     * «Enviar ahora»: descarta la espera de un reintento anterior y vuelve a intentarlo enseguida (si hay red).
     * Si había un envío a medias, se interrumpe y se repite: el servidor es idempotente, no se duplica nada.
     */
    fun sincronizarAhora()
}
