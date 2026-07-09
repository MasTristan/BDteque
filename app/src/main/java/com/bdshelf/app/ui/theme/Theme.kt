package com.bdshelf.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Palette claire BDShelf : mapping des jetons §5.1 vers le ColorScheme Material 3.
 *
 * - background -> Paper
 * - surface    -> Surface
 * - primary    -> Accent
 * - onBackground / onSurface -> Ink
 * - outline    -> Ghost
 *
 * Pas de mode sombre, pas de couleurs dynamiques (NEVER §SPEC) : ces choix
 * sont définitifs et ne sont pas exposés en paramètres.
 */
private val BdShelfColorScheme = lightColorScheme(
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
 * Thème unique de l'application. Pas de paramètre `darkTheme` ni
 * `dynamicColor` : v1 = thème clair uniquement, jetons fixes (NEVER §SPEC).
 */
@Composable
fun BdShelfTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BdShelfColorScheme,
        typography = Typography,
        content = content,
    )
}
