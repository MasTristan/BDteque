package com.bdshelf.app.ui.theme

import androidx.compose.ui.graphics.Color

// Jetons de couleur BDShelf (§5.1 SPEC). Contraste texte/fond visé ≥ 7:1.

// Thème clair : papier chaud.
val Paper = Color(0xFFF5EFE2) // fond général, papier chaud
val Ink = Color(0xFF1C1A17) // texte principal, fort contraste
val InkSoft = Color(0xFF5A534A) // texte secondaire
val Accent = Color(0xFFC0392B) // rouge BD franco-belge, CTA uniquement
val OwnedGreen = Color(0xFF2E7D54) // verdict "possédé"
val Surface = Color(0xFFFFFDF7) // cartes, feuilles
val Ghost = Color(0xFFC9C1B2) // contour des tranches manquantes

// Thème sombre : même papier, éteint. Les teintes restent chaudes pour garder
// l'identité "bibliothèque" ; les accents sont éclaircis pour rester lisibles
// sur fond sombre (texte d'accent sombre par-dessus, pas blanc).
val PaperDark = Color(0xFF17140F) // fond général, papier éteint
val InkDark = Color(0xFFEDE6D8) // texte principal
val InkSoftDark = Color(0xFFB8AF9F) // texte secondaire
val AccentDark = Color(0xFFE0796B) // rouge éclairci, CTA uniquement
val OwnedGreenDark = Color(0xFF7BC9A0) // verdict "possédé"
val SurfaceDark = Color(0xFF221E17) // cartes, feuilles
val GhostDark = Color(0xFF57503F) // contour des tranches manquantes
