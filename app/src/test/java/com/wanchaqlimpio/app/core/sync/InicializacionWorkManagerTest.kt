package com.wanchaqlimpio.app.core.sync

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val INICIALIZADOR_POR_DEFECTO = "androidx.work.WorkManagerInitializer"

/**
 * `SyncWorker` y `EliminarReporteWorker` son `@HiltWorker`: solo se pueden crear con la fábrica de Hilt, que
 * `WanchaqLimpioApp` entrega en `getWorkManagerConfiguration()`. Esa configuración **solo se usa si se quita el
 * inicializador automático de WorkManager** del manifiesto; si no, WorkManager arranca solo con la configuración por
 * defecto y los trabajos no se pueden crear (la documentación de Android lo dice y el lint de `release` lo marca como
 * error fatal). Nadie lo había notado porque la sincronización nunca se ejecutó en un dispositivo. La primera prueba
 * mira el manifiesto **ya fusionado** (lo que de verdad instala Android), no el texto del archivo.
 */
@RunWith(RobolectricTestRunner::class)
class InicializacionWorkManagerTest {
    private val contexto: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `el manifiesto fusionado no trae el inicializador automatico de WorkManager`() {
        val proveedor =
            contexto.packageManager.getProviderInfo(
                ComponentName(contexto, "androidx.startup.InitializationProvider"),
                PackageManager.GET_META_DATA,
            )
        val inicializadores = proveedor.metaData?.keySet().orEmpty()

        // Los demás inicializadores de androidx.startup (emoji, ciclo de vida…) siguen: solo se quita el de
        // WorkManager.
        assertTrue("inicializadores presentes: $inicializadores", inicializadores.isNotEmpty())
        assertFalse("inicializadores presentes: $inicializadores", INICIALIZADOR_POR_DEFECTO in inicializadores)
    }

    @Test
    fun `la configuracion de WorkManager que entrega la aplicacion trae la fabrica de Hilt`() {
        // Se pide directamente a la aplicación y no a `WorkManager.getInstance`: WorkManager es un singleton del
        // proceso y otras pruebas (con su propio WorkManager de prueba) pueden haberlo iniciado antes. Robolectric
        // tampoco ejecuta el inicializador automático, así que esta prueba no delata su presencia (para eso está la
        // anterior): comprueba que, sin él, WorkManager se inicia con lo que entrega WanchaqLimpioApp.
        val fabrica = (contexto as Configuration.Provider).workManagerConfiguration.workerFactory

        assertTrue("fábrica en uso: ${fabrica.javaClass.name}", fabrica is HiltWorkerFactory)
    }
}
