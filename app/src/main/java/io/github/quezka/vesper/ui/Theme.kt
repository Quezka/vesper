package io.github.quezka.vesper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Quire's palette: indigo accent, flat cards with a 1px border and no shadows. */
object Palette {
    val AccentLight = Color(0xFF5B5BD6)
    val AccentDark = Color(0xFF8182F0)
    val Danger = Color(0xFFE5484D)
    val Success = Color(0xFF30A46C)
    val Amber = Color(0xFFF5A524)

    val BgLight = Color(0xFFF5F6F8)
    val SurfaceLight = Color(0xFFFFFFFF)
    val BorderLight = Color(0xFFE2E4E9)
    val TextLight = Color(0xFF16181D)
    val MutedLight = Color(0xFF636A75)

    val BgDark = Color(0xFF111214)
    val SurfaceDark = Color(0xFF18191C)
    val RaisedDark = Color(0xFF202226)
    val BorderDark = Color(0xFF2A2C31)
    val TextDark = Color(0xFFECEDEF)
    val MutedDark = Color(0xFF9AA0AA)
}

@Composable
fun VesperTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            primary = Palette.AccentDark, onPrimary = Color(0xFF0E0E24),
            background = Palette.BgDark, onBackground = Palette.TextDark,
            surface = Palette.SurfaceDark, onSurface = Palette.TextDark,
            surfaceVariant = Palette.RaisedDark, onSurfaceVariant = Palette.MutedDark,
            outline = Palette.BorderDark, error = Palette.Danger,
        )
    } else {
        lightColorScheme(
            primary = Palette.AccentLight, onPrimary = Color.White,
            background = Palette.BgLight, onBackground = Palette.TextLight,
            surface = Palette.SurfaceLight, onSurface = Palette.TextLight,
            surfaceVariant = Palette.BgLight, onSurfaceVariant = Palette.MutedLight,
            outline = Palette.BorderLight, error = Palette.Danger,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
