package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = IndigoPrimaryDark,
    secondary = EmeraldSuccess,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = EmeraldContainerDark,
    tertiary = CyanAccent,
    onTertiary = Color.White,
    tertiaryContainer = CyanContainerLight,
    onTertiaryContainer = CyanContainerDark,
    error = RoseError,
    onError = Color.White,
    errorContainer = RoseContainerLight,
    onErrorContainer = RoseContainerDark,
    background = LightBackground,
    onBackground = Slate900,
    surface = LightSurface,
    onSurface = Slate900,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Slate600,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoLight,
    onPrimary = IndigoContainerDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoContainerLight,
    secondary = EmeraldLight,
    onSecondary = EmeraldContainerDark,
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = EmeraldContainerLight,
    tertiary = CyanLight,
    onTertiary = CyanContainerDark,
    tertiaryContainer = CyanContainerDark,
    onTertiaryContainer = CyanContainerLight,
    error = RoseLight,
    onError = RoseContainerDark,
    errorContainer = RoseContainerDark,
    onErrorContainer = RoseContainerLight,
    background = DarkBackground,
    onBackground = Slate100,
    surface = DarkSurface,
    onSurface = Slate100,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Slate300,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
