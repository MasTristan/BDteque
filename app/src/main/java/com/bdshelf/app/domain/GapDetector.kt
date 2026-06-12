package com.bdshelf.app.domain

import com.bdshelf.app.data.local.entities.Album

/** Détection des trous de collection à partir des albums d'une série. */
object GapDetector {

    /**
     * Numéros de tome manquants entre 1 et le plus grand numéro connu,
     * possédés ou non en base (un trou "implicite" sans Album associé
     * compte aussi comme manquant).
     */
    fun gaps(albums: List<Album>): List<Int> {
        val numbered = albums.mapNotNull { it.tomeNumber }
        if (numbered.isEmpty()) return emptyList()
        val owned = albums.filter { it.owned }.mapNotNull { it.tomeNumber }.toSet()
        val max = numbered.max()
        return (1..max).filter { it !in owned }
    }

    /** Numéro de tome suivant, pré-rempli pour "+ Ajouter un tome". */
    fun nextTomeNumber(albums: List<Album>): Int =
        (albums.mapNotNull { it.tomeNumber }.maxOrNull() ?: 0) + 1
}
