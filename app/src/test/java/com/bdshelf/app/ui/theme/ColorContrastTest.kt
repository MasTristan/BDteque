package com.bdshelf.app.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garde-fou de la porte de qualité G3 (§E6) : les jetons de [Color] ne
 * repassent jamais sous les seuils de contraste mesurés lors de l'audit de
 * reprise (docs/specs/DESIGN-SYSTEM.md §2-3), en particulier le contour des
 * tranches manquantes — la métaphore fondatrice du produit, mesurée à
 * 1,56:1 avant correction, la moitié du seuil réglementaire.
 *
 * Volontairement indépendant de `androidx.compose.ui.graphics.Color` : ce
 * test tourne sur la JVM nue (pas d'Android, pas d'émulateur), donc les
 * teintes sont recopiées ici en Int packé — **si un jeton de [Color] change,
 * mettre à jour la constante correspondante ci-dessous, sans quoi ce test
 * ne vérifie plus rien de réel.**
 */
class ColorContrastTest {

    // Recopiés depuis Color.kt (voir la remarque de classe ci-dessus).
    private val paper = 0xFFF5EFE2.toInt()
    private val ink = 0xFF1C1A17.toInt()
    private val inkSoft = 0xFF5A534A.toInt()
    private val accent = 0xFFA83024.toInt()
    private val ownedGreen = 0xFF1F5E3E.toInt()
    private val surface = 0xFFFFFDF7.toInt()
    private val ghost = 0xFF8C8371.toInt()

    private val paperDark = 0xFF17140F.toInt()
    private val inkDark = 0xFFEDE6D8.toInt()
    private val inkSoftDark = 0xFFB8AF9F.toInt()
    private val accentDark = 0xFFE0796B.toInt()
    private val ownedGreenDark = 0xFF7BC9A0.toInt()
    private val surfaceDark = 0xFF221E17.toInt()
    private val ghostDark = 0xFF7C7460.toInt()

    /** WCAG 2.2 §1.4.3 : texte porteur de sens. */
    private val textThreshold = 4.5
    /** WCAG 2.2 §1.4.11 : composant d'interface non textuel (le contour d'une tranche). */
    private val uiComponentThreshold = 3.0

    @Test
    fun lightTheme_textOnPaper_meetsWcagAA() {
        assertContrastAtLeast("Ink/Paper", ink, paper, textThreshold)
        assertContrastAtLeast("InkSoft/Paper", inkSoft, paper, textThreshold)
        assertContrastAtLeast("Accent/Paper", accent, paper, textThreshold)
        assertContrastAtLeast("OwnedGreen/Paper", ownedGreen, paper, textThreshold)
    }

    @Test
    fun lightTheme_textOnSurface_meetsWcagAA() {
        assertContrastAtLeast("Ink/Surface", ink, surface, textThreshold)
        assertContrastAtLeast("InkSoft/Surface", inkSoft, surface, textThreshold)
    }

    @Test
    fun lightTheme_ghostOutline_meetsWcagUiComponentThreshold() {
        // Le défaut le plus grave de l'audit : le vide d'un tome manquant
        // doit rester visible sans dépendre de la couleur.
        assertContrastAtLeast("Ghost/Paper", ghost, paper, uiComponentThreshold)
    }

    @Test
    fun darkTheme_textOnPaper_meetsWcagAA() {
        assertContrastAtLeast("InkDark/PaperDark", inkDark, paperDark, textThreshold)
        assertContrastAtLeast("InkSoftDark/PaperDark", inkSoftDark, paperDark, textThreshold)
        assertContrastAtLeast("AccentDark/PaperDark", accentDark, paperDark, textThreshold)
        assertContrastAtLeast("OwnedGreenDark/PaperDark", ownedGreenDark, paperDark, textThreshold)
    }

    @Test
    fun darkTheme_textOnSurface_meetsWcagAA() {
        assertContrastAtLeast("InkDark/SurfaceDark", inkDark, surfaceDark, textThreshold)
        assertContrastAtLeast("InkSoftDark/SurfaceDark", inkSoftDark, surfaceDark, textThreshold)
        assertContrastAtLeast("AccentDark/SurfaceDark", accentDark, surfaceDark, textThreshold)
    }

    @Test
    fun darkTheme_ghostOutline_meetsWcagUiComponentThreshold() {
        assertContrastAtLeast("GhostDark/PaperDark", ghostDark, paperDark, uiComponentThreshold)
    }

    private fun assertContrastAtLeast(label: String, foreground: Int, background: Int, threshold: Double) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            "$label : ratio mesuré $ratio, attendu >= $threshold",
            ratio >= threshold,
        )
    }

    /** Ratio de contraste WCAG 2.2 : (L1 + 0.05) / (L2 + 0.05), L1 la plus claire des deux. */
    private fun contrastRatio(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(argb: Int): Double {
        val r = linearize((argb shr 16 and 0xFF) / 255.0)
        val g = linearize((argb shr 8 and 0xFF) / 255.0)
        val b = linearize((argb and 0xFF) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linearize(channel: Double): Double =
        if (channel <= 0.03928) channel / 12.92 else Math.pow((channel + 0.055) / 1.055, 2.4)
}
