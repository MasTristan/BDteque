package com.bdshelf.app.data.remote

import kotlinx.serialization.Serializable

/** Miroir partiel de la réponse Open Library `jscmd=data` (§6.4, source de repli). */
@Serializable
data class OpenLibraryBook(
    val title: String? = null,
    val authors: List<OpenLibraryAuthor> = emptyList(),
)

@Serializable
data class OpenLibraryAuthor(
    val name: String? = null,
)
