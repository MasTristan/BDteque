package com.bdshelf.app.domain.catalog

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

/**
 * Implémentation Bouncy Castle de [ManifestSignatureVerifier] (§ADR-007).
 *
 * API légère (`org.bouncycastle.crypto.*`), volontairement pas la variante
 * `org.bouncycastle.jce.provider.BouncyCastleProvider` : s'enregistrer comme
 * `java.security.Provider` entrerait en conflit avec le provider BC allégé
 * qu'Android embarque déjà (source classique de `NoSuchAlgorithmException`
 * ou de comportements surprenants selon l'ordre d'enregistrement). Cette
 * classe n'a besoin que de vérifier une signature, jamais d'être un
 * `Provider` global.
 *
 * Choisi (plutôt que Tink) pour accepter directement une clé publique
 * Ed25519 brute de 32 octets — le format déjà retenu pour
 * `BuildConfig.CATALOG_PUBLIC_KEY` — sans imposer son propre format de
 * trousseau de clés.
 */
class Ed25519ManifestSignatureVerifier : ManifestSignatureVerifier {

    override fun verify(manifestBytes: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean = runCatching {
        val verifier = Ed25519Signer()
        verifier.init(false, Ed25519PublicKeyParameters(publicKey, 0))
        verifier.update(manifestBytes, 0, manifestBytes.size)
        verifier.verifySignature(signature)
    }.getOrDefault(false)
}
