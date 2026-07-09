package com.bdshelf.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Une série suivie ou possédée. */
@Serializable
@Entity(tableName = "series")
data class Series(
    @PrimaryKey val id: String, // slug stable, ex: "buck-danny-classic"
    val title: String,
    val status: SeriesStatus,
    val isTracked: Boolean, // alimente l'écran "À paraître"
    val color: Long, // couleur de tranche (ARGB), voir SpineColor.kt
    val knownTomeCount: Int?, // nb total connu de tomes, null si série ouverte
    val notes: String?,
)
