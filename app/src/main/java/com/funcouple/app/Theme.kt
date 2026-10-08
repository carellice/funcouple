package com.funcouple.app

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

// I font sono variabili: ogni peso si ottiene dallo stesso file impostando l'asse "wght".
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, style: FontStyle, vararg weights: Int) = weights.map {
    Font(resId, FontWeight(it), style, variationSettings = FontVariation.Settings(FontVariation.weight(it)))
}

private val Playfair = FontFamily(
    variable(R.font.playfair, FontStyle.Normal, 400, 500, 600, 700, 800) +
        variable(R.font.playfair_italic, FontStyle.Italic, 400, 500, 600, 700, 800),
)

private val Outfit = FontFamily(variable(R.font.outfit, FontStyle.Normal, 300, 400, 500, 600, 700, 800))

object Fc {
    val Bg0 = Color(0xFF0D0410)
    val Bg1 = Color(0xFF2A0A2B)
    val Bg2 = Color(0xFF4D0D30)
    val Pink = Color(0xFFFF3D81)
    val Rose = Color(0xFFFF8FB5)
    val Crimson = Color(0xFFE11D48)
    val Orange = Color(0xFFFF8A3D)
    val Gold = Color(0xFFFFC56B)
    val Violet = Color(0xFFB07CFF)
    val DeepViolet = Color(0xFF7B2FF7)
    val Text = Color(0xFFFFF1F5)
    val Muted = Color(0xB3FFE4EC)
    val Glass = Color(0x14FFFFFF)
    val GlassBorder = Color(0x2EFFFFFF)

    /** Titoli, nomi e testi delle carte. */
    val Display = Playfair

    /** Tutto il resto: etichette, pulsanti, descrizioni. */
    val Body = Outfit
}

data class Level(val name: String, val icon: FcIcon, val colors: List<Color>)

val levels = listOf(
    Level("Dolce", FcIcon.FLOWER, listOf(Fc.Rose, Fc.Pink)),
    Level("Piccante", FcIcon.CHILI, listOf(Fc.Gold, Fc.Orange)),
    Level("Bollente", FcIcon.FLAME, listOf(Fc.Orange, Fc.Crimson)),
    Level("Estremo", FcIcon.DEVIL, listOf(Fc.Pink, Fc.DeepViolet)),
)

@Composable
fun FunCoupleTheme(content: @Composable () -> Unit) {
    val base = Typography()
    fun body(style: androidx.compose.ui.text.TextStyle) = style.copy(fontFamily = Fc.Body)
    val typography = Typography(
        displayLarge = body(base.displayLarge), displayMedium = body(base.displayMedium), displaySmall = body(base.displaySmall),
        headlineLarge = body(base.headlineLarge), headlineMedium = body(base.headlineMedium), headlineSmall = body(base.headlineSmall),
        titleLarge = body(base.titleLarge), titleMedium = body(base.titleMedium), titleSmall = body(base.titleSmall),
        bodyLarge = body(base.bodyLarge), bodyMedium = body(base.bodyMedium), bodySmall = body(base.bodySmall),
        labelLarge = body(base.labelLarge), labelMedium = body(base.labelMedium), labelSmall = body(base.labelSmall),
    )
    MaterialTheme(
        typography = typography,
        colorScheme = darkColorScheme(
            primary = Fc.Pink,
            secondary = Fc.Violet,
            tertiary = Fc.Gold,
            background = Fc.Bg0,
            surface = Fc.Bg1,
            onPrimary = Color.White,
            onBackground = Fc.Text,
            onSurface = Fc.Text,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides Fc.Text, content = content)
    }
}
