package com.bdshelf.app.domain

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Extrait un [IsbnBook] d'une réponse SRU UNIMARC du catalogue BnF (§6.4).
 *
 * Zones UNIMARC lues sur la première notice bibliographique :
 * - 200$a = titre de l'album, 200$e = complément de titre (sous-titre),
 * - 225$a = titre de collection (série), 225$v = numéro dans la collection (tome),
 * - 700/701/702 = auteurs (b = prénom, a = nom).
 *
 * Fonction pure et sans dépendance Android (parseur DOM standard), donc
 * testable en JVM sur des notices réelles. Retourne `null` si le XML est
 * illisible ou sans titre.
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

    val record = document.getElementsByTagName("mxc:record").item(0) as? Element ?: return null
    val datafields = record.getElementsByTagName("mxc:datafield")

    var title: String? = null
    var subtitle: String? = null
    var seriesName: String? = null
    var tomeNumber: Int? = null
    val authors = mutableListOf<String>()

    for (i in 0 until datafields.length) {
        val field = datafields.item(i) as? Element ?: continue
        when (field.getAttribute("tag")) {
            "200" -> {
                if (title == null) title = field.subfield("a")
                if (subtitle == null) subtitle = field.subfield("e")
            }
            "225" -> {
                // Première collection = collection principale (série de l'album).
                if (seriesName == null) seriesName = field.subfield("a")
                if (tomeNumber == null) tomeNumber = field.subfield("v")?.let(::leadingInt)
            }
            "700", "701", "702" -> {
                val surname = field.subfield("a") ?: continue
                val given = field.subfield("b")
                authors += listOfNotNull(given, surname).joinToString(" ").clean()
            }
        }
    }

    val cleanTitle = title?.clean()?.ifBlank { null } ?: return null
    val cleanSubtitle = subtitle?.clean()?.ifBlank { null }
    val fullTitle = if (cleanSubtitle != null) "$cleanTitle : $cleanSubtitle" else cleanTitle

    return IsbnBook(
        title = fullTitle,
        seriesName = seriesName?.clean()?.ifBlank { null },
        tomeNumber = tomeNumber,
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

/** Retire les caractères de contrôle de tri UNIMARC et normalise les espaces. */
private fun String.clean(): String =
    filterNot { it in NON_SORT_CHARS }
        .replace(Regex("\\s+"), " ")
        .trim()
