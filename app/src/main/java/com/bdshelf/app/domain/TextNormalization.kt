package com.bdshelf.app.domain

import java.text.Normalizer

/** Normalisation pour une recherche insensible à la casse et aux accents (§6.5). */
fun String.normalizedForSearch(): String {
    val withoutAccents = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return withoutAccents.lowercase()
}
