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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdshelf.app.R
import com.bdshelf.app.data.local.dao.SeriesWithCounts
import com.bdshelf.app.domain.toSpineColor

/** Liste alphabétique des séries + recherche (§6.5). */
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

            if (uiState.series.isEmpty() && uiState.query.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.series_list_empty),
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
