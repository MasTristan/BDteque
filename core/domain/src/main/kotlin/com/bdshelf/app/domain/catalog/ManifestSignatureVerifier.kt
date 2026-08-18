package com.bdshelf.app.domain.catalog

/**
 * Vérification de signature du manifeste (§ADR-007, §E1 §4.5 étape 3).
 *
 * Interface séparée de son implémentation ([Ed25519ManifestSignatureVerifier],
 * Bouncy Castle — Android ne fournit Ed25519 nativement qu'à partir de
 * l'API 33, incompatible avec `minSdk = 26`) : ça permet de tester toute la
 * chaîne de confiance ([CatalogBundleVerifier]) avec un faux vérificateur,
 * indépendamment de la bibliothèque de signature elle-même — voir
 * `CatalogBundleVerifierTest`.
 */
fun interface ManifestSignatureVerifier {
    fun verify(manifestBytes: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
}
