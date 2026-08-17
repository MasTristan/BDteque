package com.bdshelf.app.domain.catalog

/**
 * Vérification de signature du manifeste (§ADR-007, §E1 §4.5 étape 3).
 *
 * Interface volontairement séparée de son implémentation : Android ne
 * fournit Ed25519 nativement (`java.security.Signature.getInstance("Ed25519")`,
 * via Conscrypt) qu'à partir de l'API 33, incompatible avec `minSdk = 26`.
 * Une bibliothèque pure JVM (Bouncy Castle, Google Tink…) sera nécessaire
 * pour couvrir 26-32 — choix encore ouvert, avec un impact réel sur le
 * budget de taille de l'APK (§specs/README.md, < 30 Mo hors catalogue).
 *
 * Cette séparation permet de tester toute la chaîne de confiance
 * ([CatalogBundleVerifier]) dès maintenant avec un faux vérificateur,
 * indépendamment de cette décision — voir les tests de
 * `CatalogBundleVerifierTest`.
 */
fun interface ManifestSignatureVerifier {
    fun verify(manifestBytes: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
}
