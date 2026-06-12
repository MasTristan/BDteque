package com.bdshelf.app.domain

import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.data.local.entities.SeriesStatus
import com.bdshelf.app.data.repo.CollectionSnapshot

/** Export CSV lisible de la collection, pour consultation hors de l'app (§6.9). */
fun CollectionSnapshot.toCsv(): String = buildString {
    appendLine("Série,Statut,Suivi,Tome,Titre,Possédé,Statut de lecture,Édition,Code-barres")
    val seriesById = series.associateBy { it.id }
    val sortedAlbums = albums.sortedWith(
        compareBy(
            { seriesById[it.seriesId]?.title ?: it.seriesId },
            { it.tomeNumber ?: Int.MAX_VALUE },
        ),
    )
    for (album in sortedAlbums) {
        val s = seriesById[album.seriesId]
        val row = listOf(
            s?.title ?: album.seriesId,
            s?.status?.toFrench() ?: "",
            if (s?.isTracked == true) "oui" else "non",
            album.tomeNumber?.toString() ?: "hors-série",
            album.title ?: "",
            if (album.owned) "oui" else "non",
            album.readStatus.toFrench(),
            album.edition ?: "",
            album.ean ?: "",
        )
        appendLine(row.joinToString(",") { it.csvEscape() })
    }
}

private fun SeriesStatus.toFrench(): String = when (this) {
    SeriesStatus.ONGOING -> "En cours"
    SeriesStatus.FINISHED -> "Terminée"
    SeriesStatus.UNKNOWN -> "Statut inconnu"
}

private fun ReadStatus.toFrench(): String = when (this) {
    ReadStatus.UNREAD -> "Non lu"
    ReadStatus.READ -> "Lu"
    ReadStatus.LENT -> "Prêté"
}

private fun String.csvEscape(): String =
    if (any { it == ',' || it == '"' || it == '\n' }) {
        "\"" + replace("\"", "\"\"") + "\""
    } else {
        this
    }
