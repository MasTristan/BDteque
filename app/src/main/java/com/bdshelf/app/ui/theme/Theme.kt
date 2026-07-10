package com.bdshelf.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Palettes BDShelf : mapping des jetons §5.1 vers le ColorScheme Material 3.
 *
 * - background -> Paper
 * - surface    -> Surface
 * - primary    -> Accent
 * - onBackground / onSurface -> Ink
 * - outline    -> Ghost
 *
 * Pas de couleurs dynamiques : les jetons restent fixes pour garder l'identité
 * papier/encre. Le mode sombre (ajouté après la v1) suit le réglage
 * Apparence ([com.bdshelf.app.data.prefs.ThemeMode]) — le système par défaut.
 */
private val BdShelfLightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Surface,
    primaryContainer = Accent,
    onPrimaryContainer = Surface,
    secondary = OwnedGreen,
    onSecondary = Surface,
    secondaryContainer = OwnedGreen,
    onSecondaryContainer = Surface,
    tertiary = OwnedGreen,
    onTertiary = Surface,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = InkSoft,
    outline = Ghost,
    outlineVariant = Ghost,
    error = Accent,
    onError = Surface,
)

/**
 * En sombre, les accents éclaircis portent du texte sombre (onPrimary = encre
 * sombre) : un texte blanc sur rouge clair ne tiendrait pas le contraste visé.
 */
private val BdShelfDarkColorScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = PaperDark,
    primaryContainer = AccentDark,
    onPrimaryContainer = PaperDark,
    secondary = OwnedGreenDark,
    onSecondary = PaperDark,
    secondaryContainer = OwnedGreenDark,
    onSecondaryContainer = PaperDark,
    tertiary = OwnedGreenDark,
    onTertiary = PaperDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = PaperDark,
    onSurfaceVariant = InkSoftDark,
    outline = GhostDark,
    outlineVariant = GhostDark,
    error = AccentDark,
    onError = PaperDark,
)

/** Thème de l'application. [darkTheme] est résolu en amont depuis le réglage Apparence. */
@Composable
fun BdShelfTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) BdShelfDarkColorScheme else BdShelfLightColorScheme,
        typography = Typography,
        content = content,
    )
}
