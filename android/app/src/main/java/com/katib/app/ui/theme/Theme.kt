package com.katib.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KatibTeal = Color(0xFF0A7B5E)
val KatibTealDark = Color(0xFF065C46)
val KatibGold = Color(0xFFC8960C)
val KatibCharcoal = Color(0xFF1C1C1E)
val KatibErrorOrange = Color(0xFFE8730C)

private val LightColors = lightColorScheme(
    primary = KatibTeal,
    onPrimary = Color.White,
    secondary = KatibGold,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = KatibCharcoal,
    surface = Color.White,
    onSurface = KatibCharcoal,
    surfaceVariant = Color(0xFFF1F3F2),
)

private val DarkColors = darkColorScheme(
    primary = KatibTeal,
    onPrimary = Color.White,
    secondary = KatibGold,
    onSecondary = Color.Black,
    background = KatibCharcoal,
    onBackground = Color.White,
    surface = Color(0xFF2C2C2E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3A3A3C),
)

@Composable
fun KatibTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
