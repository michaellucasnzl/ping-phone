package com.michaellucas.pingphone.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AutumnPrimaryDark,
    onPrimary = AutumnOnPrimaryDark,
    primaryContainer = AutumnPrimaryContainerDark,
    onPrimaryContainer = AutumnOnPrimaryContainerDark,
    secondary = AutumnSecondaryDark,
    onSecondary = AutumnOnSecondaryDark,
    secondaryContainer = AutumnSecondaryContainerDark,
    onSecondaryContainer = AutumnOnSecondaryContainerDark,
    tertiary = AutumnTertiaryDark,
    onTertiary = AutumnOnTertiaryDark,
    tertiaryContainer = AutumnTertiaryContainerDark,
    onTertiaryContainer = AutumnOnTertiaryContainerDark,
    background = AutumnBackgroundDark,
    onBackground = AutumnOnBackgroundDark,
    surface = AutumnSurfaceDark,
    onSurface = AutumnOnSurfaceDark,
    surfaceVariant = AutumnSurfaceVariantDark,
    onSurfaceVariant = AutumnOnSurfaceVariantDark,
    outline = AutumnOutlineDark,
    outlineVariant = AutumnOutlineVariantDark,
    error = AutumnErrorDark,
    onError = AutumnOnErrorDark,
    errorContainer = AutumnErrorContainerDark,
    onErrorContainer = AutumnOnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = AutumnPrimaryLight,
    onPrimary = AutumnOnPrimaryLight,
    primaryContainer = AutumnPrimaryContainerLight,
    onPrimaryContainer = AutumnOnPrimaryContainerLight,
    secondary = AutumnSecondaryLight,
    onSecondary = AutumnOnSecondaryLight,
    secondaryContainer = AutumnSecondaryContainerLight,
    onSecondaryContainer = AutumnOnSecondaryContainerLight,
    tertiary = AutumnTertiaryLight,
    onTertiary = AutumnOnTertiaryLight,
    tertiaryContainer = AutumnTertiaryContainerLight,
    onTertiaryContainer = AutumnOnTertiaryContainerLight,
    background = AutumnBackgroundLight,
    onBackground = AutumnOnBackgroundLight,
    surface = AutumnSurfaceLight,
    onSurface = AutumnOnSurfaceLight,
    surfaceVariant = AutumnSurfaceVariantLight,
    onSurfaceVariant = AutumnOnSurfaceVariantLight,
    outline = AutumnOutlineLight,
    outlineVariant = AutumnOutlineVariantLight,
    error = AutumnErrorLight,
    onError = AutumnOnErrorLight,
    errorContainer = AutumnErrorContainerLight,
    onErrorContainer = AutumnOnErrorContainerLight
)

@Composable
fun PingPhoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled: WallpaperManager binder call is slow on the emulator and
    // can generate near-black palettes from dark wallpapers, making the screen appear blank.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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
        content = content
    )
}
