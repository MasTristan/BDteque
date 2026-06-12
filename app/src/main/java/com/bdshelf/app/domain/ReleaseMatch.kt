package com.bdshelf.app.domain

import com.bdshelf.app.data.remote.ReleaseItem

/** Une sortie d'une série suivie, avec l'information "déjà possédé" (§4.3 SPEC). */
data class ReleaseWithOwnership(
    val release: ReleaseItem,
    val owned: Boolean,
)

/**
 * Croise les sorties (Moitié B) avec la collection (Moitié A) sans jamais les
 * mélanger en base : calcul pur, à partir d'instantanés des deux côtés.
 *
 * Ne garde que les sorties des séries suivies, et indique pour chacune si le
 * tome correspondant est déjà possédé.
 */
fun crossReferenceReleases(
    releases: List<ReleaseItem>,
    trackedSeriesIds: Set<String>,
    ownedTomes: Set<Pair<String, Int>>,
): List<ReleaseWithOwnership> = releases
    .filter { it.seriesId in trackedSeriesIds }
    .map { ReleaseWithOwnership(it, owned = (it.seriesId to it.tomeNumber) in ownedTomes) }

/** Nombre de nouveautés à signaler, c'est-à-dire pas encore possédées (§6.2). */
fun List<ReleaseWithOwnership>.missingCount(): Int = count { !it.owned }
