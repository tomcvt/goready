package com.tomcvt.goready.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.tomcvt.goready.LocalOverlayHost
import com.tomcvt.goready.LocalScanSetRepositoryProvider
import com.tomcvt.goready.data.ScanSetEntity
import com.tomcvt.goready.ui.AlertDialogModal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraPermissionWrapper(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    if (permissionGranted) {
        content()
    } else {
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.CAMERA) }
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Camera permission required",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerView(
    modifier: Modifier = Modifier,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasScanned by remember { mutableStateOf(false) } // debounce
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    CameraPermissionWrapper {
    Box(modifier = modifier) {
        key(lensFacing) {
            AndroidView(
                modifier = modifier,
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                        val scanner = BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                    Barcode.FORMAT_QR_CODE,
                                    Barcode.FORMAT_EAN_13,
                                    Barcode.FORMAT_EAN_8
                                )
                                .build()
                        )

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { ia ->
                                ia.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                    if (!hasScanned) {
                                        val image = proxy.image?.let { img ->
                                            InputImage.fromMediaImage(
                                                img,
                                                proxy.imageInfo.rotationDegrees
                                            )
                                        }
                                        if (image != null) {
                                            scanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    barcodes.firstOrNull()?.rawValue?.let { value ->
                                                        hasScanned = true
                                                        onBarcodeScanned(value)
                                                    }
                                                }
                                                .addOnCompleteListener { proxy.close() }
                                        } else {
                                            proxy.close()
                                        }
                                    } else {
                                        proxy.close()
                                    }
                                }
                            }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        Log.d("CameraDebug", "Permission: ${
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED
                        }")
                        Log.d("CameraDebug", "PreviewView size: ${previewView.width}x${previewView.height}")
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            analysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
        }
        IconButton(
            onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                    CameraSelector.LENS_FACING_FRONT
                else
                    CameraSelector.LENS_FACING_BACK
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.FlipCameraAndroid,
                contentDescription = "Flip camera",
                tint = Color.White
            )
        }
    }
    }
}

@Composable
fun SaveBarcodeScreen(
    //repository: BarcodeRepository,
    onSaved: (String) -> Unit,
    onDismissed: () -> Unit,
    restrictedStrings: List<String> = listOf(":", "/", "?", "*", "\"",",")
) {
    val scope = rememberCoroutineScope()
    var scanAttempt by remember { mutableIntStateOf(0) }   // increment → resets BarcodeScannerView
    var scannedValue by remember { mutableStateOf<String?>(null) }
    val flashAlpha = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ── Camera preview ──────────────────────────────────────────────
        // key() tears down and recreates the composable (and its internal
        // hasScanned flag) when the user retries — cleanest reset strategy
        key(scanAttempt) {
            BarcodeScannerView(
                modifier = Modifier.fillMaxSize(),
                onBarcodeScanned = { value ->
                    scannedValue = value
                    scope.launch {
                        flashAlpha.snapTo(0.75f)
                        flashAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                        )
                    }
                }
            )
        }

        // ── Flash overlay ────────────────────────────────────────────────
        // only composed while animating, so it doesn't permanently eat touches
        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(flashAlpha.value)
                    .background(Color.White)
            )
        }

        // ── Close button ─────────────────────────────────────────────────
        IconButton(
            onClick = onDismissed,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }

        // ── Bottom card ──────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            AnimatedContent(
                targetState = scannedValue,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "card_content"
            ) { value ->
                if (value == null) {
                    // ── Scanning state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Scan code",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    // ── Scanned state
                    val restricted = restrictedStrings.any { value.contains(it) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = value,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        if (restricted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Contains restricted characters: ${
                                        restrictedStrings.filter { value.contains(it) }
                                            .joinToString(", ") { "\"$it\"" }
                                    }",
                                    color = Color(0xFFFFB300),
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Replay — discard and scan again
                            OutlinedButton(
                                onClick = {
                                    scannedValue = null
                                    scanAttempt++
                                },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Scan again"
                                )
                            }
                            // Accept — only if not restricted
                            if (!restricted) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            onSaved(value)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Accept"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class ScanFeedback { NONE, SUCCESS, FAIL }

@Composable
fun MultiBarcodeScannerView(
    modifier: Modifier = Modifier,
    codes: List<ScanCode>,
    onSuccess: (String) -> Unit,
    onFail: (String) -> Unit,
    onFinish: (String) -> Unit,
    onDismissed: () -> Unit,
    dismissable: Boolean = false
) {
    require(codes.isNotEmpty()) { "codes must not be empty" }

    val scope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(0) }
    var retryAttempt by remember { mutableIntStateOf(0) }
    var feedbackState by remember { mutableStateOf(ScanFeedback.NONE) }

    val feedbackScale = remember { Animatable(0f) }
    val feedbackAlpha = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    // self-contained coroutine — runs animation then mutates state
    suspend fun playFeedback(isSuccess: Boolean, value: String) {
        feedbackAlpha.snapTo(1f)
        feedbackScale.snapTo(0f)
        feedbackScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        delay(if (isSuccess) 600L else 2000L) // 2s debounce lives here for fail

        feedbackAlpha.animateTo(0f, tween(300))
        feedbackScale.snapTo(0f)
        feedbackState = ScanFeedback.NONE

        if (isSuccess) {
            if (currentIndex >= codes.lastIndex) {
                onFinish(value)
            } else {
                onSuccess(value)
                currentIndex++      // triggers key() → resets BarcodeScannerView
            }
        } else {
            retryAttempt++          // triggers key() → resets BarcodeScannerView
        }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {

        // key on both — advance resets via currentIndex, retry resets via retryAttempt
        key(currentIndex, retryAttempt) {
            BarcodeScannerView(
                modifier = Modifier.fillMaxSize(),
                onBarcodeScanned = { value ->
                    // guard for the brief window between key() teardown and recomposition
                    if (feedbackState != ScanFeedback.NONE) return@BarcodeScannerView

                    val isMatch = value == codes[currentIndex].barcode
                    feedbackState = if (isMatch) ScanFeedback.SUCCESS else ScanFeedback.FAIL

                    if (isMatch) {
                        scope.launch {
                            flashAlpha.snapTo(0.75f)
                            flashAlpha.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
                        }
                    } else {
                        onFail(value)
                    }

                    scope.launch { playFeedback(isMatch, value) }
                }
            )
        }

        // white flash — success only
        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(flashAlpha.value)
                    .background(Color.White)
            )
        }

        // center feedback icon
        if (feedbackState != ScanFeedback.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(feedbackAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (feedbackState) {
                        ScanFeedback.SUCCESS -> Icons.Default.CheckCircle
                        ScanFeedback.FAIL    -> Icons.Default.Block
                        ScanFeedback.NONE    -> Icons.Default.CheckCircle // unreachable
                    },
                    contentDescription = null,
                    tint = when (feedbackState) {
                        ScanFeedback.SUCCESS -> Color(0xFF4CAF50)
                        ScanFeedback.FAIL    -> Color(0xFFEF5350)
                        ScanFeedback.NONE    -> Color.Transparent
                    },
                    modifier = Modifier
                        .size(120.dp)
                        .scale(feedbackScale.value)
                )
            }
        }

        if (dismissable) {
            IconButton(
                onClick = onDismissed,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        // bottom card — slides tip in/out on index change
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically { -it } + fadeOut(tween(200)))
                },
                label = "tip_transition"
            ) { index ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${index + 1} / ${codes.size}",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = codes[index].tip,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable: TipInputDialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TipInputDialog(
    initialTip: String = "",
    onAccept: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialTip) { mutableStateOf(initialTip) }

    AlertDialogModal(
        onDismissRequest = onDismiss,
        title = { Text("Scan tip") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("What should the user scan?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        },
        confirmButton = {
            TextButton(onClick = { onAccept(text) }) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable: BarcodeValueInputDialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BarcodeValueInputDialog(
    initialValue: String = "",
    onAccept: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialogModal(
        onDismissRequest = onDismiss,
        title = { Text("Edit barcode value") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Barcode / QR value") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAccept(text) },
                enabled = text.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable: ScanCodeItemCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScanCodeItemCard(
    scanCode: ScanCode,
    onEditBarcode: () -> Unit,
    onEditTip: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scanCode.barcode,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // light-blue QR code button to manually edit the barcode value
                IconButton(onClick = onEditBarcode) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "Edit barcode value",
                        tint = Color(0xFF64B5F6)
                    )
                }
                // icon toggles between Add and Edit depending on whether tip exists
                IconButton(onClick = onEditTip) {
                    Icon(
                        imageVector = if (scanCode.tip.isBlank()) Icons.Default.Add
                        else Icons.Default.Edit,
                        contentDescription = if (scanCode.tip.isBlank()) "Add tip" else "Edit tip",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            AnimatedVisibility(visible = scanCode.tip.isNotBlank()) {
                Text(
                    text = scanCode.tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable: AddBarcodeCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AddBarcodeCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        //onClick = onClick,
        modifier = modifier.fillMaxWidth()
                .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add barcode",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// scanSetContentHash — stable hash for a decoded list + name, used to detect
// unsaved changes relative to the last loaded/saved state.
// ─────────────────────────────────────────────────────────────────────────────

fun scanSetContentHash(codes: List<ScanCode>, name: String): Int =
    (codes.joinToString(",") { "${it.barcode}:${it.tip}" } + "|" + name).hashCode()

// ─────────────────────────────────────────────────────────────────────────────
// ScanSetToolbarRow — private toolbar composable for MultiBarcodesSaverView
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanSetToolbarRow(
    loadedSet: ScanSetEntity?,
    isEditingName: Boolean,
    editingName: String,
    focusRequester: FocusRequester,
    onNameChange: (String) -> Unit,
    onStartEditing: () -> Unit,
    onStopEditing: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Name display / inline editor (only when a set is loaded) ──────────
        if (loadedSet != null) {
            // Both Card and TextField are always composed so the Box height stays
            // constant regardless of which is visible (alpha keeps layout stable).
            Box(modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (!isEditingName) 1f else 0f)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onStartEditing,
                            enabled = !isEditingName
                        ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = editingName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedTextField(
                    value = editingName,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isEditingName) 1f else 0f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { if (!it.isFocused) onStopEditing() },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            // ── Save (update existing) ────────────────────────────────────────
            Card(
                onClick = onSave,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save set",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ── Save As (always visible) ──────────────────────────────────────────
        Card(
            onClick = onSaveAs,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.SaveAs,
                    contentDescription = "Save as new set",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // ── Load / Bookmark (always visible) ─────────────────────────────────
        Card(
            onClick = onLoad,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Load set",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MultiBarcodesSaverView
// Note: passes through SaveBarcodeScreen which writes to DataStore as a
// side effect — harmless here, refactor to a plain BarcodeScannerView dialog
// if you need a repository-free version later.
// ─────────────────────────────────────────────────────────────────────────────

@ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiBarcodesSaverView(
    //repository: BarcodeRepository,
    onDismiss: () -> Unit,
    onAccept: (List<ScanCode>) -> Unit,
    modifier: Modifier = Modifier,
    initialCodes: List<ScanCode> = emptyList(),
    loadedSet: ScanSetEntity? = null
) {

    var items by remember { mutableStateOf<List<ScanCode>>(initialCodes) }
    val ssRepository = LocalScanSetRepositoryProvider.current
    val overlayHost = LocalOverlayHost.current
    var loadedSet by remember { mutableStateOf<ScanSetEntity?>(loadedSet) }
    var loadedSetHash by remember { mutableStateOf<Int?>(null) }
    // ── Name editing state (toolbar) ──────────────────────────────────────────
    var isEditingSetName by remember { mutableStateOf(false) }
    var editingSetName by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        if (loadedSet != null) {
            val decoded = ssRepository.decodeScanCodes(loadedSet!!.encodedCodes)
            if (decoded.isNotEmpty()) {
                items = decoded
                loadedSetHash = scanSetContentHash(decoded, loadedSet!!.name)
                editingSetName = loadedSet!!.name
            }
            if (initialCodes.isNotEmpty()) {
                //we error because we shouldnt do that
                throw IllegalStateException("initialCodes should be empty when providing a loaded set")
                //TODO maybe switch to dialog for graceful handling
            }
        }

    }
    val scope = rememberCoroutineScope()

    val setNameFocusRequester = remember { FocusRequester() }

    // Sync editing name when a set is loaded/changed
    LaunchedEffect(loadedSet) {
        editingSetName = loadedSet?.name ?: ""
        isEditingSetName = false
    }

    // Auto-focus the TextField when editing starts
    LaunchedEffect(isEditingSetName) {
        if (isEditingSetName) {
            try { setNameFocusRequester.requestFocus() } catch (e: Exception) { /* ignore */ }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Code list setup") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = { onAccept(items) },
                        // Validation: list must not be empty and every barcode value must be non-blank
                        enabled = items.isNotEmpty() && items.all { it.barcode.isNotBlank() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Accept  •  ${items.size} code${if (items.size == 1) "" else "s"}")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Set toolbar row ───────────────────────────────────────────────
            item {
                ScanSetToolbarRow(
                    loadedSet = loadedSet,
                    isEditingName = isEditingSetName,
                    editingName = editingSetName,
                    focusRequester = setNameFocusRequester,
                    onNameChange = { editingSetName = it },
                    onStartEditing = { isEditingSetName = true },
                    onStopEditing = { isEditingSetName = false },
                    onSave = {
                        loadedSet?.let { set ->
                            scope.launch {
                                val newName = editingSetName.trim().ifBlank { set.name }
                                val encoded = ssRepository.encodeScanCodes(items)
                                val updated = set.copy(
                                    name = newName,
                                    encodedCodes = encoded,
                                    size = items.size
                                )
                                ssRepository.updateScanSet(updated)
                                loadedSet = updated
                                loadedSetHash = scanSetContentHash(items, newName)
                            }
                        }
                    },
                    onSaveAs = {
                        overlayHost.show { dismiss ->
                            SaveSetModal(
                                currentCodes = items,
                                initialName = editingSetName,
                                onDone = { newSet ->
                                    loadedSet = newSet
                                    editingSetName = newSet.name
                                    loadedSetHash = scanSetContentHash(items, newSet.name)
                                    dismiss()
                                },
                                onDismiss = dismiss
                            )
                        }
                    },
                    onLoad = {
                        overlayHost.show { dismiss ->
                            LoadSetModal(
                                onSelected = { set ->
                                    val decoded = ssRepository.decodeScanCodes(set.encodedCodes)
                                    items = decoded
                                    loadedSet = set
                                    editingSetName = set.name
                                    loadedSetHash = scanSetContentHash(decoded, set.name)
                                    dismiss()
                                },
                                onDismiss = dismiss
                            )
                        }
                    }
                )
            }

            // ── Code item cards ───────────────────────────────────────────────
            itemsIndexed(
                items = items,
                // barcode + index key: handles duplicate barcodes in the list safely
                key = { index, item -> "${item.barcode}_$index" }
            ) { index, scanCode ->
                ScanCodeItemCard(
                    scanCode = scanCode,
                    onEditBarcode = {
                        val initial = scanCode.barcode
                        overlayHost.show { dismiss ->
                            BarcodeValueInputDialog(
                                initialValue = initial,
                                onAccept = { value ->
                                    items = items.toMutableList().also { it[index] = it[index].copy(barcode = value) }
                                    dismiss()
                                },
                                onDismiss = dismiss
                            )
                        }
                    },
                    onEditTip = {
                        val initial = scanCode.tip
                        overlayHost.show { dismiss ->
                            TipInputDialog(
                                initialTip = initial,
                                onAccept = { tip ->
                                    items = items.toMutableList().also { it[index] = it[index].copy(tip = tip) }
                                    dismiss()
                                },
                                onDismiss = dismiss
                            )
                        }
                    },
                    onDelete = {
                        items = items.toMutableList().also { it.removeAt(index) }
                    }
                )
            }

            item {
                AddBarcodeCard(
                    onClick = {
                        overlayHost.show { dismiss ->
                            SaveBarcodeScreen(
                                onSaved = { value ->
                                    items = items + ScanCode(barcode = value, tip = "")
                                    dismiss()
                                },
                                onDismissed = dismiss
                            )
                        }
                    },
                    onLongClick = { items = items.toMutableList().also { it.add(ScanCode("", "")) } }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DebugScanView
// ─────────────────────────────────────────────────────────────────────────────

private enum class DebugScanState { SAVING, SCANNING }

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScanView(
    //repository: BarcodeRepository,
    onDismiss: () -> Unit,
    onFinished: () -> Unit
) {
    var state by remember { mutableStateOf(DebugScanState.SAVING) }
    // survive the state transition so the scanner gets the list the saver built
    var codes by remember { mutableStateOf<List<ScanCode>>(emptyList()) }

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "debug_scan_state"
    ) { currentState ->
        when (currentState) {
            DebugScanState.SAVING -> {
                MultiBarcodesSaverView(
                    //repository = repository,
                    onDismiss = onDismiss,
                    onAccept = { accepted ->
                        codes = accepted
                        state = DebugScanState.SCANNING
                    }
                )
            }
            DebugScanState.SCANNING -> {
                MultiBarcodeScannerView(
                    codes = codes,
                    onSuccess = {},
                    onFail = {},
                    onFinish = { onFinished() },
                    onDismissed = { state = DebugScanState.SAVING },
                    dismissable = true
                )
            }
        }
    }
}