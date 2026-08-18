package com.bdshelf.app.domain

/**
 * Palette de dos de tome, en ARGB (§ADR-002) : le domaine ne connaît pas
 * `androidx.compose.ui.graphics.Color`, seulement des entiers. La conversion
 * vers `Color` vit côté application ([toSpineColor]).
 */
val SpinePaletteArgb: List<Long> = listOf(
    0xFF7B6B8DL, 0xFF5B8A6FL, 0xFF8B6B3DL, 0xFF4A7A9BL,
    0xFF9B7B4AL, 0xFF6B8B7BL, 0xFF8B5B6BL, 0xFF5B7B5BL,
    0xFF9B6B5BL, 0xFF5B6B9BL, 0xFF7B8B5BL, 0xFF6B5B8BL,
)

fun seriesSpineColorArgb(seriesId: String): Long =
    SpinePaletteArgb[seriesId.hashCode().mod(SpinePaletteArgb.size)]
