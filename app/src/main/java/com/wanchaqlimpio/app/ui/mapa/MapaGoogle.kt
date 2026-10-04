package com.wanchaqlimpio.app.ui.mapa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.domain.LimiteDistritalWanchaq
import com.wanchaqlimpio.app.domain.model.PuntoCritico
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.ui.common.etiqueta
import com.wanchaqlimpio.app.ui.common.formatearFechaHora

private const val ZOOM_INICIAL = 14f
private const val ZOOM_MINIMO = 12f

/** Margen (≈ 1 km) alrededor del distrito por el que se puede desplazar el mapa, sin atrapar al usuario. */
private const val MARGEN_GRADOS = 0.01

@Composable
fun MapaGoogle(
    puntos: List<PuntoCritico>,
    misReportes: List<Reporte> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var capaActiva by remember { mutableStateOf(0) } // 0: Focos, 1: Mis Reportes, 2: Calor

    val camara =
        rememberCameraPositionState {
            val (latitud, longitud) = LimiteDistritalWanchaq.centro
            position = CameraPosition.fromLatLngZoom(LatLng(latitud, longitud), ZOOM_INICIAL)
        }
    val limites =
        LatLngBounds(
            LatLng(
                LimiteDistritalWanchaq.suroeste.first - MARGEN_GRADOS,
                LimiteDistritalWanchaq.suroeste.second - MARGEN_GRADOS,
            ),
            LatLng(
                LimiteDistritalWanchaq.noreste.first + MARGEN_GRADOS,
                LimiteDistritalWanchaq.noreste.second + MARGEN_GRADOS,
            ),
        )

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camara,
            properties = MapProperties(latLngBoundsForCameraTarget = limites, minZoomPreference = ZOOM_MINIMO),
            uiSettings = MapUiSettings(mapToolbarEnabled = false, zoomControlsEnabled = true),
        ) {
            if (capaActiva == 0) {
                puntos.forEach { punto ->
                    val titulo = punto.categoriaPredominante?.etiqueta() ?: stringResource(R.string.mapa_marcador_titulo)
                    val reportes = pluralStringResource(R.plurals.mapa_marcador_reportes, punto.numReportes, punto.numReportes)
                    val detalle =
                        if (punto.fechaDeteccionEpochMillis > 0) {
                            stringResource(
                                R.string.mapa_marcador_detalle,
                                reportes,
                                formatearFechaHora(punto.fechaDeteccionEpochMillis),
                            )
                        } else {
                            reportes
                        }
                    key(punto.id) {
                        Marker(
                            state = rememberUpdatedMarkerState(position = LatLng(punto.latitud, punto.longitud)),
                            title = titulo,
                            snippet = detalle,
                        )
                    }
                }
            } else if (capaActiva == 1) {
                misReportes.forEach { reporte ->
                    key(reporte.id) {
                        Marker(
                            state = rememberUpdatedMarkerState(position = LatLng(reporte.latitud, reporte.longitud)),
                            title = "Mi Reporte",
                            snippet = reporte.categoria.name,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                }
            } else if (capaActiva == 2) {
                // Placeholder para mapa de calor.
            }
        }

        // Switcher de Capas (Botón flotante desplegable)
        var menuCapasExpandido by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 96.dp, end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (menuCapasExpandido) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        shadowElevation = 8.dp,
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            val opciones = listOf("Focos", "Míos", "Calor")
                            opciones.forEachIndexed { index, texto ->
                                val isSelected = capaActiva == index
                                val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                                Surface(
                                    modifier = Modifier
                                        .widthIn(min = 120.dp)
                                        .clickable { 
                                            capaActiva = index 
                                            menuCapasExpandido = false 
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = bgColor
                                ) {
                                    Text(
                                        text = texto,
                                        color = textColor,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                FloatingActionButton(
                    onClick = { menuCapasExpandido = !menuCapasExpandido },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Capas")
                }
            }
        }
    }
}
