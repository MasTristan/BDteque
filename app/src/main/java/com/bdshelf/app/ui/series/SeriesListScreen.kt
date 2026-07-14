package com.bdshelf.app.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.rememberScrollState
import com.bdshelf.app.R
import com.bdshelf.app.data.local.dao.SeriesWithCounts
import com.bdshelf.app.domain.SeriesFilter
import com.bdshelf.app.domain.SeriesSort
import com.bdshelf.app.domain.toSpineColor

/** Liste des séries : recherche (séries et albums), filtres rapides, tris (§6.5). */
@Composable
fun SeriesListScreen(
    onBack: () -> Unit,
    onSeriesClick: (String) -> Unit,
    onAddSeries: () -> Unit,
    viewModel: SeriesListViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSeries,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.series_list_add_cd),
                )
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
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
                    text = stringResource(R.string.series_list_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.series_list_search_label)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.series_list_search_clear_cd),
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .defaultMinSize(minHeight = 56.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Chips défilants : trois filtres + gros textes système peuvent
                // dépasser la largeur d'un petit écran.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = uiState.filter == SeriesFilter.ALL,
                        onClick = { viewModel.onFilterChange(SeriesFilter.ALL) },
                        label = { Text(stringResource(R.string.series_list_filter_all)) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = uiState.filter == SeriesFilter.INCOMPLETE,
                        onClick = { viewModel.onFilterChange(SeriesFilter.INCOMPLETE) },
                        label = { Text(stringResource(R.string.series_list_filter_incomplete)) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = uiState.filter == SeriesFilter.UNREAD,
                        onClick = { viewModel.onFilterChange(SeriesFilter.UNREAD) },
                        label = { Text(stringResource(R.string.series_list_filter_unread)) },
                    )
                }
                SortMenuButton(sort = uiState.sort, onSortChange = viewModel::onSortChange)
            }

            if (uiState.series.isEmpty()) {
                // Distinguer « rien ne correspond » (recherche/filtre) de « collection
                // vide » (premier lancement) : le second cas guide vers le bouton +.
                val noFilterActive = uiState.query.isBlank() && uiState.filter == SeriesFilter.ALL
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (noFilterActive) R.string.series_list_empty_no_series else R.string.series_list_empty,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = uiState.series, key = { it.id }) { series ->
                        SeriesRow(series = series, onClick = { onSeriesClick(series.id) })
                    }
                }
            }
        }
    }
}

/** Menu de tri (§6.5) : titre, complétion, ajout récent. */
@Composable
private fun SortMenuButton(sort: SeriesSort, onSortChange: (SeriesSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.series_list_sort_cd),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortMenuItem(
                label = stringResource(R.string.series_list_sort_title),
                selected = sort == SeriesSort.TITLE,
                onClick = {
                    expanded = false
                    onSortChange(SeriesSort.TITLE)
                },
            )
            SortMenuItem(
                label = stringResource(R.string.series_list_sort_completion),
                selected = sort == SeriesSort.COMPLETION,
                onClick = {
                    expanded = false
                    onSortChange(SeriesSort.COMPLETION)
                },
            )
            SortMenuItem(
                label = stringResource(R.string.series_list_sort_recent),
                selected = sort == SeriesSort.RECENT,
                onClick = {
                    expanded = false
                    onSortChange(SeriesSort.RECENT)
                },
            )
        }
    }
}

@Composable
private fun SortMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { RadioButton(selected = selected, onClick = null) },
        onClick = onClick,
    )
}

@Composable
private fun SeriesRow(series: SeriesWithCounts, onClick: () -> Unit) {
    val counts = stringResource(R.string.series_list_counts, series.ownedCount, series.totalCount)
    val gapSuffix = if (series.hasGap) stringResource(R.string.series_list_gap_cd) else ""
    val description = "${series.title}. $counts$gapSuffix"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(series.color.toSpineColor()),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = series.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = counts,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (series.hasGap) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
