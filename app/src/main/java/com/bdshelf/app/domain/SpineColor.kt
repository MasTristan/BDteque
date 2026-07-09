package com.bdshelf.app.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Palette de ~12 teintes chaudes désaturées pour les tranches de série. */
val SpinePalette = listOf(
    Color(0xFF7B6B8D), // violet poussiéreux
    Color(0xFF5B8A6F), // vert sauge
    Color(0xFF8B6B3D), // noyer
    Color(0xFF4A7A9B), // bleu acier
    Color(0xFF9B7B4A), // ambre
    Color(0xFF6B8B7B), // teal gris
    Color(0xFF8B5B6B), // mauve
    Color(0xFF5B7B5B), // mousse
    Color(0xFF9B6B5B), // terracotta
    Color(0xFF5B6B9B), // ardoise bleue
    Color(0xFF7B8B5B), // olive
    Color(0xFF6B5B8B), // violet gris
)

/** Couleur de tranche assignée de façon déterministe (hash de l'id), stable entre lancements. */
fun seriesSpineColor(seriesId: String): Color =
    SpinePalette[seriesId.hashCode().mod(SpinePalette.size)]

/** Conversion pour stockage dans `Series.color` (ARGB packé en Long). */
fun Color.toArgbLong(): Long = this.toArgb().toLong() and 0xFFFFFFFFL

/** Conversion depuis `Series.color` vers une [Color] Compose. */
fun Long.toSpineColor(): Color = Color(this.toInt())
