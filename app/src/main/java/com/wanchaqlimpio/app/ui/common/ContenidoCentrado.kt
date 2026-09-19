package com.wanchaqlimpio.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ancho máximo de lectura del contenido. En una tablet (una Tab S9+ mide ≈ 1000 dp de ancho en horizontal) un
 * párrafo que ocupa toda la pantalla tiene líneas de más de 150 caracteres, difíciles de leer, y un botón "de
 * borde a borde" resulta desproporcionado. 640 dp es un ancho cómodo de lectura (del orden de 80 caracteres por
 * línea con texto de cuerpo; cifra aproximada) y es mayor que el ancho de cualquier teléfono en vertical, así que
 * ahí no cambia nada.
 */
val ANCHO_MAXIMO_CONTENIDO: Dp = 640.dp

/**
 * Centra el contenido horizontalmente y limita su ancho a [ancho]. En pantallas más angostas que ese valor (un
 * teléfono) ocupa todo el ancho, igual que antes. Ver también `docs/prueba-en-dispositivo.md`.
 */
@Composable
fun ContenidoCentrado(
    modifier: Modifier = Modifier,
    ancho: Dp = ANCHO_MAXIMO_CONTENIDO,
    contenido: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = ancho).fillMaxSize(), content = contenido)
    }
}
