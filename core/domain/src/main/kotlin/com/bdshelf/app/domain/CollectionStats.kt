package com.bdshelf.app.domain

/** Statistiques globales affichées sur la carte "Ma collection" de l'accueil. */
data class CollectionStats(
    val seriesCount: Int,
    val ownedCount: Int,
    val missingCount: Int,
)
