@file:OptIn(ExperimentalTextApi::class)

package com.bdshelf.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bdshelf.app.R

/**
 * Fraunces (variable) — display / titres d'écran / numéros de tome.
 * opsz fixé à 72 (registre "display"), SOFT 50 pour le rendu chaleureux
 * recherché par la maquette, WONK désactivé pour rester lisible.
 */
val FrauncesFamily = FontFamily(
    Font(
        resId = R.font.fraunces_variable,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.Setting("opsz", 72f),
            FontVariation.Setting("SOFT", 50f),
            FontVariation.Setting("WONK", 0f),
        ),
    ),
    Font(
        resId = R.font.fraunces_variable,
        weight = FontWeight.Medium,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.Setting("opsz", 72f),
            FontVariation.Setting("SOFT", 50f),
            FontVariation.Setting("WONK", 0f),
        ),
    ),
    Font(
        resId = R.font.fraunces_variable,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.Setting("opsz", 72f),
            FontVariation.Setting("SOFT", 50f),
            FontVariation.Setting("WONK", 0f),
        ),
    ),
    Font(
        resId = R.font.fraunces_italic_variable,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.Setting("opsz", 72f),
            FontVariation.Setting("SOFT", 50f),
            FontVariation.Setting("WONK", 0f),
        ),
    ),
)

/** Atkinson Hyperlegible — corps de texte, listes, titres de série. */
val AtkinsonHyperlegibleFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.atkinson_hyperlegible_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.atkinson_hyperlegible_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

// Échelle de tailles (§5.2 SPEC) : Display 34 / Titre écran 28 / Titre série 22 /
// Corps 18 / Légende 15. Toutes en sp pour suivre fontScale système.
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    // Titre d'écran
    titleLarge = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    // Numéro de tome sur la tranche
    titleMedium = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    // Titre de série en liste
    titleSmall = TextStyle(
        fontFamily = AtkinsonHyperlegibleFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    // Corps de texte
    bodyLarge = TextStyle(
        fontFamily = AtkinsonHyperlegibleFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AtkinsonHyperlegibleFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = AtkinsonHyperlegibleFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = AtkinsonHyperlegibleFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    // Légendes, badges
    labelMedium = TextStyle(
        fontFamily = AtkinsonHyperlegibleFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = AtkinsonHyperlegibleFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)
