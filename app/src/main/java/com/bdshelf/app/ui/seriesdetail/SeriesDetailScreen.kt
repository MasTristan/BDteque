package com.bdshelf.app.ui.seriesdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdshelf.app.R
import com.bdshelf.app.data.local.entities.SeriesStatus
import com.bdshelf.app.domain.toSpineColor
import com.bdshelf.app.ui.components.Shelf

/** Détail d'une série = l'étagère de tranches + en-tête de stats (§6.6). */
@Composable
fun SeriesDetailScreen(
    seriesId: String,
    onBack: () -> Unit,
    onEditSeries: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onAddTome: (String) -> Unit,
    onGapClick: (String, Int) -> Unit,
    viewModel: SeriesDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(seriesId) { viewModel.load(seriesId) }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back_cd),
                    )
                }
                Text(
                    text = uiState.series?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                IconButton(
                    onClick = { onEditSeries(seriesId) },
                    modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.series_detail_edit_series_cd),
                    )
                }
            }

            val series = uiState.series
            if (series != null) {
                val statusText = when (series.status) {
                    SeriesStatus.ONGOING -> stringResource(R.string.series_status_ongoing)
                    SeriesStatus.FINISHED -> stringResource(R.string.series_status_finished)
                    SeriesStatus.UNKNOWN -> stringResource(R.string.series_status_unknown)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.series_detail_owned_of_total, uiState.ownedCount, uiState.totalCount),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.albums.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.series_detail_empty_shelf),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Shelf(
                        seriesId = series.id,
                        seriesColor = series.color.toSpineColor(),
                        albums = uiState.albums,
                        onAlbumClick = { album -> onAlbumClick(album.id) },
                        onGapClick = { tomeNumber -> onGapClick(series.id, tomeNumber) },
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onAddTome(series.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .defaultMinSize(minHeight = 56.dp),
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.series_detail_add_tome_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
