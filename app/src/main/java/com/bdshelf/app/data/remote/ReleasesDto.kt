package com.bdshelf.app.data.remote

import kotlinx.serialization.Serializable

/** Miroir du fichier distant `releases.json` (§4.3 SPEC, Moitié B). */
@Serializable
data class ReleasesDocument(
    val version: Int,
    val updatedAt: String,
    val releases: List<ReleaseItem>,
)

@Serializable
data class ReleaseItem(
    val seriesId: String,
    val seriesTitle: String,
    val tomeNumber: Int,
    val title: String,
    val expectedDate: String,
    val status: String, // "UPCOMING" | "RELEASED"
    val note: String = "",
)
