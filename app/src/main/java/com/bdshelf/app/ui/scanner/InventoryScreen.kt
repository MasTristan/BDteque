package com.bdshelf.app.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdshelf.app.R

/**
 * Mode inventaire (§6.11) : la caméra reste ouverte, chaque scan confirmé
 * s'empile en bas avec son mini-verdict et son action rapide. Pensé pour
 * cataloguer une pile d'albums en quelques minutes.
 */
@Composable
fun InventoryScreen(
    onOpenVerdict: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: InventoryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Chaque nouveau scan confirmé vibre : on catalogue sans regarder l'écran.
    LaunchedEffect(uiState.scannedCount) {
        if (uiState.scannedCount > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var showIsbnDialog by remember { mutableStateOf(false) }

    if (showIsbnDialog) {
        ManualIsbnDialog(
            onConfirm = { code ->
                showIsbnDialog = false
                viewModel.onManualEan(code)
            },
            onDismiss = { showIsbnDialog = false },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = stringResource(R.string.inventory_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val scannedLabel = pluralStringResource(
                        R.plurals.inventory_counter_scanned,
                        uiState.scannedCount,
                        uiState.scannedCount,
                    )
                    val addedLabel = pluralStringResource(
                        R.plurals.inventory_counter_added,
                        uiState.addedCount,
                        uiState.addedCount,
                    )
                    Text(
                        text = "$scannedLabel · $addedLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (hasCameraPermission) {
                    ScanCameraArea(
                        lifecycleOwner = lifecycleOwner,
                        onBarcodeDetected = viewModel::onBarcodeDetected,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CameraPermissionRationale(
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (uiState.scans.isEmpty()) {
                    Text(
                        text = stringResource(R.string.inventory_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    // Les trois derniers suffisent : le compteur en haut tient le total.
                    uiState.scans.take(3).forEach { scan ->
                        InventoryScanCard(
                            scan = scan,
                            onMarkOwned = { viewModel.onMarkOwned(scan) },
                            onAddSuggested = { viewModel.onAddSuggested(scan) },
                            onOpenVerdict = { onOpenVerdict(scan.ean) },
                            onUndo = { viewModel.onUndo(scan) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                OutlinedButton(
                    onClick = { showIsbnDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.scanner_type_isbn_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/** Mini-verdict d'un scan : ce qui a été reconnu + l'action en un geste (§6.11). */
@Composable
private fun InventoryScanCard(
    scan: InventoryScan,
    onMarkOwned: () -> Unit,
    onAddSuggested: () -> Unit,
    onOpenVerdict: () -> Unit,
    onUndo: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scanHeadline(scan),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = scanStatusLabel(scan),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        // secondary = vert "possédé" dans les deux thèmes.
                        scan.actionTaken || scan.status == InventoryScanStatus.OWNED -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (scan.canUndo) {
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(onClick = onUndo, modifier = Modifier.defaultMinSize(minHeight = 56.dp)) {
                    Text(stringResource(R.string.common_undo), style = MaterialTheme.typography.labelLarge)
                }
            }
            if (!scan.actionTaken) {
                when (scan.status) {
                    InventoryScanStatus.MISSING -> {
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = onMarkOwned, modifier = Modifier.defaultMinSize(minHeight = 56.dp)) {
                            Text(stringResource(R.string.inventory_action_owned), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    InventoryScanStatus.SUGGESTED -> {
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = onAddSuggested, modifier = Modifier.defaultMinSize(minHeight = 56.dp)) {
                            Text(stringResource(R.string.inventory_action_add), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    InventoryScanStatus.UNKNOWN -> {
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = onOpenVerdict, modifier = Modifier.defaultMinSize(minHeight = 56.dp)) {
                            Text(stringResource(R.string.inventory_action_open), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    InventoryScanStatus.IDENTIFYING, InventoryScanStatus.OWNED -> Unit
                }
            }
        }
    }
}

/** Première ligne : le livre tel qu'on l'a reconnu (série · tome, ou titre, ou code). */
@Composable
private fun scanHeadline(scan: InventoryScan): String {
    val seriesTitle = scan.seriesTitle
    return when {
        seriesTitle != null && scan.tomeNumber != null ->
            stringResource(R.string.verdict_album_subtitle, seriesTitle, scan.tomeNumber)
        seriesTitle != null -> seriesTitle
        scan.identifiedTitle != null -> scan.identifiedTitle
        else -> scan.ean
    }
}

@Composable
private fun scanStatusLabel(scan: InventoryScan): String = when {
    scan.actionTaken -> stringResource(R.string.inventory_status_added)
    else -> when (scan.status) {
        InventoryScanStatus.IDENTIFYING -> stringResource(R.string.inventory_status_identifying)
        InventoryScanStatus.OWNED -> stringResource(R.string.inventory_status_owned)
        InventoryScanStatus.MISSING -> stringResource(R.string.inventory_status_missing)
        InventoryScanStatus.SUGGESTED -> stringResource(R.string.inventory_status_suggested)
        InventoryScanStatus.UNKNOWN -> stringResource(R.string.inventory_status_unknown)
    }
}
