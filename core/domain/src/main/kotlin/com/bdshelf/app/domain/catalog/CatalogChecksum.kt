package com.bdshelf.app.domain.catalog

import java.security.MessageDigest

/** Empreinte du corps d'un bundle (§E1 §4.5, étape 5) : détecte un corps corrompu ou tronqué avant tout import. */
object CatalogChecksum {

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    /** [expectedHex] accepté avec ou sans préfixe `sha256:`, insensible à la casse. */
    fun matches(bytes: ByteArray, expectedHex: String): Boolean =
        sha256Hex(bytes).equals(expectedHex.removePrefix("sha256:"), ignoreCase = true)
}
