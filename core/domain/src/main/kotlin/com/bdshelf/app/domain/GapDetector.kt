package com.bdshelf.app.domain

/**
 * Ce qu'un calcul de trous a besoin de savoir sur un tome — rien de plus
 * (§ADR-002) : le domaine ne connaît pas l'entité Room `Album`. Côté
 * application, `List<Album>.tomeGaps()` fait la conversion à la frontière
 * (voir `app/src/main/java/com/bdshelf/app/domain/GapsSupport.kt`).
 */
data class TomeOwnership(val tomeNumber: Int?, val owned: Boolean)

/** Détection des trous de collection à partir des tomes connus d'une série. */
object GapDetector {

    /**
     * Numéros de tome manquants entre 1 et le plus grand numéro connu,
     * possédés ou non en base (un trou "implicite" sans tome associé compte
     * aussi comme manquant).
     */
    fun gaps(tomes: List<TomeOwnership>): List<Int> {
        val numbered = tomes.mapNotNull { it.tomeNumber }
        if (numbered.isEmpty()) return emptyList()
        val owned = tomes.filter { it.owned }.mapNotNull { it.tomeNumber }.toSet()
        val max = numbered.max()
        return (1..max).filter { it !in owned }
    }

    /**
     * Version « catalogue » de [gaps] (§E1 §4.7) : un trou interne (jamais
     * possédé, entre 1 et le plus grand tome connu en base) n'est pas la même
     * information qu'un retard (paru après, connu du catalogue, mais absent
     * de la base — [gaps] ne peut rien en dire, elle ne voit que ce qui a
     * déjà été saisi).
     *
     * [catalogTomeCount] = null restitue exactement [gaps] dans `internal`,
     * `ahead` toujours vide : c'est le comportement actuel tant qu'aucune
     * série n'est rattachée à un catalogue.
     */
    fun gapReport(tomes: List<TomeOwnership>, catalogTomeCount: Int? = null): GapReport {
        val internal = gaps(tomes)
        val maxKnown = tomes.mapNotNull { it.tomeNumber }.maxOrNull() ?: 0
        val ahead = if (catalogTomeCount != null && catalogTomeCount > maxKnown) {
            (maxKnown + 1..catalogTomeCount).toList()
        } else {
            emptyList()
        }
        return GapReport(internal = internal, ahead = ahead)
    }

    /** Numéro de tome suivant, pré-rempli pour "+ Ajouter un tome". */
    fun nextTomeNumber(tomes: List<TomeOwnership>): Int =
        (tomes.mapNotNull { it.tomeNumber }.maxOrNull() ?: 0) + 1
}

/**
 * Les deux familles de manquants d'une série (§E1 §4.7) : un trou ne se
 * comble qu'en achetant un tome précis qu'on sait déjà exister ;
 * un retard se comble en découvrant qu'un tome est paru. Les confondre en un
 * seul nombre affaiblirait l'écran le plus utile de l'application.
 */
data class GapReport(
    val internal: List<Int>,
    val ahead: List<Int>,
)
