package com.bdshelf.app.data.repo

import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.Series
import kotlinx.serialization.Serializable

/** Export/import complet de la collection (§6.9). */
@Serializable
data class CollectionSnapshot(
    val version: Int = 1,
    val series: List<Series>,
    val albums: List<Album>,
)
