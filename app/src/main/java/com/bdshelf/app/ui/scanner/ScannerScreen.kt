package com.bdshelf.app.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdshelf.app.R

/**
 * Scanner plein écran (§6.3) : zone caméra partagée ([ScanCameraArea]),
 * replis saisie d'ISBN au clavier et parcours manuel de la collection,
 * accès au mode inventaire (scan en rafale, §6.11).
 */
@Composable
fun ScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onManualEntry: () -> Unit,
    onInventoryMode: () -> Unit,
    onBack: () -> Unit,
    viewModel: ScannerViewModel = viewModel(),
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

    // Réévalue la permission au retour sur l'écran : sans cela, accorder la
    // permission depuis les réglages système laisserait la caméra bloquée.
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

    LaunchedEffect(uiState.scannedEan) {
        uiState.scannedEan?.let { ean ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress) // confirme la lecture sans regarder l'écran
            onBarcodeScanned(ean)
        }
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
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onInventoryMode,
                    modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.scanner_inventory_button),
                        style = MaterialTheme.typography.labelLarge,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { showIsbnDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.scanner_type_isbn_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                OutlinedButton(
                    onClick = onManualEntry,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.scanner_manual_entry_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/** Explication + accès aux réglages quand la permission caméra est refusée (§6.3). */
@Composable
internal fun CameraPermissionRationale(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.scanner_permission_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp),
        ) {
            Text(
                text = stringResource(R.string.scanner_permission_settings_button),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
