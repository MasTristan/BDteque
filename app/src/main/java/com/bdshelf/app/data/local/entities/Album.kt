package com.bdshelf.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Un album / tome.
 *
 * `owned = false` représente un trou connu de la collection : l'album existe
 * en base pour que l'étagère puisse afficher le vide correspondant.
 */
@Serializable
@Entity(
    tableName = "albums",
    foreignKeys = [
        ForeignKey(
            entity = Series::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("seriesId"),
        Index(value = ["ean"], unique = true),
    ],
)
data class Album(
    @PrimaryKey val id: String, // ex: "buck-danny-classic-7", hors-série: "${seriesId}-hs-${index}"
    val seriesId: String,
    val tomeNumber: Int?, // null possible (hors-série)
    val title: String?,
    val owned: Boolean,
    val readStatus: ReadStatus,
    val edition: String?, // "souple", "intégrale", "collector"...
    val ean: String?, // EAN appris au scan, unique, nullable
    val dateAdded: Long, // epoch millis
)
