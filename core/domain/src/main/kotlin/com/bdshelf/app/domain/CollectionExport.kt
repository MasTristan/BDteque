package com.bdshelf.app.domain

/**
 * Une ligne d'export CSV de la collection (§ADR-002) : le domaine ne connaît
 * ni `Album`, ni `Series`, ni `CollectionSnapshot` — seulement des champs déjà
 * mis en forme textuelle (libellés français inclus) à la frontière, côté app.
 */
data class ExportRow(
    val seriesTitle: String,
    val seriesStatus: String,
    val seriesTracked: Boolean,
    val tomeNumber: Int?,
    val albumTitle: String?,
    val owned: Boolean,
    val readStatus: String,
    val edition: String?,
    val ean: String?,
)

private const val CSV_HEADER = "Série,Statut,Suivi,Tome,Titre,Possédé,Statut de lecture,Édition,Code-barres"

/** Export CSV lisible de la collection, pour consultation hors de l'app (§6.9). */
fun buildCollectionCsv(rows: List<ExportRow>): String = buildString {
    appendLine(CSV_HEADER)
    val sorted = rows.sortedWith(compareBy({ it.seriesTitle }, { it.tomeNumber ?: Int.MAX_VALUE }))
    for (row in sorted) {
        val line = listOf(
            row.seriesTitle,
            row.seriesStatus,
            if (row.seriesTracked) "oui" else "non",
            row.tomeNumber?.toString() ?: "hors-série",
            row.albumTitle ?: "",
            if (row.owned) "oui" else "non",
            row.readStatus,
            row.edition ?: "",
            row.ean ?: "",
        )
        appendLine(line.joinToString(",") { it.csvEscape() })
    }
}

private fun String.csvEscape(): String =
    if (any { it == ',' || it == '"' || it == '\n' }) {
        "\"" + replace("\"", "\"\"") + "\""
    } else {
        this
    }
