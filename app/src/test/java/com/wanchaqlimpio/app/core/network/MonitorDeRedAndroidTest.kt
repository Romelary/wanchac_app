package com.wanchaqlimpio.app.core.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanchaqlimpio.app.domain.model.TipoDeRed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Qué conexión anota la app en cada reporte. Sale de aquí el dato «según condición de red» de las pruebas de
 * confiabilidad, así que cada condición debe distinguirse bien, sobre todo el Wi-Fi que no llega a Internet.
 */
@RunWith(RobolectricTestRunner::class)
class MonitorDeRedAndroidTest {
    @Test
    fun `Wi-Fi con salida a Internet es WIFI`() {
        assertEquals(TipoDeRed.WIFI, clasificarRed(conInternetValidado = true, esWifi = true, esMovil = false))
    }

    @Test
    fun `datos moviles con salida a Internet es DATOS_MOVILES`() {
        assertEquals(TipoDeRed.DATOS_MOVILES, clasificarRed(conInternetValidado = true, esWifi = false, esMovil = true))
    }

    @Test
    fun `un Wi-Fi que Android no valida (sin salida o con portal) es SIN_INTERNET y no WIFI`() {
        assertEquals(TipoDeRed.SIN_INTERNET, clasificarRed(conInternetValidado = false, esWifi = true, esMovil = false))
    }

    @Test
    fun `datos moviles sin salida validada tambien es SIN_INTERNET`() {
        assertEquals(TipoDeRed.SIN_INTERNET, clasificarRed(conInternetValidado = false, esWifi = false, esMovil = true))
    }

    @Test
    fun `ethernet, VPN u otra red validada es OTRA`() {
        assertEquals(TipoDeRed.OTRA, clasificarRed(conInternetValidado = true, esWifi = false, esMovil = false))
    }

    @Test
    fun `si hay Wi-Fi y datos a la vez gana el Wi-Fi, que es por donde sale el trafico`() {
        assertEquals(TipoDeRed.WIFI, clasificarRed(conInternetValidado = true, esWifi = true, esMovil = true))
    }

    @Test
    fun `consultar la red real del sistema no falla, tenga o no conexion`() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()

        // Solo se comprueba que la parte de Android (servicio, red activa, capacidades) no lanza ni devuelve nulo: qué
        // red hay lo decide el entorno de la prueba. Su comportamiento real se comprueba en un dispositivo.
        assertNotNull(MonitorDeRedAndroid(contexto).actual())
    }
}
