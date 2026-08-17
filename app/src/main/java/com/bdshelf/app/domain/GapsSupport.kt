package com.bdshelf.app.domain

import com.bdshelf.app.data.local.entities.Album

/**
 * Pont entre les albums Room et [GapDetector] (§ADR-002) : le domaine ne
 * connaît pas l'entité `Album`, seulement ce qui lui est nécessaire
 * ([TomeOwnership]). Cette conversion vit côté application, à la frontière.
 */
private fun Album.toTomeOwnership() = TomeOwnership(tomeNumber, owned)

fun List<Album>.tomeGaps(): List<Int> = GapDetector.gaps(map { it.toTomeOwnership() })

fun List<Album>.nextTomeNumber(): Int = GapDetector.nextTomeNumber(map { it.toTomeOwnership() })
