package com.bdshelf.app.domain.catalog

import kotlin.math.abs

/** Verdict de vraisemblance d'un bundle (§E1 §4.5, étape 7), avec le motif si rejeté. */
data class PlausibilityResult(val plausible: Boolean, val reason: String?) {
    companion object {
        fun ok() = PlausibilityResult(true, null)
        fun rejected(reason: String) = PlausibilityResult(false, reason)
    }
}

/**
 * Derniers contrôles avant bascule (§E1 §4.5) : un bundle qui a passé la
 * signature et l'empreinte peut quand même être un contenu légitime mais
 * absurde — un manifeste signé pour la mauvaise version, un corps tronqué
 * dont le SHA-256 collerait par accident à un ancien manifeste réutilisé.
 * Ni la signature ni l'empreinte seules ne détectent ce cas.
 */
object CatalogPlausibility {
    private const val MIN_SERIES = 1_000
    private const val COUNT_TOLERANCE = 0.05

    fun check(manifestCounts: CatalogCounts, actualSeriesCount: Int, actualAlbumCount: Int): PlausibilityResult {
        if (actualSeriesCount == 0 || actualAlbumCount == 0) {
            return PlausibilityResult.rejected("Table vide (séries=$actualSeriesCount, albums=$actualAlbumCount)")
        }
        if (actualSeriesCount < MIN_SERIES) {
            return PlausibilityResult.rejected("Seulement $actualSeriesCount séries (minimum $MIN_SERIES)")
        }
        if (!withinTolerance(actualSeriesCount, manifestCounts.series)) {
            return PlausibilityResult.rejected(
                "Nombre de séries hors tolérance : $actualSeriesCount contre ${manifestCounts.series} annoncées",
            )
        }
        if (!withinTolerance(actualAlbumCount, manifestCounts.albums)) {
            return PlausibilityResult.rejected(
                "Nombre d'albums hors tolérance : $actualAlbumCount contre ${manifestCounts.albums} annoncés",
            )
        }
        return PlausibilityResult.ok()
    }

    private fun withinTolerance(actual: Int, expected: Int): Boolean {
        if (expected <= 0) return actual == 0
        return abs(actual - expected).toDouble() / expected <= COUNT_TOLERANCE
    }
}
