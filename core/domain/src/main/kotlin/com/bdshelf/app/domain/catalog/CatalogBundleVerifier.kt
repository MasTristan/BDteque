package com.bdshelf.app.domain.catalog

import kotlinx.serialization.json.Json

/** Issue de la chaîne de confiance d'un bundle (§E1 §4.5) : soit un contenu digne d'import, soit un motif de rejet en clair. */
sealed interface CatalogVerificationResult {
    data class Accepted(val manifest: CatalogManifest, val entities: List<CatalogBundleEntity>) : CatalogVerificationResult
    data class Rejected(val reason: String) : CatalogVerificationResult
}

/**
 * Chaîne de confiance d'un bundle catalogue (§E1 §4.5, étapes 3, 5, 6, 7) :
 * signature, empreinte, analyse, vraisemblance — dans cet ordre, chacune
 * pouvant arrêter la chaîne sans jamais toucher le catalogue en service.
 *
 * Volontairement hors de cette classe : le téléchargement (étapes 1-2, 4)
 * et la bascule atomique en base (étape 8), qui appartiennent à la couche
 * données (Context, réseau, Room) — cette classe ne fait que juger un
 * contenu déjà en mémoire, ce qui la rend testable sans Android.
 */
class CatalogBundleVerifier(
    private val signatureVerifier: ManifestSignatureVerifier,
    private val publicKey: ByteArray,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun verify(manifestBytes: ByteArray, manifestSignature: ByteArray, bodyBytes: ByteArray): CatalogVerificationResult {
        if (!signatureVerifier.verify(manifestBytes, manifestSignature, publicKey)) {
            return CatalogVerificationResult.Rejected("Signature du manifeste invalide")
        }

        val manifest = runCatching { json.decodeFromString<CatalogManifest>(manifestBytes.decodeToString()) }
            .getOrElse { return CatalogVerificationResult.Rejected("Manifeste illisible") }

        if (!CatalogChecksum.matches(bodyBytes, manifest.body.sha256)) {
            return CatalogVerificationResult.Rejected("Empreinte du corps du bundle incorrecte")
        }

        val parsed = CatalogBundleParser.parse(bodyBytes.decodeToString().lineSequence())
        val seriesCount = parsed.entities.count { it is CatalogBundleEntity.SeriesEntity }
        val albumCount = parsed.entities.count { it is CatalogBundleEntity.AlbumEntity }

        val plausibility = CatalogPlausibility.check(manifest.counts, seriesCount, albumCount)
        if (!plausibility.plausible) {
            return CatalogVerificationResult.Rejected(plausibility.reason ?: "Bundle invraisemblable")
        }

        return CatalogVerificationResult.Accepted(manifest, parsed.entities)
    }
}
