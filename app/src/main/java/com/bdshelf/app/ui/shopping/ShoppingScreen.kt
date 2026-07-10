package com.bdshelf.app.ui.shopping

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdshelf.app.R
import com.bdshelf.app.domain.ShoppingGroup
import com.bdshelf.app.domain.ShoppingItem
import com.bdshelf.app.domain.toShareText

/**
 * « À acheter » (§6.10) : la liste de courses de la collection. Chaque ligne se
 * coche en un geste (« Je l'ai ! »), et la liste entière se partage en texte
 * (au libraire, à la famille).
 */
@Composable
fun ShoppingScreen(
    onBack: () -> Unit,
    viewModel: ShoppingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                    text = stringResource(R.string.shopping_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                )
                if (uiState.groups.isNotEmpty()) {
                    val shareHeader = stringResource(R.string.shopping_share_header)
                    val chooserTitle = stringResource(R.string.shopping_share_chooser_title)
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, uiState.groups.toShareText(shareHeader))
                            }
                            context.startActivity(Intent.createChooser(intent, chooserTitle))
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.shopping_share_cd),
                        )
                    }
                }
            }

            if (!uiState.isLoading && uiState.groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.shopping_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    uiState.groups.forEach { group ->
                        item(key = "header-${group.seriesId}") {
                            ShoppingGroupHeader(group)
                        }
                        items(
                            items = group.items,
                            key = { "${group.seriesId}-${it.albumId ?: it.tomeNumber ?: it.title}" },
                        ) { item ->
                            ShoppingItemRow(item = item, onBought = { viewModel.onItemBought(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingGroupHeader(group: ShoppingGroup) {
    Text(
        text = group.seriesTitle,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun ShoppingItemRow(item: ShoppingItem, onBought: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (item.tomeNumber != null) {
                    stringResource(R.string.shopping_item_tome, item.tomeNumber)
                } else {
                    stringResource(R.string.album_form_unnumbered_label)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!item.title.isNullOrBlank()) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedButton(
            onClick = onBought,
            modifier = Modifier.defaultMinSize(minHeight = 56.dp),
        ) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.shopping_item_bought_button),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
