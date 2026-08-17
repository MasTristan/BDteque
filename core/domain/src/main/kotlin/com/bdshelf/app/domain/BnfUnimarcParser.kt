package com.bdshelf.app.domain

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Extrait un [IsbnBook] d'une réponse SRU UNIMARC du catalogue BnF (§6.4).
 *
 * Zones UNIMARC lues sur chaque notice bibliographique :
 * - 200$a = titre de l'album, 200$e = complément de titre (sous-titre) ;
 *   pour les notices multi-volumes, 200$i = titre de partie (le vrai titre de
 *   l'album, 200$a étant alors la série) et 200$h = numéro de partie (tome),
 * - 225$a = titre de collection (série), 225$v = numéro dans la collection,
 * - 461$t / 461$v = ensemble englobant (série / tome), repli fréquent quand
 *   la zone 225 est absente de la notice,
 * - 700/701/702 = auteurs (b = prénom, a = nom).
 *
 * Quand la réponse contient plusieurs notices pour un même ISBN (rééditions,
 * tirages), on retient la plus complète : série + tome > série seule > titre
 * seul, à égalité la première (ordre de pertinence BnF).
 *
 * Fonction pure et sans dépendance Android (parseur DOM standard), donc
 * testable en JVM sur des notices réelles. Retourne `null` si le XML est
 * illisible ou qu'aucune notice n'a de titre.
 */
fun parseBnfUnimarc(xml: String): IsbnBook? {
    val document = runCatching {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            // Durcissement XXE : la source est de confiance (BnF, HTTPS) mais on
            // désactive les entités externes par principe. Ignoré si non supporté.
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        }
        factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
    }.getOrNull() ?: return null

    val records = document.getElementsByTagName("mxc:record")
    var best: IsbnBook? = null
    var bestScore = -1
    for (i in 0 until records.length) {
        val record = records.item(i) as? Element ?: continue
        val book = parseRecord(record) ?: continue
        val score = (if (book.seriesName != null) 2 else 0) + (if (book.tomeNumber != null) 1 else 0)
        if (score > bestScore) {
            best = book
            bestScore = score
        }
        if (bestScore == 3) break // série + tome : rien de mieux à espérer
    }
    return best
}

private fun parseRecord(record: Element): IsbnBook? {
    val datafields = record.getElementsByTagName("mxc:datafield")

    var title: String? = null
    var subtitle: String? = null
    var partTitle: String? = null
    var partNumber: Int? = null
    var collectionName: String? = null
    var collectionNumber: Int? = null
    var setName: String? = null
    var setNumber: Int? = null
    val authors = mutableListOf<String>()

    for (i in 0 until datafields.length) {
        val field = datafields.item(i) as? Element ?: continue
        when (field.getAttribute("tag")) {
            "200" -> {
                if (title == null) title = field.subfield("a")
                if (subtitle == null) subtitle = field.subfield("e")
                if (partTitle == null) partTitle = field.subfield("i")
                if (partNumber == null) partNumber = field.subfield("h")?.let(::leadingInt)
            }
            "225" -> {
                // Première collection = collection principale (série de l'album).
                if (collectionName == null) collectionName = field.subfield("a")
                if (collectionNumber == null) collectionNumber = field.subfield("v")?.let(::leadingInt)
            }
            "461" -> {
                if (setName == null) setName = field.subfield("t")
                if (setNumber == null) setNumber = field.subfield("v")?.let(::leadingInt)
            }
            "700", "701", "702" -> {
                val surname = field.subfield("a") ?: continue
                val given = field.subfield("b")
                authors += listOfNotNull(given, surname).joinToString(" ").clean()
            }
        }
    }

    // Notice multi-volumes "Série. N, Titre" : 200$i est le titre de l'album,
    // 200$a bascule alors comme repli de série (après 225 et 461).
    val cleanPartTitle = partTitle?.clean()?.ifBlank { null }
    val cleanMainTitle = title?.clean()?.ifBlank { null }
    val cleanSubtitle = subtitle?.clean()?.ifBlank { null }

    val albumTitle: String?
    val titleAsSeries: String?
    if (cleanPartTitle != null) {
        albumTitle = cleanPartTitle
        titleAsSeries = cleanMainTitle
    } else {
        albumTitle = if (cleanMainTitle != null && cleanSubtitle != null) "$cleanMainTitle : $cleanSubtitle" else cleanMainTitle
        titleAsSeries = null
    }
    if (albumTitle == null) return null

    return IsbnBook(
        title = albumTitle,
        seriesName = (collectionName ?: setName ?: titleAsSeries)?.clean()?.ifBlank { null },
        tomeNumber = collectionNumber ?: setNumber ?: partNumber,
        authors = authors.filter { it.isNotBlank() }.distinct(),
    )
}

/** Premier sous-champ portant le code [code] dans un datafield UNIMARC. */
private fun Element.subfield(code: String): String? {
    val subfields = getElementsByTagName("mxc:subfield")
    for (i in 0 until subfields.length) {
        val sub = subfields.item(i) as? Element ?: continue
        if (sub.getAttribute("code") == code) return sub.textContent
    }
    return null
}

/** Entier en tête de chaîne ("36", "1re" -> 1), ou `null` si aucun chiffre initial. */
private fun leadingInt(value: String): Int? =
    value.trimStart().takeWhile { it.isDigit() }.toIntOrNull()

/**
 * Caractères de contrôle de tri UNIMARC : U+0088 (début de partie non triable)
 * et U+0089 (fin). Référencés par point de code pour garder le source en ASCII.
 */
private val NON_SORT_CHARS = setOf(Char(0x88), Char(0x89))

/** Ponctuation ISBD résiduelle en fin de champ (" :", " /", " ;", " =", ","). */
private val TRAILING_ISBD = Regex("""\s*[:/;=,]\s*$""")

/**
 * Retire les caractères de contrôle de tri UNIMARC, la ponctuation ISBD
 * pendante en fin de champ, et normalise les espaces.
 */
private fun String.clean(): String =
    filterNot { it in NON_SORT_CHARS }
        .replace(Regex("\\s+"), " ")
        .trim()
        .replace(TRAILING_ISBD, "")
