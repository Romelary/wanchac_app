package com.wanchaqlimpio.app.domain

/**
 * Verifica si una coordenada cae dentro del distrito de Wanchaq (CORR-007:
 * los criterios de inclusión exigen el límite del distrito, 6.38 km², no
 * el de la provincia del Cusco).
 *
 * ADVERTENCIA — límites aproximados, no oficiales: el rectángulo de abajo es
 * un cálculo de caja delimitadora (bounding box) hecho a mano a partir de
 * coordenadas públicas aproximadas de Cusco/Wanchaq, NO el polígono oficial
 * del distrito. Sirve solo como pre-filtro rápido en el cliente para
 * descartar coordenadas obviamente fuera de rango antes de guardar. La
 * validación definitiva y confiable debe hacerse en el backend con PostGIS
 * (`ST_Within` contra el polígono real, obtenido del catastro municipal o
 * del INEI) una vez que el backend exista (ver plan.md, Módulo 4).
 * Pendiente registrado en progreso.md.
 */
object LimiteDistritalWanchaq {
    private const val LATITUD_MINIMA = -13.545
    private const val LATITUD_MAXIMA = -13.515
    private const val LONGITUD_MINIMA = -71.965
    private const val LONGITUD_MAXIMA = -71.930

    fun contiene(
        latitud: Double,
        longitud: Double,
    ): Boolean =
        latitud in LATITUD_MINIMA..LATITUD_MAXIMA &&
            longitud in LONGITUD_MINIMA..LONGITUD_MAXIMA

    /*
     * Esquinas y centro del rectángulo, como pares (latitud, longitud), para encuadrar el mapa (Módulo 5). El
     * dominio se mantiene en Kotlin puro: la pantalla del mapa los convierte a los tipos de Google Maps. Son los
     * mismos límites aproximados de arriba, con la misma advertencia.
     */
    val suroeste: Pair<Double, Double> get() = LATITUD_MINIMA to LONGITUD_MINIMA
    val noreste: Pair<Double, Double> get() = LATITUD_MAXIMA to LONGITUD_MAXIMA
    val centro: Pair<Double, Double> get() =
        (LATITUD_MINIMA + LATITUD_MAXIMA) / 2 to
            (LONGITUD_MINIMA + LONGITUD_MAXIMA) / 2
}
