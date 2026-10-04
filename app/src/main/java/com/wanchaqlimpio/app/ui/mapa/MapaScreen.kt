package com.wanchaqlimpio.app.ui.mapa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanchaqlimpio.app.BuildConfig

import com.wanchaqlimpio.app.ui.misreportes.MisReportesViewModel
import com.wanchaqlimpio.app.ui.misreportes.MisReportesUiState

/** Mapa público de focos confirmados (Módulo 5, ADR-0005). Solo lectura. */
@Composable
fun MapaScreen(
    viewModel: MapaViewModel = hiltViewModel(),
    misReportesViewModel: MisReportesViewModel = hiltViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val estadoMisReportes by misReportesViewModel.uiState.collectAsStateWithLifecycle()

    // Cada vez que se abre la pestaña se vuelve a pedir la lista: un foco pudo confirmarse mientras tanto.
    LaunchedEffect(Unit) { viewModel.recargar() }

    MapaContent(
        estado = estado,
        avisarFaltaLaClave = BuildConfig.DEBUG && !BuildConfig.MAPS_CLAVE_CONFIGURADA,
        onActualizar = viewModel::recargar,
    ) { puntos -> 
        val misReportes = if (estadoMisReportes is MisReportesUiState.Lista) {
            (estadoMisReportes as MisReportesUiState.Lista).reportes
        } else {
            emptyList()
        }
        MapaGoogle(puntos = puntos, misReportes = misReportes) 
    }
}
