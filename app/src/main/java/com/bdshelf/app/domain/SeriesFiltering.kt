package com.bdshelf.app.domain

import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.data.local.dao.SeriesWithCounts

/** Filtres rapides de la liste des séries (§6.5). */
enum class SeriesFilter {
    ALL, // toutes les séries
    INCOMPLETE, // séries avec au moins un tome manquant ("à compléter")
    UNREAD, // séries avec au moins un tome possédé non lu ("à lire")
}

/** Ordres de tri de la liste des séries (§6.5). */
enum class SeriesSort {
    TITLE, // alphabétique
    COMPLETION, // les moins complètes d'abord
    RECENT, // dernier tome ajouté le plus récemment d'abord
}

/**
 * Recherche, filtre et trie la liste des séries (§6.5). Calcul pur en mémoire :
 * les collections restent petites (quelques centaines d'albums), et la
 * normalisation insensible aux accents ([normalizedForSearch]) n'existe pas
 * en SQL.
 *
 * La recherche couvre le titre de la série ET les titres de ses albums : taper
 * « Aniel » retrouve Thorgal même si l'on ne se souvient plus de la série.
 */
fun filterAndSortSeries(
    series: List<SeriesWithCounts>,
    albums: List<Album>,
    query: String,
    filter: SeriesFilter,
    sort: SeriesSort,
): List<SeriesWithCounts> {
    val albumsBySeries = albums.groupBy { it.seriesId }

    val normalizedQuery = query.normalizedForSearch().trim()
    val searched = if (normalizedQuery.isEmpty()) {
        series
    } else {
        series.filter { s ->
            s.title.normalizedForSearch().contains(normalizedQuery) ||
                albumsBySeries[s.id].orEmpty().any {
                    it.title?.normalizedForSearch()?.contains(normalizedQuery) == true
                }
        }
    }

    val filtered = when (filter) {
        SeriesFilter.ALL -> searched
        SeriesFilter.INCOMPLETE -> searched.filter { it.hasGap }
        SeriesFilter.UNREAD -> searched.filter { s ->
            albumsBySeries[s.id].orEmpty().any { it.owned && it.readStatus == ReadStatus.UNREAD }
        }
    }

    return when (sort) {
        SeriesSort.TITLE -> filtered.sortedBy { it.title.normalizedForSearch() }
        // Ratio de complétion croissant : les étagères les plus trouées en tête.
        // Les séries sans aucun album (ratio indéfini) vont en fin de liste.
        SeriesSort.COMPLETION -> filtered.sortedWith(
            compareBy(
                { if (it.totalCount == 0) 2.0 else it.ownedCount.toDouble() / it.totalCount },
                { it.title.normalizedForSearch() },
            ),
        )
        // Dernier ajout possédé le plus récent d'abord ; séries jamais alimentées en fin.
        SeriesSort.RECENT -> filtered.sortedWith(
            compareByDescending<SeriesWithCounts> { s ->
                albumsBySeries[s.id].orEmpty().filter { it.owned }.maxOfOrNull { it.dateAdded } ?: Long.MIN_VALUE
            }.thenBy { it.title.normalizedForSearch() },
        )
    }
}
