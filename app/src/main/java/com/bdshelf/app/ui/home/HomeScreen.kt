package com.bdshelf.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdshelf.app.R
import com.bdshelf.app.ui.components.BigScanButton

/** Écran d'accueil (§6.2) : wordmark, scan, et raccourcis vers la collection et les sorties. */
@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onCollectionClick: () -> Unit,
    onReleasesClick: () -> Unit,
    onShoppingClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_wordmark, uiState.ownerName),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.home_settings_cd),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            BigScanButton(onClick = onScanClick)

            Spacer(modifier = Modifier.height(24.dp))

            HomeSummaryCard(
                title = stringResource(R.string.home_collection_title),
                value = stringResource(
                    R.string.home_collection_stats,
                    uiState.stats.seriesCount,
                    uiState.stats.ownedCount,
                    uiState.stats.missingCount,
                ),
                onClick = onCollectionClick,
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeSummaryCard(
                title = stringResource(R.string.home_releases_title),
                value = if (uiState.upcomingCount == 0) {
                    stringResource(R.string.home_releases_count_zero)
                } else {
                    pluralStringResource(R.plurals.home_releases_count, uiState.upcomingCount, uiState.upcomingCount)
                },
                onClick = onReleasesClick,
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeSummaryCard(
                title = stringResource(R.string.home_shopping_title),
                value = if (uiState.shoppingCount == 0) {
                    stringResource(R.string.home_shopping_count_zero)
                } else {
                    pluralStringResource(R.plurals.home_shopping_count, uiState.shoppingCount, uiState.shoppingCount)
                },
                onClick = onShoppingClick,
            )
        }
    }
}

@Composable
private fun HomeSummaryCard(title: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
