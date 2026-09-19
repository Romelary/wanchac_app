package com.wanchaqlimpio.app.ui.mapa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanchaqlimpio.app.BuildConfig

/** Mapa público de focos confirmados (Módulo 5, ADR-0005). Solo lectura. */
@Composable
fun MapaScreen(viewModel: MapaViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Cada vez que se abre la pestaña se vuelve a pedir la lista: un foco pudo confirmarse mientras tanto.
    LaunchedEffect(Unit) { viewModel.recargar() }

    MapaContent(
        estado = estado,
        avisarFaltaLaClave = BuildConfig.DEBUG && !BuildConfig.MAPS_CLAVE_CONFIGURADA,
        onActualizar = viewModel::recargar,
    ) { puntos -> MapaGoogle(puntos) }
}
