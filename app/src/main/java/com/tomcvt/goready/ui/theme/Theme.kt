package com.tomcvt.goready.ui.theme

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
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

val PrimaryContainerLight = Green80
val OnPrimaryContainerLight = OnLight

val SecondaryContainerLight = Orange80
val OnSecondaryContainerLight = Orange40

val TertiaryContainerLight = RPink80
val OnTertiaryContainerLight = OnLight

val SurfaceVariantLight = Color(0xFFF1ECE6)
val OnSurfaceVariantLight = OnLight

val OutlineLight = Color(0xFF8F8A84)
/*
private val VibrantLightColorScheme = lightColorScheme(
    primary = Green60,
    onPrimary = OnDark,

    secondary = Orange60,
    onSecondary = Orange40,

    tertiary = RPink60,
    onTertiary = OnDark,

    background = Background90,
    onBackground = OnLight,

    surface = Surface90,
    onSurface = OnLight
)*/

private val VibrantLightColorScheme = lightColorScheme(
    primary = Green60,
    onPrimary = OnDark,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,

    secondary = Orange60,
    onSecondary = Orange40,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,

    tertiary = RPink60,
    onTertiary = OnDark,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,

    background = Background90,
    onBackground = OnLight,

    surface = Surface90,
    onSurface = OnLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,

    outline = OutlineLight
)

val BackgroundDark = Color(0xFF121413)
val SurfaceDark = Color(0xFF1A1F1B)
val SurfaceVariantDark = Color(0xFF2A302C)

val OnBackgroundDark = OnDark
val OnSurfaceDark = OnDark
val OnSurfaceVariantDark = Color(0xFFD6DAD6)

val PrimaryDark = Green80
val SecondaryDark = Orange80
val TertiaryDark = RPink80

val PrimaryContainerDark = Color(0xFF1F3D2A)   // green-tinted
val SecondaryContainerDark = Color(0xFF4A3218) // orange-tinted
val TertiaryContainerDark = Color(0xFF4A2334)  // raspberry-tinted

private val VibrantDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF0E2A1A),

    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = Green90,

    secondary = SecondaryDark,
    onSecondary = Color(0xFF3A250F),

    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = Orange90,

    tertiary = TertiaryDark,
    onTertiary = Color(0xFF3A1A26),

    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = RPink90,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,

    outline = Color(0xFF7A837D)
)

@Composable
fun GoReadyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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

@Composable
fun VibrantLightTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VibrantLightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun VibrantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> VibrantDarkColorScheme
        else -> VibrantLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}