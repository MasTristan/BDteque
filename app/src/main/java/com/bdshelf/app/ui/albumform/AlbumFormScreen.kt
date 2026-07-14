package com.bdshelf.app.ui.albumform

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdshelf.app.R
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.ui.components.CoverImage
import com.bdshelf.app.ui.components.SpineTile

/** Fiche tome (§6.6, §6.8) : ajout ou édition, toggle possédé avec animation tampon. */
@Composable
fun AlbumFormScreen(
    seriesId: String,
    albumId: String?,
    prefilledTomeNumber: Int?,
    prefilledEan: String? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: AlbumFormViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(seriesId, albumId) { viewModel.load(seriesId, albumId, prefilledTomeNumber, prefilledEan) }
    LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved() }
    LaunchedEffect(uiState.deleted) { if (uiState.deleted) onDeleted() }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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
                    text = stringResource(if (uiState.isNew) R.string.album_form_title_add else R.string.album_form_title_edit),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            if (!uiState.isLoading) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    // Tranche + couverture (si téléchargée, §6.4) côte à côte : la
                    // tranche reste la représentation canonique de l'étagère.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SpineTile(
                            tomeNumber = uiState.tomeNumber,
                            seriesColor = uiState.seriesColor,
                            owned = uiState.owned,
                            readStatus = uiState.readStatus,
                            height = 180.dp,
                            onClick = {},
                        )
                        if (uiState.coverFile != null) {
                            Spacer(modifier = Modifier.width(16.dp))
                            CoverImage(
                                coverFile = uiState.coverFile,
                                modifier = Modifier
                                    .width(135.dp)
                                    .height(180.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .toggleable(
                                value = uiState.owned,
                                onValueChange = viewModel::onOwnedChange,
                                role = Role.Switch,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.album_form_owned_label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = uiState.owned, onCheckedChange = null)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.isNew) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .toggleable(
                                    value = uiState.isUnnumbered,
                                    onValueChange = viewModel::onUnnumberedChange,
                                    role = Role.Checkbox,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = uiState.isUnnumbered, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.album_form_unnumbered_checkbox),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        if (!uiState.isUnnumbered) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.tomeNumberText,
                                onValueChange = viewModel::onTomeNumberChange,
                                label = { Text(stringResource(R.string.album_form_tome_number_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                isError = uiState.duplicateError,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp),
                            )
                        }
                    } else {
                        Text(
                            text = uiState.tomeNumber?.let { stringResource(R.string.album_form_tome_label, it) }
                                ?: stringResource(R.string.album_form_unnumbered_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    if (uiState.duplicateError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.album_form_duplicate_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text(stringResource(R.string.album_form_title_label)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.album_form_read_status_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    ReadStatusOption(
                        label = stringResource(R.string.album_form_read_status_unread),
                        selected = uiState.readStatus == ReadStatus.UNREAD,
                        onClick = { viewModel.onReadStatusChange(ReadStatus.UNREAD) },
                    )
                    ReadStatusOption(
                        label = stringResource(R.string.album_form_read_status_read),
                        selected = uiState.readStatus == ReadStatus.READ,
                        onClick = { viewModel.onReadStatusChange(ReadStatus.READ) },
                    )
                    ReadStatusOption(
                        label = stringResource(R.string.album_form_read_status_lent),
                        selected = uiState.readStatus == ReadStatus.LENT,
                        onClick = { viewModel.onReadStatusChange(ReadStatus.LENT) },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.edition,
                        onValueChange = viewModel::onEditionChange,
                        label = { Text(stringResource(R.string.album_form_edition_label)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                    )

                    if (uiState.ean != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.album_form_ean_label, uiState.ean!!),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = viewModel::onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.common_save),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    if (!uiState.isNew) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.album_form_delete_button),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.album_form_delete_confirm_title)) },
            text = { Text(stringResource(R.string.album_form_delete_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    viewModel.onDeleteConfirmed()
                }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ReadStatusOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
