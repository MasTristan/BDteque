package com.bdshelf.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bdshelf.app.R
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.ReadStatus

/**
 * Vue liste de l'étagère (§ADR-009, anomalie A-02) : mêmes tomes, mêmes
 * actions, même information que [Shelf] — numéro, titre, état en toutes
 * lettres — en défilement vertical.
 *
 * Ce n'est pas un « mode accessibilité » séparé mais une deuxième façon,
 * offerte à tout le monde, de voir la même vérité : l'étagère horizontale
 * devient un tunnel sans repère au clavier ou au lecteur d'écran dès qu'une
 * série dépasse une vingtaine de tomes, et cette vue sert aussi les
 * collections longues pour n'importe qui.
 */
@Composable
fun SeriesTomeList(
    albums: List<Album>,
    highlightedAlbumId: String? = null,
    onAlbumClick: (Album) -> Unit,
    onGapClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(albums) { buildShelfItems(albums) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items = items, key = { it.key }) { item ->
            when (item) {
                is ShelfItem.Existing -> TomeRow(
                    tomeNumber = item.album.tomeNumber,
                    title = item.album.title,
                    owned = item.album.owned,
                    readStatus = item.album.readStatus,
                    highlighted = item.album.id == highlightedAlbumId,
                    onClick = { onAlbumClick(item.album) },
                )

                is ShelfItem.Gap -> TomeRow(
                    tomeNumber = item.tomeNumber,
                    title = null,
                    owned = false,
                    readStatus = ReadStatus.UNREAD,
                    highlighted = false,
                    onClick = { onGapClick(item.tomeNumber) },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun TomeRow(
    tomeNumber: Int?,
    title: String?,
    owned: Boolean,
    readStatus: ReadStatus,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    // Même phrase que la tranche correspondante de l'étagère (§ADR-009) :
    // les deux vues racontent exactement la même chose.
    val description = spineContentDescription(tomeNumber, owned, readStatus, highlighted)
    val stateLabel = stringResource(if (owned) R.string.tome_row_state_owned else R.string.tome_row_state_missing)
    val stateColor = if (owned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    val readLabel = when (readStatus) {
        ReadStatus.READ -> stringResource(R.string.tome_row_state_read)
        ReadStatus.LENT -> stringResource(R.string.tome_row_state_lent)
        ReadStatus.UNREAD -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tomeNumber?.let { stringResource(R.string.tome_row_number, it) }
                    ?: stringResource(R.string.spine_unnumbered_marker),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.labelLarge,
                color = stateColor,
            )
            if (readLabel != null) {
                Text(
                    text = readLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
