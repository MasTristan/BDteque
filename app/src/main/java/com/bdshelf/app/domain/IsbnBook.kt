package com.bdshelf.app.domain

/**
 * Livre identifié à partir d'un ISBN scanné (§6.4).
 *
 * [title] est le titre de l'album (pas de la série). [seriesName] et
 * [tomeNumber] sont renseignés quand la source les expose de façon structurée
 * (catalogue BnF, zone UNIMARC 225), sinon `null`.
 */
data class IsbnBook(
    val title: String,
    val seriesName: String?,
    val tomeNumber: Int?,
    val authors: List<String>,
) {
    /** Auteurs affichables, au plus [max], joints par des virgules. */
    fun authorsLabel(max: Int = 2): String? =
        authors.take(max).joinToString(", ").ifBlank { null }
}
