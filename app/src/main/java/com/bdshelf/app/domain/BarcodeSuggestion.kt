package com.bdshelf.app.domain

private val TOME_NUMBER_PATTERN = Regex(
    """(?:tome|t\.?)\s*#?\s*(\d{1,3})|#(\d{1,3})""",
    RegexOption.IGNORE_CASE,
)

/**
 * Extrait un numéro de tome d'un texte libre (titre/sous-titre Google Books).
 * Best-effort : reconnaît "Tome 12", "T12", "T. 12", "#12". Retourne `null` si
 * rien de plausible n'est trouvé (mieux vaut aucune suggestion qu'une fausse).
 */
fun guessTomeNumber(text: String): Int? {
    val match = TOME_NUMBER_PATTERN.find(text) ?: return null
    val digits = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: return null
    return digits.toIntOrNull()
}

/**
 * Devine la série correspondant à un titre Google Books parmi les séries
 * connues de la collection (§6.4, suggestion de scan).
 *
 * Les fiches Google Books pour la BD commencent quasi systématiquement par le
 * nom de la série ("Thorgal - La Forteresse invisible", "Astérix, tome 1 :
 * Astérix le Gaulois"...). On retient la série candidate si son titre normalisé
 * apparaît comme sous-texte du titre Google, en préférant la correspondance la
 * plus longue en cas d'ambiguïté (évite qu'une série au nom très court
 * "masque" une série au nom plus précis).
 */
fun guessSeries(googleTitle: String, candidates: List<Pair<String, String>>): String? {
    val normalizedTitle = googleTitle.normalizedForSearch()
    return candidates
        .filter { (_, title) -> title.isNotBlank() && normalizedTitle.contains(title.normalizedForSearch()) }
        .maxByOrNull { (_, title) -> title.length }
        ?.first
}
