package com.bdshelf.app.domain.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogChecksumTest {

    // Vecteurs SHA-256 de référence (calculés indépendamment, pas par le code testé).
    private val helloSha256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"

    @Test
    fun `sha256Hex matches a known reference vector`() {
        assertTrue(CatalogChecksum.matches("hello".toByteArray(), helloSha256))
    }

    @Test
    fun `matches accepts an sha256 colon prefix`() {
        assertTrue(CatalogChecksum.matches("hello".toByteArray(), "sha256:$helloSha256"))
    }

    @Test
    fun `matches is case insensitive`() {
        assertTrue(CatalogChecksum.matches("hello".toByteArray(), helloSha256.uppercase()))
    }

    @Test
    fun `matches rejects a mismatched checksum`() {
        assertFalse(CatalogChecksum.matches("hello".toByteArray(), helloSha256.replaceRange(0, 1, "0")))
    }

    @Test
    fun `matches rejects a single-byte corruption`() {
        val original = "hello".toByteArray()
        val corrupted = original.copyOf().also { it[0] = it[0].inc() }
        assertFalse(CatalogChecksum.matches(corrupted, helloSha256))
    }

    @Test
    fun `sha256Hex is 64 lowercase hex characters`() {
        val hex = CatalogChecksum.sha256Hex("hello".toByteArray())
        assertTrue(hex.matches(Regex("[0-9a-f]{64}")))
    }
}
