package com.wanchaqlimpio.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.wanchaqlimpio.app.domain.model.TipoDeRed

/**
 * Dice qué conexión tiene el teléfono ahora mismo. Es una interfaz para que el envío y el guardado de reportes se
 * puedan probar sin tocar la red real (ver [MonitorDeRedAndroid]).
 */
interface MonitorDeRed {
    fun actual(): TipoDeRed
}

/**
 * Lee la red activa de Android. No guarda ni expone nada que identifique la red (ni su nombre ni su dirección): solo
 * su tipo y si Android la da por buena para Internet.
 */
class MonitorDeRedAndroid(
    private val context: Context,
) : MonitorDeRed {
    override fun actual(): TipoDeRed {
        val administrador = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val capacidades =
            administrador?.let { am -> am.activeNetwork?.let { am.getNetworkCapabilities(it) } }
                ?: return TipoDeRed.SIN_RED
        return clasificarRed(
            conInternetValidado = capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            esWifi = capacidades.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            esMovil = capacidades.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
        )
    }
}

/**
 * La decisión, sin Android para poder probarla: que la red exista no basta. Android marca como «validada» la que de
 * verdad sale a Internet; un Wi-Fi sin salida (o con portal cautivo) es justo la condición que más engaña en campo,
 * porque el teléfono cree tener conexión y los envíos fallan.
 */
internal fun clasificarRed(
    conInternetValidado: Boolean,
    esWifi: Boolean,
    esMovil: Boolean,
): TipoDeRed =
    when {
        !conInternetValidado -> TipoDeRed.SIN_INTERNET
        esWifi -> TipoDeRed.WIFI
        esMovil -> TipoDeRed.DATOS_MOVILES
        else -> TipoDeRed.OTRA
    }
