package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.ContrastUtils
import com.example.domain.ThemeModePreference

@Immutable
data class MasroufExtraColors(
    val accentGold: Color = WarmGold,
    val onAccentGold: Color = Color(0xFF1F1702),
    val heroStart: Color = DeepEmerald,
    val heroEnd: Color = VibrantEmerald
)

val LocalMasroufExtraColors = staticCompositionLocalOf { MasroufExtraColors() }

val MasroufShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MasroufAlBaitTheme(
    themeMode: ThemeModePreference = ThemeModePreference.SYSTEM,
    primaryArgb: Long = ContrastUtils.DEFAULT_PRIMARY_ARGB,
    secondaryArgb: Long = ContrastUtils.DEFAULT_SECONDARY_ARGB,
    accentArgb: Long = ContrastUtils.DEFAULT_ACCENT_ARGB,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeModePreference.SYSTEM -> isSystemInDarkTheme()
        ThemeModePreference.LIGHT -> false
        ThemeModePreference.DARK -> true
    }

    val primaryColor = Color(primaryArgb.toInt())
    val secondaryColor = Color(secondaryArgb.toInt())
    val accentColor = Color(accentArgb.toInt())

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = secondaryColor,
            onPrimary = ContrastUtils.onColorFor(secondaryColor),
            primaryContainer = ContrastUtils.containerFor(primaryColor, darkTheme = true),
            onPrimaryContainer = Color(0xFFE6F9F0),
            secondary = primaryColor,
            onSecondary = ContrastUtils.onColorFor(primaryColor),
            secondaryContainer = DarkSurfaceVariant,
            onSecondaryContainer = Color(0xFFD9EDE4),
            tertiary = accentColor,
            onTertiary = ContrastUtils.onColorFor(accentColor),
            background = DarkBackground,
            onBackground = Color(0xFFF3F6F4),
            surface = DarkSurface,
            onSurface = Color(0xFFF3F6F4),
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFFB5C7BF),
            outline = Color(0xFF335246),
            error = Color(0xFFF87171),
            onError = Color(0xFF450A0A)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = ContrastUtils.onColorFor(primaryColor),
            primaryContainer = ContrastUtils.containerFor(primaryColor, darkTheme = false),
            onPrimaryContainer = Color(0xFF042F24),
            secondary = secondaryColor,
            onSecondary = ContrastUtils.onColorFor(secondaryColor),
            secondaryContainer = ContrastUtils.containerFor(secondaryColor, darkTheme = false),
            onSecondaryContainer = Color(0xFF052E1E),
            tertiary = accentColor,
            onTertiary = ContrastUtils.onColorFor(accentColor),
            background = CreamBackground,
            onBackground = Color(0xFF111C17),
            surface = WarmCardSurface,
            onSurface = Color(0xFF111C17),
            surfaceVariant = SoftCreamVariant,
            onSurfaceVariant = Color(0xFF47574F),
            outline = Color(0xFFD2DDD6),
            error = DangerRed,
            onError = Color.White
        )
    }

    val extraColors = MasroufExtraColors(
        accentGold = accentColor,
        onAccentGold = ContrastUtils.onColorFor(accentColor),
        heroStart = primaryColor,
        heroEnd = secondaryColor
    )

    CompositionLocalProvider(LocalMasroufExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MasroufTypography,
            shapes = MasroufShapes,
            content = content
        )
    }
}
