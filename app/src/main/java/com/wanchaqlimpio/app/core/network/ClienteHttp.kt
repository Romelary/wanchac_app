package com.wanchaqlimpio.app.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/*
 * Tiempos de espera del cliente HTTP. Los de OkHttp por defecto (10 s de conexión, lectura y escritura) NO alcanzan: el
 * plan gratuito de Render duerme el servicio tras 15 min sin tráfico y tarda ≈ 1 min en despertar (docs/despliegue.md
 * §2), así que la PRIMERA petición tras un rato de inactividad fallaría siempre. Con estos valores espera al
 * despertar; si aun así no llega, la sincronización de reportes reintenta sola (WorkManager) y el mapa ofrece
 * "Reintentar".
 */

/** Establecer la conexión TCP/TLS: Render acepta la conexión rápido aunque la instancia siga dormida. */
const val CONEXION_SEGUNDOS = 30L

/** Esperar la respuesta: cubre el arranque en frío de Render (≈ 60 s) con margen. */
const val LECTURA_SEGUNDOS = 90L

/** Enviar el cuerpo: una foto de varios MB por datos móviles lentos. */
const val ESCRITURA_SEGUNDOS = 60L

/** Tope de toda la llamada (conexión + envío + espera + lectura), para que nada quede colgado indefinidamente. */
const val LLAMADA_SEGUNDOS = 150L

/**
 * @param nivelDeRegistro en depuración `BODY` (registra TODO, incluidos los identificadores de dispositivo y los bytes
 * de las fotos: nunca debe llegar a publicación); en publicación `NONE`.
 */
fun crearClienteHttp(nivelDeRegistro: HttpLoggingInterceptor.Level): OkHttpClient =
    OkHttpClient
        .Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = nivelDeRegistro })
        .connectTimeout(CONEXION_SEGUNDOS, TimeUnit.SECONDS)
        .readTimeout(LECTURA_SEGUNDOS, TimeUnit.SECONDS)
        .writeTimeout(ESCRITURA_SEGUNDOS, TimeUnit.SECONDS)
        .callTimeout(LLAMADA_SEGUNDOS, TimeUnit.SECONDS)
        .build()
