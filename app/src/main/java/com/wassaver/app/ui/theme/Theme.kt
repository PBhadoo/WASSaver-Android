package com.wassaver.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// WhatsApp-inspired green palette
val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppDarkGreen = Color(0xFF075E54)
val WhatsAppTeal = Color(0xFF128C7E)
val WhatsAppLightGreen = Color(0xFFDCF8C6)
val WhatsAppBlue = Color(0xFF34B7F1)

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A4D2E),
    onPrimaryContainer = Color(0xFFB8F5D0),
    secondary = WhatsAppTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0A3D35),
    onSecondaryContainer = Color(0xFFB0E8DF),
    tertiary = WhatsAppBlue,
    onTertiary = Color.White,
    background = Color(0xFF111B21),
    onBackground = Color(0xFFE9EDEF),
    surface = Color(0xFF111B21),
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = Color(0xFF1F2C34),
    onSurfaceVariant = Color(0xFF8696A0),
    error = Color(0xFFEA4335),
    onError = Color.White,
    outline = Color(0xFF8696A0),
    surfaceContainerHighest = Color(0xFF2A3942),
    surfaceContainerHigh = Color(0xFF233138),
    surfaceContainer = Color(0xFF1D2B32),
    surfaceContainerLow = Color(0xFF182429),
    surfaceContainerLowest = Color(0xFF0D1418),
)

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppDarkGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F5D0),
    onPrimaryContainer = Color(0xFF002110),
    secondary = WhatsAppTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB0E8DF),
    onSecondaryContainer = Color(0xFF00201B),
    tertiary = WhatsAppBlue,
    onTertiary = Color.White,
    background = Color(0xFFF0F2F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8ECF0),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFEA4335),
    onError = Color.White,
    outline = Color(0xFF79747E),
)

@Composable
fun WASSaverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        typography = Typography(),
        content = content
    )
}
