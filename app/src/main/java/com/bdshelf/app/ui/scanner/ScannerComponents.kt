package com.bdshelf.app.ui.scanner

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.defaultMinSize
import androidx.lifecycle.LifecycleOwner
import com.bdshelf.app.R
import com.bdshelf.app.domain.canonicalEan
import com.bdshelf.app.domain.normalizeEanInput
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Zone caméra du scanner (§6.3), partagée entre le scan simple et le mode
 * inventaire : preview CameraX, réticule, lampe torche.
 */
@Composable
internal fun ScanCameraArea(
    lifecycleOwner: LifecycleOwner,
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CameraPreview(
            lifecycleOwner = lifecycleOwner,
            onBarcodeDetected = onBarcodeDetected,
            onCameraReady = {
                camera = it
                // Réapplique l'état de la lampe après un re-bind
                // (retour de l'écran des réglages, par exemple).
                it.cameraControl.enableTorch(torchOn)
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1.6f)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp),
                ),
        )

        Text(
            text = stringResource(R.string.scanner_instruction),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp, start = 24.dp, end = 24.dp),
        )

        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            FilledTonalIconButton(
                onClick = {
                    torchOn = !torchOn
                    camera?.cameraControl?.enableTorch(torchOn)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
            ) {
                Icon(
                    imageVector = if (torchOn) Icons.Filled.FlashOff else Icons.Filled.FlashOn,
                    contentDescription = stringResource(
                        if (torchOn) R.string.scanner_torch_off_cd else R.string.scanner_torch_on_cd,
                    ),
                )
            }
        }
    }
}

/**
 * Saisie manuelle d'un code (§6.3, repli quand le code-barres est abîmé ou que
 * la caméra ne l'accroche pas) : EAN-13, EAN-8 ou ISBN-10, tirets et espaces
 * tolérés. Validation par somme de contrôle avant d'autoriser la confirmation.
 */
@Composable
internal fun ManualIsbnDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val isValid = canonicalEan(input) != null
    // N'affiche l'erreur qu'à partir d'une saisie complète : avant 13 chiffres,
    // l'utilisateur est probablement encore en train de taper.
    val showError = !isValid && normalizeEanInput(input).length >= 13

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scanner_isbn_dialog_title)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.scanner_isbn_dialog_label)) },
                supportingText = {
                    Text(
                        stringResource(
                            if (showError) R.string.scanner_isbn_dialog_error else R.string.scanner_isbn_dialog_hint,
                        ),
                    )
                },
                isError = showError,
                singleLine = true,
                // Ascii et pas Number : la clé d'un ISBN-10 peut être la lettre X.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                // « Terminé » au clavier = Valider, sans chercher le bouton.
                keyboardActions = KeyboardActions(onDone = { if (isValid) onConfirm(input) }),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(input) }, enabled = isValid) {
                Text(stringResource(R.string.scanner_isbn_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.scanner_isbn_dialog_cancel))
            }
        },
    )
}

@Composable
private fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    onBarcodeDetected: (String) -> Unit,
    onCameraReady: (Camera) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDetected by rememberUpdatedState(onBarcodeDetected)
    val currentOnCameraReady by rememberUpdatedState(onCameraReady)
    val analyzer = remember { BarcodeAnalyzer { currentOnDetected(it) } }
    val cameraProviderHolder = remember { arrayOfNulls<ProcessCameraProvider>(1) }
    val disposed = remember { booleanArrayOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            disposed[0] = true
            cameraProviderHolder[0]?.unbindAll()
            analyzer.close() // libère le détecteur natif ML Kit
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener(
                {
                    // L'écran peut avoir été quitté avant que le provider soit prêt.
                    if (disposed[0]) return@addListener
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderHolder[0] = cameraProvider

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        // 1280x720 plutôt que les 640x480 par défaut : les barres
                        // fines d'un EAN-13 serré demandent plus de définition.
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        Size(1280, 720),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                    ),
                                )
                                .build(),
                        )
                        .build()
                        .also { it.setAnalyzer(ContextCompat.getMainExecutor(ctx), analyzer) }

                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }.onSuccess { currentOnCameraReady(it) }
                },
                ContextCompat.getMainExecutor(ctx),
            )
            previewView
        },
    )
}

/** Analyse les images de la preview pour y détecter un code-barres EAN-13/EAN-8 (ISBN inclus). */
private class BarcodeAnalyzer(private val onDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8)
            .build(),
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes -> barcodes.firstOrNull()?.rawValue?.let(onDetected) }
            .addOnCompleteListener { imageProxy.close() }
    }

    /** Ferme le détecteur natif ML Kit pour éviter une fuite mémoire. */
    fun close() = scanner.close()
}
