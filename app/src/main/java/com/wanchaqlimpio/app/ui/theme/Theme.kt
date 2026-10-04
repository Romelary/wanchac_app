package com.wanchaqlimpio.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MoradoDark,
    onPrimary = MoradoFondoOscuro,
    primaryContainer = MoradoContinental,
    onPrimaryContainer = BlancoContinental,
    secondary = MoradoClaro,
    onSecondary = MoradoFondoOscuro,
    background = MoradoFondoOscuro, // Reemplazamos el negro total por un morado profundo
    surface = MoradoSuperficieOscura, // Reemplazamos las tarjetas grises/negras por morado oscuro
    surfaceVariant = Color(0xFF402466), // Para los botones y barras, un morado más notorio
    onSurfaceVariant = MoradoClaro,
    error = RojoAlerta
)

private val LightColorScheme = lightColorScheme(
    primary = MoradoContinental,
    onPrimary = BlancoContinental,
    primaryContainer = MoradoClaro,
    onPrimaryContainer = MoradoOscuro,
    secondary = MoradoOscuro,
    onSecondary = BlancoContinental,
    background = MoradoFondoClaro, // Un "moradito" clarito de fondo en vez de blanco puro o gris
    surface = BlancoContinental,
    surfaceVariant = Color(0xFFF0E6FA), // Botones y contenedores con un lila muy tenue
    onSurfaceVariant = MoradoOscuro,
    error = RojoAlerta
)

@Composable
fun WanchaqLimpioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // IMPORTANTE: dynamicColor apagado por defecto para que la identidad de marca (Verde Wanchaq) siempre se respete
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
