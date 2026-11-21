package com.example.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

data class ThemePreset(
    val id: String,
    val nameAr: String,
    val primaryArgb: Long,
    val secondaryArgb: Long,
    val accentArgb: Long
)

object ContrastUtils {
    const val DEFAULT_PRIMARY_ARGB = 0xFF064E3B
    const val DEFAULT_SECONDARY_ARGB = 0xFF10B981
    const val DEFAULT_ACCENT_ARGB = 0xFFD4AF37

    val Presets = listOf(
        ThemePreset(
            id = "emerald_gold",
            nameAr = "الزمردي والذهبي (الافتراضي)",
            primaryArgb = DEFAULT_PRIMARY_ARGB,
            secondaryArgb = DEFAULT_SECONDARY_ARGB,
            accentArgb = DEFAULT_ACCENT_ARGB
        ),
        ThemePreset(
            id = "royal_sapphire",
            nameAr = "الياقوتي الفاخر",
            primaryArgb = 0xFF0F3460,
            secondaryArgb = 0xFF1D6FE2,
            accentArgb = 0xFFF59E0B
        ),
        ThemePreset(
            id = "warm_olive",
            nameAr = "الزيتوني الدافئ",
            primaryArgb = 0xFF365314,
            secondaryArgb = 0xFF65A30D,
            accentArgb = 0xFFD97706
        ),
        ThemePreset(
            id = "plum_pearl",
            nameAr = "البرقوقي الأنيق",
            primaryArgb = 0xFF4C1D95,
            secondaryArgb = 0xFF7C3AED,
            accentArgb = 0xFFEAB308
        ),
        ThemePreset(
            id = "crimson_sand",
            nameAr = "العنابي الملكي",
            primaryArgb = 0xFF7F1D1D,
            secondaryArgb = 0xFFDC2626,
            accentArgb = 0xFFFBBF24
        )
    )

    fun onColorFor(background: Color): Color {
        return if (background.luminance() > 0.36f) Color(0xFF111827) else Color(0xFFFFFFFF)
    }

    fun containerFor(base: Color, darkTheme: Boolean): Color {
        return if (darkTheme) {
            Color(
                red = (base.red * 0.35f + 0.08f).coerceIn(0f, 1f),
                green = (base.green * 0.35f + 0.08f).coerceIn(0f, 1f),
                blue = (base.blue * 0.35f + 0.08f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            Color(
                red = (base.red * 0.16f + 0.84f).coerceIn(0f, 1f),
                green = (base.green * 0.16f + 0.84f).coerceIn(0f, 1f),
                blue = (base.blue * 0.16f + 0.84f).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
    }

    fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    }

    fun colorToHsv(color: Color): FloatArray {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        return hsv
    }
}
