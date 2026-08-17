package com.bdshelf.app.domain.catalog

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

/**
 * Ce que ce test vérifie : que [Ed25519ManifestSignatureVerifier] enveloppe
 * correctement l'API Bouncy Castle (bon décalage de clé, bonne longueur,
 * `false` plutôt qu'une exception sur une entrée malformée). L'algorithme
 * Ed25519 lui-même n'est pas ce qui est mis à l'épreuve ici — c'est la
 * responsabilité de Bouncy Castle, choisie précisément pour cette raison
 * (§ADR-007). D'où une paire de clés générée dans le test plutôt qu'un
 * vecteur figé : ce qui compte est le comportement de la classe, pas une
 * valeur de signature particulière.
 */
class Ed25519ManifestSignatureVerifierTest {

    private val verifier = Ed25519ManifestSignatureVerifier()

    private fun generatePrivateKey(): Ed25519PrivateKeyParameters {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        return generator.generateKeyPair().private as Ed25519PrivateKeyParameters
    }

    private fun sign(privateKey: Ed25519PrivateKeyParameters, message: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, privateKey)
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    @Test
    fun `accepts a genuine signature from the matching key`() {
        val privateKey = generatePrivateKey()
        val publicKey = privateKey.generatePublicKey().encoded
        val message = """{"version":"2026.11"}""".toByteArray()
        val signature = sign(privateKey, message)

        assertTrue(verifier.verify(message, signature, publicKey))
    }

    @Test
    fun `rejects a signature over a different message`() {
        val privateKey = generatePrivateKey()
        val publicKey = privateKey.generatePublicKey().encoded
        val signature = sign(privateKey, """{"version":"2026.11"}""".toByteArray())

        val tamperedMessage = """{"version":"2026.12"}""".toByteArray()
        assertFalse(verifier.verify(tamperedMessage, signature, publicKey))
    }

    @Test
    fun `rejects a corrupted signature`() {
        val privateKey = generatePrivateKey()
        val publicKey = privateKey.generatePublicKey().encoded
        val message = """{"version":"2026.11"}""".toByteArray()
        val signature = sign(privateKey, message)
        signature[0] = signature[0].inc()

        assertFalse(verifier.verify(message, signature, publicKey))
    }

    @Test
    fun `rejects a signature verified against the wrong public key`() {
        val privateKey = generatePrivateKey()
        val message = """{"version":"2026.11"}""".toByteArray()
        val signature = sign(privateKey, message)

        val wrongPublicKey = generatePrivateKey().generatePublicKey().encoded
        assertFalse(verifier.verify(message, signature, wrongPublicKey))
    }

    @Test
    fun `returns false rather than throwing on a malformed public key`() {
        val message = "irrelevant".toByteArray()
        val signature = ByteArray(64)
        val tooShortKey = ByteArray(4)

        assertFalse(verifier.verify(message, signature, tooShortKey))
    }

    @Test
    fun `returns false rather than throwing on a malformed signature`() {
        val privateKey = generatePrivateKey()
        val publicKey = privateKey.generatePublicKey().encoded
        val message = """{"version":"2026.11"}""".toByteArray()
        val tooShortSignature = ByteArray(4)

        assertFalse(verifier.verify(message, tooShortSignature, publicKey))
    }
}
