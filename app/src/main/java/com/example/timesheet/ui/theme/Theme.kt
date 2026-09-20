package com.example.timesheet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SteelBlueSoft,
    onPrimary = NavyDark,
    primaryContainer = SteelBlue,
    onPrimaryContainer = Color.White,
    secondary = BlueGreySoft,
    onSecondary = Color(0xFF1B2A35),
    secondaryContainer = Color(0xFF33434E),
    onSecondaryContainer = BlueGreyLight,
    tertiary = SafetyOrangeSoft,
    onTertiary = SafetyOrangeDark,
    tertiaryContainer = Color(0xFF7A3300),
    onTertiaryContainer = SafetyOrangeLight,
    background = Color(0xFF10151A),
    onBackground = Color(0xFFF7F9FB),
    surface = Color(0xFF10151A),
    onSurface = Color(0xFFF7F9FB),
    surfaceVariant = Color(0xFF2A333B),
    onSurfaceVariant = Color(0xFFDDE3E9),
    surfaceTint = SteelBlueSoft,
    surfaceDim = Color(0xFF10151A),
    surfaceBright = Color(0xFF353E47),
    surfaceContainerLowest = Color(0xFF0B0F13),
    surfaceContainerLow = Color(0xFF151B21),
    surfaceContainer = Color(0xFF1A2128),
    surfaceContainerHigh = Color(0xFF232B33),
    surfaceContainerHighest = Color(0xFF2D363F),
    outline = Color(0xFFA9B1B9),
    outlineVariant = Color(0xFF5C666F)
)

private val LightColorScheme = lightColorScheme(
    primary = SteelBlue,
    onPrimary = Color.White,
    primaryContainer = SteelBlueLight,
    onPrimaryContainer = NavyDark,
    secondary = BlueGrey,
    onSecondary = Color.White,
    secondaryContainer = BlueGreyLight,
    onSecondaryContainer = Color(0xFF1B2A35),
    tertiary = SafetyOrange,
    onTertiary = Color.White,
    tertiaryContainer = SafetyOrangeLight,
    onTertiaryContainer = SafetyOrangeDark,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF0F1214),
    surface = Color(0xFFF5F7FA),
    onSurface = Color(0xFF0F1214),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF2F353B),
    surfaceTint = SteelBlue,
    surfaceDim = Color(0xFFD8DDE3),
    surfaceBright = Color(0xFFF5F7FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF2F6),
    surfaceContainer = Color(0xFFE9EDF2),
    surfaceContainerHigh = Color(0xFFE3E8EE),
    surfaceContainerHighest = Color(0xFFDDE3EA),
    outline = Color(0xFF4E555C),
    outlineVariant = Color(0xFF9AA3AB)
)

/** Solid blue used for the top bars and main buttons; deep steel in dark mode, so it never goes pale. */
val ColorScheme.bar: Color
    get() = if (background.luminance() > 0.5f) primary else primaryContainer

val ColorScheme.onBar: Color
    get() = if (background.luminance() > 0.5f) onPrimary else onPrimaryContainer

@Composable
fun isAppDark(): Boolean = when (AppearanceSettings.themeMode) {
    ThemeMode.AUTO -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun TimesheetTheme(
    darkTheme: Boolean = isAppDark(),
    // Off by default so the app always uses its own blue, not the wallpaper colors.
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

    // The app's own "Text size" choice multiplies whatever font size the phone already uses.
    val base = LocalDensity.current
    val density = Density(base.density, base.fontScale * AppearanceSettings.textSize.scale)

    CompositionLocalProvider(LocalDensity provides density) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
