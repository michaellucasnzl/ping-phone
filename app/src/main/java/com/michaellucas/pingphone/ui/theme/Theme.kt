package com.michaellucas.pingphone.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PingTeal80,
    secondary = PingTealGrey80,
    tertiary = PingAccent80,
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC2C7CF),
    surfaceVariant = Color(0xFF42474E),
    primaryContainer = Color(0xFF004F48),
    onPrimaryContainer = Color(0xFF9CF1E7),
    outline = Color(0xFF8C9199)
)

private val LightColorScheme = lightColorScheme(
    primary = PingTeal40,
    secondary = PingTealGrey40,
    tertiary = PingAccent40,
    background = Color(0xFFFCFCFF),
    surface = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceVariant = Color(0xFFDEE3EB),
    primaryContainer = Color(0xFF9CF1E7),
    onPrimaryContainer = Color(0xFF00201D),
    outline = Color(0xFF72777F)
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
