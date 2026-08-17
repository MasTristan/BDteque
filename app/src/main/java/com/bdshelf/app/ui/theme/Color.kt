package com.bdshelf.app.ui.theme

import androidx.compose.ui.graphics.Color

// Jetons de couleur BDShelf (§5.1 SPEC). Contraste texte/fond visé ≥ 7:1.
//
// Anomalie d'accessibilité A-01 (§E6, docs/specs/DESIGN-SYSTEM.md §2) :
// OwnedGreen, Ghost, Accent et GhostDark ont été mesurés sous le seuil WCAG
// 2.2 lors de l'audit de reprise. Le plus grave : Ghost à 1,56:1 sur Paper —
// le contour d'un tome manquant, soit la métaphore fondatrice du produit,
// était presque invisible pour une personne malvoyante. Valeurs corrigées
// ci-dessous ; ratios mesurés dans DESIGN-SYSTEM.md §3, ne pas modifier sans
// remesurer et sans mettre à jour ce document.

// Thème clair : papier chaud.
val Paper = Color(0xFFF5EFE2) // fond général, papier chaud
val Ink = Color(0xFF1C1A17) // texte principal, fort contraste
val InkSoft = Color(0xFF5A534A) // texte secondaire
val Accent = Color(0xFFA83024) // rouge BD franco-belge, CTA uniquement — 5,89:1 sur Paper
val OwnedGreen = Color(0xFF1F5E3E) // verdict "possédé" — 6,72:1 sur Paper
val Surface = Color(0xFFFFFDF7) // cartes, feuilles
val Ghost = Color(0xFF8C8371) // contour des tranches manquantes — 3,27:1 sur Paper (seuil non-texte 3:1)

// Thème sombre : même papier, éteint. Les teintes restent chaudes pour garder
// l'identité "bibliothèque" ; les accents sont éclaircis pour rester lisibles
// sur fond sombre (texte d'accent sombre par-dessus, pas blanc).
val PaperDark = Color(0xFF17140F) // fond général, papier éteint
val InkDark = Color(0xFFEDE6D8) // texte principal
val InkSoftDark = Color(0xFFB8AF9F) // texte secondaire
val AccentDark = Color(0xFFE0796B) // rouge éclairci, CTA uniquement
val OwnedGreenDark = Color(0xFF7BC9A0) // verdict "possédé"
val SurfaceDark = Color(0xFF221E17) // cartes, feuilles
val GhostDark = Color(0xFF7C7460) // contour des tranches manquantes — 3,96:1 sur PaperDark (seuil non-texte 3:1)
