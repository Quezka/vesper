package io.github.quezka.vesper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** The suite's palette (suite-kit's quezka_ui/theme.py, as Quire's phone app): brighter indigo, tinted backgrounds. */
object Palette {
    val AccentLight = Color(0xFF4F46E5)
    val AccentDark = Color(0xFFA9A6FF)
    val Danger = Color(0xFFD0262D)
    val Success = Color(0xFF127A46)
    val Amber = Color(0xFFF5A524)
    val Glow = Color(0xFFEC4899)

    val BgLight = Color(0xFFF3F2FF)
    val SurfaceLight = Color(0xFFFFFFFF)
    val RaisedLight = Color(0xFFE9E8FF)
    val BorderLight = Color(0xFFB3B6CC)
    val TextLight = Color(0xFF0B0D14)
    val MutedLight = Color(0xFF343B4D)

    val BgDark = Color(0xFF0E0E19)
    val SurfaceDark = Color(0xFF1A1A2B)
    val RaisedDark = Color(0xFF27273F)
    val BorderDark = Color(0xFF4B4E66)
    val TextDark = Color(0xFFF6F7FB)
    val MutedDark = Color(0xFFCBCFDD)
}

/** Liquid glass on top of Material 3, as across the suite: a colour wash behind, see-through cards with a light edge. */
object Glass {
    const val tint = 0.72f

    @Composable
    fun wash(): Brush {
        val c = MaterialTheme.colorScheme
        val dark = c.background.luminance() < 0.5f
        return Brush.linearGradient(listOf(
            lerp(c.background, c.primary, if (dark) 0.20f else 0.16f), c.background,
            lerp(c.background, Palette.Glow, if (dark) 0.13f else 0.10f)))
    }

    @Composable
    fun edge(): BorderStroke {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return BorderStroke(1.dp, Brush.verticalGradient(listOf(
            Color.White.copy(alpha = if (dark) 0.16f else 0.95f), Color.White.copy(alpha = if (dark) 0.04f else 0.35f))))
    }

    @Composable
    fun surface(): Color = MaterialTheme.colorScheme.surface.copy(alpha = tint)
}

@Composable
fun VesperTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            primary = Palette.AccentDark, onPrimary = Color(0xFF0B0B14),
            background = Palette.BgDark, onBackground = Palette.TextDark,
            surface = Palette.SurfaceDark, onSurface = Palette.TextDark,
            surfaceVariant = Palette.RaisedDark, onSurfaceVariant = Palette.MutedDark,
            outline = Palette.BorderDark, error = Color(0xFFFF7B80),
        )
    } else {
        lightColorScheme(
            primary = Palette.AccentLight, onPrimary = Color.White,
            background = Palette.BgLight, onBackground = Palette.TextLight,
            surface = Palette.SurfaceLight, onSurface = Palette.TextLight,
            surfaceVariant = Palette.RaisedLight, onSurfaceVariant = Palette.MutedLight,
            outline = Palette.BorderLight, error = Palette.Danger,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
