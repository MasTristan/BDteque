package com.bdshelf.app.ui.verdict

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdshelf.app.R
import com.bdshelf.app.data.local.dao.SeriesWithCounts
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.domain.toSpineColor
import com.bdshelf.app.ui.components.Shelf
import com.bdshelf.app.ui.theme.OwnedGreen
import com.bdshelf.app.ui.theme.Surface as SurfaceColor

/** Verdict de scan (§6.4) : trois états plein écran — possédé, manquant, inconnu. */
@Composable
fun VerdictScreen(
    ean: String,
    onBackToHome: () -> Unit,
    onCreateAlbum: (seriesId: String, ean: String) -> Unit,
    viewModel: VerdictViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(ean) { viewModel.load(ean) }

    if (uiState.isLoading) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
        return
    }

    when (uiState.outcome) {
        VerdictOutcome.OWNED -> OwnedVerdict(uiState, onBackToHome)
        VerdictOutcome.MISSING -> MissingVerdict(uiState, viewModel, onBackToHome)
        VerdictOutcome.UNKNOWN -> UnknownVerdict(uiState, viewModel, onBackToHome, onCreateAlbum)
    }
}

@Composable
private fun OwnedVerdict(uiState: VerdictUiState, onBackToHome: () -> Unit) {
    val series = uiState.series
    val album = uiState.album

    Surface(modifier = Modifier.fillMaxSize(), color = OwnedGreen) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SurfaceColor,
                modifier = Modifier.size(96.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.verdict_owned_title),
                style = MaterialTheme.typography.titleLarge,
                color = SurfaceColor,
            )
            if (series != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = albumSubtitle(series.title, album?.tomeNumber),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SurfaceColor,
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onBackToHome,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor, contentColor = OwnedGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
            ) {
                Text(
                    text = stringResource(R.string.verdict_back_home_button),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun MissingVerdict(uiState: VerdictUiState, viewModel: VerdictViewModel, onBackToHome: () -> Unit) {
    val series = uiState.series ?: return
    val album = uiState.album ?: return

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.verdict_missing_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = albumSubtitle(series.title, album.tomeNumber),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Shelf(
                seriesId = series.id,
                seriesColor = series.color.toSpineColor(),
                albums = uiState.seriesAlbums,
                highlightedAlbumId = album.id,
                onAlbumClick = {},
                onGapClick = {},
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                if (!album.owned) {
                    Button(
                        onClick = viewModel::onMarkOwned,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.verdict_missing_button),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.verdict_back_home_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnknownVerdict(
    uiState: VerdictUiState,
    viewModel: VerdictViewModel,
    onBackToHome: () -> Unit,
    onCreateAlbum: (String, String) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text(
                text = stringResource(R.string.verdict_unknown_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.verdict_unknown_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.linkedAlbumId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.verdict_unknown_linked_message),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            } else {
                val selectedSeries = uiState.selectedSeries
                if (selectedSeries == null) {
                    OutlinedTextField(
                        value = uiState.seriesQuery,
                        onValueChange = viewModel::onSeriesQueryChange,
                        label = { Text(stringResource(R.string.verdict_unknown_search_label)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.seriesResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.verdict_unknown_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(items = uiState.seriesResults, key = { it.id }) { series ->
                                SeriesPickerRow(series = series, onClick = { viewModel.onSeriesSelected(series.id) })
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selectedSeries.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = viewModel::onSeriesDeselected) {
                            Text(stringResource(R.string.verdict_unknown_change_series_cd))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(items = uiState.selectedSeriesAlbums, key = { it.id }) { album ->
                            AlbumPickerRow(album = album, onClick = { viewModel.onLinkAlbum(album) })
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onCreateAlbum(selectedSeries.id, uiState.ean) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.verdict_unknown_create_button),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
            ) {
                Text(
                    text = stringResource(R.string.verdict_back_home_button),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun albumSubtitle(seriesTitle: String, tomeNumber: Int?): String =
    if (tomeNumber != null) {
        stringResource(R.string.verdict_album_subtitle, seriesTitle, tomeNumber)
    } else {
        stringResource(R.string.verdict_album_subtitle_unnumbered, seriesTitle)
    }

@Composable
private fun SeriesPickerRow(series: SeriesWithCounts, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(series.color.toSpineColor()),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = series.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun AlbumPickerRow(album: Album, onClick: () -> Unit) {
    val label = album.tomeNumber?.let { stringResource(R.string.album_form_tome_label, it) }
        ?: stringResource(R.string.album_form_unnumbered_label)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (!album.title.isNullOrBlank()) {
            Text(
                text = album.title.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
