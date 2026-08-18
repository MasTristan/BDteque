package com.bdshelf.app.domain.catalog

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogBundleVerifierTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val publicKey = "test-key".toByteArray()

    private class FakeSignatureVerifier(private val result: Boolean) : ManifestSignatureVerifier {
        var callCount = 0
            private set

        override fun verify(manifestBytes: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
            callCount++
            return result
        }
    }

    /**
     * Corps synthétique valide : au-dessus du seuil de vraisemblance
     * ([CatalogPlausibility], 1000 séries minimum), sans dépendre de vraies
     * données BnF. L'empreinte est calculée avec la vraie fonction de
     * production ([CatalogChecksum]) — ce n'est pas elle qui est testée ici
     * (voir [CatalogChecksumTest]), seulement l'enchaînement des étapes.
     */
    private fun syntheticBody(seriesCount: Int, albumsPerSeries: Int): String = buildString {
        repeat(seriesCount) { i ->
            appendLine("""{"t":"s","id":"serie-$i","title":"Série $i","status":"ONGOING"}""")
            repeat(albumsPerSeries) { j ->
                appendLine("""{"t":"a","ean":"97800000${i.toString().padStart(3, '0')}${j}","seriesId":"serie-$i","tome":$j,"title":"Tome $j"}""")
            }
        }
    }

    private fun manifestFor(body: String, seriesCount: Int, albumCount: Int): CatalogManifest = CatalogManifest(
        version = "2026.11",
        schemaVersion = 1,
        builtAt = "2026-11-01T02:00:00Z",
        source = "Catalogue général de la Bibliothèque nationale de France — Licence Ouverte Etalab 2.0",
        sourceUpdatedAt = "2026-10-28",
        counts = CatalogCounts(series = seriesCount, albums = albumCount),
        body = CatalogBodyRef(file = "catalog-2026.11.jsonl", bytes = body.length.toLong(), sha256 = CatalogChecksum.sha256Hex(body.toByteArray())),
    )

    @Test
    fun `accepts a well-formed, correctly signed and checksummed bundle`() {
        val body = syntheticBody(seriesCount = 1200, albumsPerSeries = 1)
        val manifest = manifestFor(body, seriesCount = 1200, albumCount = 1200)
        val verifier = CatalogBundleVerifier(FakeSignatureVerifier(true), publicKey)

        val result = verifier.verify(
            manifestBytes = json.encodeToString(manifest).toByteArray(),
            manifestSignature = "irrelevant-to-the-fake".toByteArray(),
            bodyBytes = body.toByteArray(),
        )

        assertTrue(result is CatalogVerificationResult.Accepted)
        result as CatalogVerificationResult.Accepted
        assertEquals(1200, result.entities.count { it is CatalogBundleEntity.SeriesEntity })
        assertEquals(1200, result.entities.count { it is CatalogBundleEntity.AlbumEntity })
    }

    @Test
    fun `rejects an invalid signature before looking at anything else`() {
        val body = syntheticBody(seriesCount = 1200, albumsPerSeries = 1)
        val manifest = manifestFor(body, seriesCount = 1200, albumCount = 1200)
        val signatureVerifier = FakeSignatureVerifier(false)
        val verifier = CatalogBundleVerifier(signatureVerifier, publicKey)

        val result = verifier.verify(
            manifestBytes = json.encodeToString(manifest).toByteArray(),
            manifestSignature = "bad-signature".toByteArray(),
            bodyBytes = body.toByteArray(),
        )

        assertTrue(result is CatalogVerificationResult.Rejected)
        assertEquals(1, signatureVerifier.callCount)
    }

    @Test
    fun `rejects a body whose checksum does not match the manifest`() {
        val body = syntheticBody(seriesCount = 1200, albumsPerSeries = 1)
        val manifest = manifestFor(body, seriesCount = 1200, albumCount = 1200)
        val verifier = CatalogBundleVerifier(FakeSignatureVerifier(true), publicKey)

        val corruptedBody = body + "\n{\"t\":\"s\",\"id\":\"injected\",\"title\":\"Injected\",\"status\":\"ONGOING\"}"
        val result = verifier.verify(
            manifestBytes = json.encodeToString(manifest).toByteArray(),
            manifestSignature = "irrelevant-to-the-fake".toByteArray(),
            bodyBytes = corruptedBody.toByteArray(),
        )

        assertTrue(result is CatalogVerificationResult.Rejected)
        assertEquals(
            "Empreinte du corps du bundle incorrecte",
            (result as CatalogVerificationResult.Rejected).reason,
        )
    }

    @Test
    fun `rejects an unreadable manifest`() {
        val verifier = CatalogBundleVerifier(FakeSignatureVerifier(true), publicKey)

        val result = verifier.verify(
            manifestBytes = "not a json manifest".toByteArray(),
            manifestSignature = "irrelevant-to-the-fake".toByteArray(),
            bodyBytes = "irrelevant".toByteArray(),
        )

        assertTrue(result is CatalogVerificationResult.Rejected)
    }

    @Test
    fun `rejects a bundle failing plausibility despite a valid signature and checksum`() {
        // En dessous du seuil de vraisemblance (1000 séries minimum) : un
        // bundle authentique mais visiblement tronqué ne doit pas passer.
        val body = syntheticBody(seriesCount = 5, albumsPerSeries = 1)
        val manifest = manifestFor(body, seriesCount = 5, albumCount = 5)
        val verifier = CatalogBundleVerifier(FakeSignatureVerifier(true), publicKey)

        val result = verifier.verify(
            manifestBytes = json.encodeToString(manifest).toByteArray(),
            manifestSignature = "irrelevant-to-the-fake".toByteArray(),
            bodyBytes = body.toByteArray(),
        )

        assertTrue(result is CatalogVerificationResult.Rejected)
    }
}
