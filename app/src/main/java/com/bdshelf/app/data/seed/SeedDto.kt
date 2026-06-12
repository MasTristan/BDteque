package com.bdshelf.app.data.seed

import kotlinx.serialization.Serializable

/** Miroir de `assets/seed-collection.json` (§JSON_SCHEMAS). */
@Serializable
data class SeedDocument(
    val version: Int,
    val series: List<SeedSeries>,
)

@Serializable
data class SeedSeries(
    val id: String,
    val title: String,
    val status: String,
    val tracked: Boolean,
    val knownTomeCount: Int? = null,
    val ownedTomes: List<Int> = emptyList(),
    val notes: String = "",
)
