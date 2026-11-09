package io.github.quezka.vesper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.quezka.vesper.R

/** The clock's two status colours, for its black background (the rest comes from the colour scheme). */
object Palette {
    val Success = Color(0xFF4ADE80)  // on the clock's black
    val Amber = Color(0xFFF5A524)
}

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal), Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold), Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extralight, FontWeight.ExtraLight),  // the clock's big digits
)

/** Material's type scale in the suite's typeface. */
private val SuiteType = SuiteTokens.typography(Manrope)

/** The suite's palette, the same as the desktop's (quezka_ui/theme.py), Quire's and Cadence's. */
private val Light = SuiteTokens.Light

private val Dark = SuiteTokens.Dark

@Composable
fun VesperTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, typography = SuiteType, content = content)
}
