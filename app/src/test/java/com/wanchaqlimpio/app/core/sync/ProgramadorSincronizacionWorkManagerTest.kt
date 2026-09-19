package com.wanchaqlimpio.app.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Cómo se encolan los envíos. Es donde estaba el hueco por el que un reporte guardado mientras se enviaba otro podía
 * quedar sin enviar: con la política KEEP, encolar en ese momento no hacía nada.
 */
@RunWith(RobolectricTestRunner::class)
class ProgramadorSincronizacionWorkManagerTest {
    private val contexto: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var programador: ProgramadorSincronizacionWorkManager

    @Before
    fun iniciarWorkManagerDePrueba() {
        val configuracion = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(contexto, configuracion)
        programador = ProgramadorSincronizacionWorkManager(contexto)
    }

    private fun envios(): List<WorkInfo> =
        WorkManager.getInstance(contexto).getWorkInfosForUniqueWork(NOMBRE_TRABAJO_SINCRONIZACION).get()

    private fun estados() = envios().map { it.state }.toSet()

    @Test
    fun `guardar dos reportes seguidos encadena el segundo envio detras del primero`() {
        programador.programarSincronizacion()
        programador.programarSincronizacion()

        // El segundo espera a que termine el primero (BLOCKED) en vez de descartarse: así no se pierde el reporte
        // guardado mientras el primero se estaba enviando.
        assertEquals(2, envios().size)
        assertEquals(setOf(WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED), estados())
    }

    @Test
    fun `al abrir la app varias veces no se apilan envios`() {
        repeat(3) { programador.sincronizarAlIniciar() }

        assertEquals(1, envios().size)
        assertEquals(setOf(WorkInfo.State.ENQUEUED), estados())
    }

    @Test
    fun `al abrir la app no se apila detras de un envio que ya esta esperando`() {
        programador.programarSincronizacion()

        programador.sincronizarAlIniciar()

        assertEquals(1, envios().size)
    }

    @Test
    fun `enviar ahora reemplaza el envio que esperaba por uno nuevo, sin reintentos previos`() {
        programador.programarSincronizacion()
        val anterior = envios().single().id

        programador.sincronizarAhora()

        // REPLACE cancela y BORRA el envío anterior: queda uno solo, nuevo y listo para correr en cuanto haya red.
        val nuevo = envios().single()
        assertEquals(WorkInfo.State.ENQUEUED, nuevo.state)
        assertNotEquals(anterior, nuevo.id)
    }

    @Test
    fun `el envio solo corre cuando hay red`() {
        programador.programarSincronizacion()

        assertEquals(NetworkType.CONNECTED, envios().single().constraints.requiredNetworkType)
    }
}
