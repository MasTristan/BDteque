package com.bdshelf.app.domain.catalog

import kotlinx.serialization.Serializable

/**
 * Manifeste d'un bundle catalogue (§E1 §4.4, format détaillé dans
 * `docs/specs/CONTRATS-DE-DONNEES.md` §5.2). Signé séparément — voir
 * [ManifestSignatureVerifier] — ce fichier ne décrit que sa structure.
 */
@Serializable
data class CatalogManifest(
    val version: String,
    val schemaVersion: Int,
    val builtAt: String,
    val source: String,
    val sourceUpdatedAt: String,
    val counts: CatalogCounts,
    val body: CatalogBodyRef,
    val supersedes: String? = null,
)

@Serializable
data class CatalogCounts(val series: Int, val albums: Int)

@Serializable
data class CatalogBodyRef(val file: String, val bytes: Long, val sha256: String)
