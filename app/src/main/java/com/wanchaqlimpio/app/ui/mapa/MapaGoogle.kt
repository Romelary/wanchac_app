package com.wanchaqlimpio.app.ui.mapa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.wanchaqlimpio.app.ui.common.etiqueta
import com.wanchaqlimpio.app.ui.common.formatearFechaHora

private const val ZOOM_INICIAL = 14f
private const val ZOOM_MINIMO = 12f

/** Margen (≈ 1 km) alrededor del distrito por el que se puede desplazar el mapa, sin atrapar al usuario. */
private const val MARGEN_GRADOS = 0.01

/**
 * Capa FINA sobre Google Maps: solo dibuja. Toda la lógica (estados, textos, reintentos) vive en [MapaContent] y
 * [MapaViewModel], que sí se prueban en la JVM; esta función necesita Google Play services y una clave válida, así que
 * solo se puede comprobar en un dispositivo (escenario G del protocolo, docs/prueba-en-dispositivo.md).
 *
 * El mapa se encuadra en Wanchaq y el desplazamiento se limita al distrito más un margen: es un mapa del distrito.
 */
@Composable
fun MapaGoogle(
    puntos: List<PuntoCritico>,
    modifier: Modifier = Modifier,
) {
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

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = camara,
        properties = MapProperties(latLngBoundsForCameraTarget = limites, minZoomPreference = ZOOM_MINIMO),
        uiSettings = MapUiSettings(mapToolbarEnabled = false, zoomControlsEnabled = true),
    ) {
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
            // key(id): cada foco conserva su marcador aunque cambie el orden o el resto de la lista al recargar.
            key(punto.id) {
                Marker(
                    state = rememberUpdatedMarkerState(position = LatLng(punto.latitud, punto.longitud)),
                    title = titulo,
                    snippet = detalle,
                )
            }
        }
    }
}
