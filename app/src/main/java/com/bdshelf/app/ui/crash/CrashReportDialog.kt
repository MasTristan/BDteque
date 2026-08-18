package com.bdshelf.app.ui.crash

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdshelf.app.R

/**
 * Bannière de plantage au démarrage suivant (§E6 5.4). N'affiche rien tant
 * qu'aucun rapport non acquitté n'existe — poser ce composable au sommet de
 * l'arbre suffit, il est inerte le reste du temps.
 */
@Composable
fun CrashReportGate(viewModel: CrashReportViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    if (uiState.fileName == null) return

    if (uiState.reportVisible) {
        AlertDialog(
            onDismissRequest = viewModel::onDismiss,
            title = { Text(stringResource(R.string.crash_report_view_title)) },
            text = {
                Text(
                    text = uiState.content,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    sendCrashReport(context, uiState.content)
                    viewModel.onSent()
                }) { Text(stringResource(R.string.crash_report_send)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismiss) { Text(stringResource(R.string.crash_report_close)) }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = viewModel::onDismiss,
            title = { Text(stringResource(R.string.crash_report_banner_title)) },
            text = { Text(stringResource(R.string.crash_report_banner_message)) },
            confirmButton = {
                TextButton(onClick = {
                    sendCrashReport(context, uiState.content)
                    viewModel.onSent()
                }) { Text(stringResource(R.string.crash_report_send)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::onViewReport) { Text(stringResource(R.string.crash_report_view)) }
                    TextButton(onClick = viewModel::onDismiss) { Text(stringResource(R.string.crash_report_no_thanks)) }
                }
            },
        )
    }
}

/** Sélecteur système : ni le canal ni le destinataire ne sont choisis par l'app (§E6 5.4). */
private fun sendCrashReport(context: Context, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.crash_report_chooser_title)))
}
