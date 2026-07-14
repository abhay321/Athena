package com.example.ui

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.OcrTemplate
import com.example.data.OcrSimulator
import com.example.viewmodel.BrainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    viewModel: BrainViewModel,
    modifier: Modifier = Modifier
) {
    val activeMode by viewModel.activeCaptureMode.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Mode Selectors ---
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = activeMode == "camera",
                onClick = { if (!isScanning) viewModel.activeCaptureMode.value = "camera" },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "Camera Scanner") }
            ) {
                Text("Simulated Scanner")
            }
            SegmentedButton(
                selected = activeMode == "text",
                onClick = { if (!isScanning) viewModel.activeCaptureMode.value = "text" },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Default.EditNote, contentDescription = "Direct Input") }
            ) {
                Text("Direct Input")
            }
        }

        if (activeMode == "camera") {
            OcrScannerView(viewModel)
        } else {
            DirectInputView(viewModel)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OcrScannerView(viewModel: BrainViewModel) {
    val ocrSimulator = remember { OcrSimulator() }
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val statusMessage by viewModel.scanningStatusMessage.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var useSimulationFallback by remember { mutableStateOf(false) }
    var cameraInitError by remember { mutableStateOf<String?>(null) }

    // Animation for laser sweeping
    val infiniteTransition = rememberInfiniteTransition()
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Document Template Selector
        Text(
            text = "Select Document to Scan:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ocrSimulator.templates) { template ->
                val isSelected = selectedTemplate?.id == template.id
                Card(
                    modifier = Modifier
                        .width(180.dp)
                        .clickable(enabled = !isScanning) { viewModel.selectTemplate(template) }
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (template.category) {
                                    "Whiteboard" -> Icons.Default.BorderColor
                                    "Book Scan" -> Icons.Default.MenuBook
                                    "Meeting Slide" -> Icons.Default.Tv
                                    else -> Icons.Default.EventNote
                                },
                                contentDescription = template.category,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = template.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = template.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Scanner Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selectedTemplate != null) {
                if (cameraPermissionState.status.isGranted && !useSimulationFallback && cameraInitError == null) {
                    CameraPreviewView(
                        modifier = Modifier.fillMaxSize(),
                        onCameraFailed = { error ->
                            cameraInitError = error.localizedMessage ?: "Camera init failed"
                        }
                    )
                } else {
                    // Renders the document placeholder or image
                    Image(
                        painter = painterResource(id = R.drawable.img_onboarding_brain_1783818582428),
                        contentDescription = "Simulated Scan Target",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isScanning) 0.6f else 1.0f
                    )
                }

                // High tech hud elements
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Corners of edge-detector
                    Box(modifier = Modifier.align(Alignment.TopStart).size(24.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)))
                    Box(modifier = Modifier.align(Alignment.TopEnd).size(24.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp)))
                    Box(modifier = Modifier.align(Alignment.BottomStart).size(24.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 0.dp)))
                    Box(modifier = Modifier.align(Alignment.BottomEnd).size(24.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 8.dp)))

                    Text(
                        text = "TEMPLATE: ${selectedTemplate!!.title.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    if (cameraInitError != null || useSimulationFallback) {
                        Text(
                            text = if (cameraInitError != null) "CAM ERROR: FALLBACK SIMULATION ACTIVE" else "SIMULATOR ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cameraInitError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (isScanning) {
                        // Scan Laser Sweep Animation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = 200.dp * laserOffset)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.primary, Color.Transparent)
                                    )
                                )
                        )
                    }
                }

                // If not granted and not using simulation fallback, show the prompt overlay on top
                if (!cameraPermissionState.status.isGranted && !useSimulationFallback) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Camera Permission Required",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Live Camera Viewfinder",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Grant camera permission to see a live view of your document, or proceed with high-fidelity simulation.",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { cameraPermissionState.launchPermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Grant Permission")
                                }
                                OutlinedButton(
                                    onClick = { useSimulationFallback = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                                ) {
                                    Text("Use Simulation")
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.CenterFocusWeak,
                        contentDescription = "No template selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Simulated Camera Viewport",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Select one of the document templates above to simulate aligning the physical camera feed.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Scanning Progress Overlay
            if (isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "EXTRACTING KNOWLEDGE...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusMessage,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Action Trigger Button
        Button(
            onClick = { selectedTemplate?.let { viewModel.startOcrScan(it) } },
            enabled = selectedTemplate != null && !isScanning,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("initiate_ocr_scan_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.DocumentScanner, contentDescription = "Scan")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Capture & Process with AI Pipeline",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun DirectInputView(viewModel: BrainViewModel) {
    val title by viewModel.manualTitle.collectAsState()
    val content by viewModel.manualContent.collectAsState()
    val category by viewModel.manualCategory.collectAsState()
    val isSaving by viewModel.isSavingManual.collectAsState()

    var showCategoryMenu by remember { mutableStateOf(false) }
    val categories = listOf("Personal", "Whiteboard", "Book Scan", "Meeting Minutes", "Project Plans")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Create Manual Note:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.manualTitle.value = it },
                label = { Text("Document Title") },
                placeholder = { Text("e.g. Project Specs or Brain Dump") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_title_input"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            )
        }

        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { showCategoryMenu = !showCategoryMenu }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category")
                        }
                    },
                    enabled = !isSaving
                )
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.manualCategory.value = cat
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.manualContent.value = it },
                label = { Text("Document Body (Run OCR or paste text)") },
                placeholder = { Text("Type or paste any information here. Project Athena's Document Understanding pipeline will automatically structure it, create check-lists, summaries, flashcards and link tags.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .testTag("manual_content_input"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving,
                maxLines = 20
            )
        }

        item {
            Button(
                onClick = { viewModel.captureManualText() },
                enabled = title.trim().isNotEmpty() && content.trim().isNotEmpty() && !isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_manual_note_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Processing Ingestion...")
                } else {
                    Icon(Icons.Default.SaveAlt, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Analyze & Sync to Second Brain",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    onCameraReady: () -> Unit = {},
    onCameraFailed: (Throwable) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = CameraPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    onCameraReady()
                } catch (e: Exception) {
                    onCameraFailed(e)
                }
            }, ContextCompat.getMainExecutor(context))
            previewView
        },
        modifier = modifier
    )
}
