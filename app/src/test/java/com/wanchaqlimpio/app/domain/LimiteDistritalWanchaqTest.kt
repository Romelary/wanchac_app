package com.wanchaqlimpio.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las esquinas y el centro que usa el mapa (Módulo 5) deben coincidir con el rectángulo con el que se valida un
 * reporte: si un día cambia uno, el otro tiene que cambiar con él.
 */
class LimiteDistritalWanchaqTest {
    @Test
    fun `las esquinas del mapa estan dentro del distrito y son las esquinas de verdad`() {
        val (latitudSur, longitudOeste) = LimiteDistritalWanchaq.suroeste
        val (latitudNorte, longitudEste) = LimiteDistritalWanchaq.noreste

        assertTrue(LimiteDistritalWanchaq.contiene(latitudSur, longitudOeste))
        assertTrue(LimiteDistritalWanchaq.contiene(latitudNorte, longitudEste))
        assertTrue(latitudSur < latitudNorte && longitudOeste < longitudEste) // suroeste queda "abajo a la izquierda"
        // un paso más afuera de cada esquina ya no es Wanchaq
        assertFalse(LimiteDistritalWanchaq.contiene(latitudSur - 0.0001, longitudOeste))
        assertFalse(LimiteDistritalWanchaq.contiene(latitudSur, longitudOeste - 0.0001))
        assertFalse(LimiteDistritalWanchaq.contiene(latitudNorte + 0.0001, longitudEste))
        assertFalse(LimiteDistritalWanchaq.contiene(latitudNorte, longitudEste + 0.0001))
    }

    @Test
    fun `el centro esta dentro y a medio camino de las esquinas`() {
        val (latitud, longitud) = LimiteDistritalWanchaq.centro

        assertTrue(LimiteDistritalWanchaq.contiene(latitud, longitud))
        assertEquals((LimiteDistritalWanchaq.suroeste.first + LimiteDistritalWanchaq.noreste.first) / 2, latitud, 1e-9)
        assertEquals(
            (LimiteDistritalWanchaq.suroeste.second + LimiteDistritalWanchaq.noreste.second) / 2,
            longitud,
            1e-9,
        )
    }

    @Test
    fun `el punto que usa la ubicacion simulada cae dentro del rectangulo del mapa`() {
        // ProveedorUbicacionSimulado: latitud inicial -13.540, longitud -71.9475
        assertTrue(LimiteDistritalWanchaq.contiene(-13.540, -71.9475))
    }
}
