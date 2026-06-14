package com.bdshelf.app.data.remote

import kotlinx.serialization.Serializable

/** Miroir partiel de la réponse Google Books Volumes API (§6.4 SPEC, enrichissement optionnel). */
@Serializable
data class GoogleBooksResponse(
    val items: List<GoogleBooksItem> = emptyList(),
)

@Serializable
data class GoogleBooksItem(
    val volumeInfo: GoogleBooksVolumeInfo? = null,
)

@Serializable
data class GoogleBooksVolumeInfo(
    val title: String? = null,
    val subtitle: String? = null,
)
