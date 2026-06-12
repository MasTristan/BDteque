package com.bdshelf.app.ui.seriesform

import androidx.compose.foundation.layout.Arrangement
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
import com.bdshelf.app.data.local.entities.SeriesStatus

/** Ajout/édition manuelle d'une série (§6.8, PLUS). */
@Composable
fun SeriesFormScreen(
    seriesId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: SeriesFormViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(seriesId) { viewModel.load(seriesId) }
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
                    text = stringResource(if (uiState.isNew) R.string.series_form_title_add else R.string.series_form_title_edit),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            if (!uiState.isLoading) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text(stringResource(R.string.series_form_title_label)) },
                        singleLine = true,
                        isError = uiState.titleError,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                    )
                    if (uiState.titleError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.series_form_title_required_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.series_form_status_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    StatusOption(
                        label = stringResource(R.string.series_status_ongoing),
                        selected = uiState.status == SeriesStatus.ONGOING,
                        onClick = { viewModel.onStatusChange(SeriesStatus.ONGOING) },
                    )
                    StatusOption(
                        label = stringResource(R.string.series_status_finished),
                        selected = uiState.status == SeriesStatus.FINISHED,
                        onClick = { viewModel.onStatusChange(SeriesStatus.FINISHED) },
                    )
                    StatusOption(
                        label = stringResource(R.string.series_status_unknown),
                        selected = uiState.status == SeriesStatus.UNKNOWN,
                        onClick = { viewModel.onStatusChange(SeriesStatus.UNKNOWN) },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .toggleable(
                                value = uiState.isTracked,
                                onValueChange = viewModel::onTrackedChange,
                                role = Role.Switch,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.series_form_tracked_label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = uiState.isTracked, onCheckedChange = null)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.knownTomeCountText,
                        onValueChange = viewModel::onKnownTomeCountChange,
                        label = { Text(stringResource(R.string.series_form_known_tome_count_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = { Text(stringResource(R.string.series_form_notes_label)) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                    )

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
                                text = stringResource(R.string.series_form_delete_button),
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
            title = { Text(stringResource(R.string.series_form_delete_confirm_title)) },
            text = { Text(stringResource(R.string.series_form_delete_confirm_message)) },
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
private fun StatusOption(label: String, selected: Boolean, onClick: () -> Unit) {
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
