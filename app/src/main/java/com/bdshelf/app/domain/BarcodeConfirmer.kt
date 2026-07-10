package com.bdshelf.app.domain

/**
 * Confirmation multi-images d'un code-barres (§6.3), partagée entre le scan
 * simple et le mode inventaire.
 *
 * Deux garde-fous de fiabilité avant d'accepter une lecture caméra :
 * - somme de contrôle EAN vérifiée ([canonicalEan]) — une image floue peut
 *   faire lire un chiffre de travers à ML Kit ;
 * - le même code doit être lu sur [requiredReads] images consécutives : deux
 *   lectures erronées identiques d'affilée sont improbables, et à ~10-30
 *   images/s la confirmation reste imperceptible.
 *
 * Le compteur se réarme après chaque confirmation : en mode inventaire, le
 * même code re-confirme régulièrement tant qu'il est visé (l'appelant
 * dédoublonne), et un nouveau livre repart de zéro.
 */
class BarcodeConfirmer(private val requiredReads: Int = DEFAULT_REQUIRED_READS) {

    private var candidate: String? = null
    private var reads = 0

    /** Offre une lecture brute ; retourne l'EAN canonique une fois confirmé, sinon `null`. */
    fun offer(raw: String): String? {
        val ean = canonicalEan(raw) ?: return null
        if (ean == candidate) {
            reads++
        } else {
            candidate = ean
            reads = 1
        }
        if (reads < requiredReads) return null
        reset()
        return ean
    }

    fun reset() {
        candidate = null
        reads = 0
    }

    companion object {
        const val DEFAULT_REQUIRED_READS = 3
    }
}
