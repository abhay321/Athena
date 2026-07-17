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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
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
    var useSimulationFallback by remember { mutableStateOf(true) }
    var cameraInitError by remember { mutableStateOf<String?>(null) }

    // Continuous Ingest States
    val isContinuousActive by viewModel.isContinuousActive.collectAsState()
    val continuousSegments by viewModel.continuousSegments.collectAsState()
    val continuousStatus by viewModel.continuousStatus.collectAsState()
    val continuousTitle by viewModel.continuousTitle.collectAsState()
    val continuousCategory by viewModel.continuousCategory.collectAsState()

    var scanSubMode by remember { mutableStateOf("single") } // "single" or "continuous"
    var localTitleInput by remember { mutableStateOf("Jira & Confluence Scroll Ingest") }
    var localCategoryInput by remember { mutableStateOf("Project Plans") }

    var activeScrollSessionIndex by remember { mutableStateOf(0) }
    var currentScrollFrameIndex by remember { mutableStateOf(0) }
    var showEvaluationPopup by remember { mutableStateOf(false) }

    // Laser animation
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
        // Sub-mode Tab Row
        TabRow(
            selectedTabIndex = if (scanSubMode == "single") 0 else 1,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = scanSubMode == "single",
                onClick = { if (!isScanning) scanSubMode = "single" },
                text = { Text("Single Snapshot", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.FilterFrames, contentDescription = "Single") }
            )
            Tab(
                selected = scanSubMode == "continuous",
                onClick = { if (!isScanning) scanSubMode = "continuous" },
                text = { Text("Live Scroll Capture", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.DynamicFeed, contentDescription = "Continuous") }
            )
        }

        // Scan Feed Source Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Feed Source:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { useSimulationFallback = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!useSimulationFallback) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (!useSimulationFallback) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Physical Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { useSimulationFallback = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (useSimulationFallback) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (useSimulationFallback) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Monitor, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simulated Desktop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (scanSubMode == "single") {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- SINGLE SNAPSHOT SCANNER ---
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
                        }
                    }
                }
            }

            // Viewport
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
                        Image(
                            painter = painterResource(id = R.drawable.img_onboarding_brain_1783818582428),
                            contentDescription = "Simulated Scan Target",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = if (isScanning) 0.6f else 1.0f
                        )
                    }

                    // Viewport HUD overlays
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Box(modifier = Modifier.align(Alignment.TopStart).size(24.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)))
                        Box(modifier = Modifier.align(Alignment.TopEnd).size(24.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp)))
                        Box(modifier = Modifier.align(Alignment.BottomStart).size(24.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 0.dp)))
                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(24.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 8.dp)))

                        Text(
                            text = "SINGLE SCAN TEMPLATE: ${selectedTemplate!!.title.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        if (cameraInitError != null || useSimulationFallback) {
                            Text(
                                text = if (cameraInitError != null) "CAM ERROR: FALLBACK ACTIVE" else "FALLBACK SIMULATOR ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (cameraInitError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (isScanning) {
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

                    if (!cameraPermissionState.status.isGranted && !useSimulationFallback) {
                        PermissionOverlay(
                            cameraPermissionState = cameraPermissionState,
                            onUseSimulation = { useSimulationFallback = true }
                        )
                    }
                } else {
                    SelectTemplatePrompt()
                }

                // In-Progress overlay
                if (isScanning) {
                    ScanningProgressOverlay(statusMessage = statusMessage, scanProgress = scanProgress)
                }
            }

            // Action Button
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
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- LIVE CONTINUOUS SCROLL-CAPTURE SCANNER ---
                if (!isContinuousActive) {
                // Session Configuration view
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Live Scrolling Work Companion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            "Point your camera at a laptop or monitor screen and scroll through documentation naturally. Athena will continuously stack captured frames, intelligently deduplicate overlapping text blocks, and assemble a cohesive structured note with task lists and summaries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = localTitleInput,
                            onValueChange = { localTitleInput = it },
                            label = { Text("Second Brain Document Title") },
                            placeholder = { Text("e.g., Jira-291 Code Review") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = localCategoryInput,
                            onValueChange = { localCategoryInput = it },
                            label = { Text("Target Category") },
                            placeholder = { Text("e.g., Project Plans") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.startContinuousSession(localTitleInput, localCategoryInput)
                                currentScrollFrameIndex = 0
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Continuous Ingestion Session", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // ACTIVE Continuous scan view
                Text(
                    text = "Active Session: $continuousTitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Viewport representation
                Box(
                    modifier = Modifier
                        .height(260.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraPermissionState.status.isGranted && !useSimulationFallback && cameraInitError == null) {
                        // Show actual Camera Preview
                        CameraPreviewView(modifier = Modifier.fillMaxSize())
                    } else {
                        // SIMULATED Scrolling Laptop Viewport
                        val activeStream = ocrSimulator.scrollSessions[activeScrollSessionIndex]
                        val activeSegment = activeStream[currentScrollFrameIndex]

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E1E1E))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Laptop header bar simulation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2D2D2D), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                                    Box(modifier = Modifier.size(8.dp).background(Color.Yellow, CircleShape))
                                    Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                                }
                                Text(
                                    text = "http://developer.athena.internal/docs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                            }

                            // Document body viewport representing physical scroll state
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "[Laptop Display - Pointing Camera]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = activeSegment.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = activeSegment.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 10,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Scroll bar indicator simulation
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(4.dp)
                                            .fillMaxHeight(0.3f)
                                            .offset(y = (currentScrollFrameIndex * 40).dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                                    )
                                }
                            }

                            // Source selector row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SCROLL CHANNEL: ${if (activeScrollSessionIndex == 0) "JIRA BOARD" else "GITHUB PR"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Scroll Segment ${currentScrollFrameIndex + 1} of ${activeStream.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Viewport corner guides
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Box(modifier = Modifier.align(Alignment.TopStart).size(20.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 6.dp)))
                        Box(modifier = Modifier.align(Alignment.TopEnd).size(20.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topEnd = 6.dp)))
                        Box(modifier = Modifier.align(Alignment.BottomStart).size(20.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomStart = 6.dp)))
                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(20.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomEnd = 6.dp)))

                        // Laser sweep animation when session is scanning or active
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

                    if (!cameraPermissionState.status.isGranted && !useSimulationFallback) {
                        PermissionOverlay(
                            cameraPermissionState = cameraPermissionState,
                            onUseSimulation = { useSimulationFallback = true }
                        )
                    }

                    // Loading overlay
                    if (isScanning) {
                        ScanningProgressOverlay(statusMessage = statusMessage, scanProgress = scanProgress)
                    }
                }

                // Target Feed Selector (if fallback/emulator)
                if (useSimulationFallback || !cameraPermissionState.status.isGranted) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Simulated Screen Scroll Feeds:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    activeScrollSessionIndex = 0
                                    currentScrollFrameIndex = 0
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeScrollSessionIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text("Jira Ticket #291", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    activeScrollSessionIndex = 1
                                    currentScrollFrameIndex = 0
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeScrollSessionIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text("GitHub PR #409", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Ingest Control Trigger
                Button(
                    onClick = {
                        val activeStream = ocrSimulator.scrollSessions[activeScrollSessionIndex]
                        val activeSegment = activeStream[currentScrollFrameIndex]
                        viewModel.captureContinuousSegment(activeSegment.text)
                        
                        // Increment or cycle frames to simulate scrolling
                        currentScrollFrameIndex = (currentScrollFrameIndex + 1) % activeStream.size
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scroll Screen & Capture Overlapping Frame", fontWeight = FontWeight.ExtraBold)
                }

                // Live Session Segment log console
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "LIVE ATHENA PIPELINE CONSOLE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color.Red.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "RECORDING",
                                    fontSize = 8.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• $continuousStatus",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Display the last 2 segment log events to fit perfectly within the 110.dp Card with no nesting scroll
                            val lastSegments = continuousSegments.takeLast(2)
                            lastSegments.forEachIndexed { index, _ ->
                                val frameNum = continuousSegments.size - lastSegments.size + index + 1
                                Text(
                                    text = "• [Frame #$frameNum] Extracted segment text into second brain merge buffer. Status: OK.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // End Session buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.cancelContinuousSession() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Discard", fontSize = 11.sp, maxLines = 1)
                    }
                    Button(
                        onClick = { viewModel.saveRawCapturedNote() },
                        enabled = continuousSegments.isNotEmpty(),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("save_raw_note_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Raw", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Save Raw", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Button(
                        onClick = { viewModel.stopAndProcessContinuousSession() },
                        enabled = continuousSegments.isNotEmpty(),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Process", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("AI Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Evaluation / Preview Section Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Stitched Evaluation & Preview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "Evaluate and verify the aggregated continuous text buffer before running the final structured AI analysis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showEvaluationPopup = true },
                            enabled = continuousSegments.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("evaluate_preview_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RateReview,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview & Evaluate Content", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            }
        }
    }

    if (showEvaluationPopup) {
        val deduplicatedText = viewModel.getDeduplicatedContinuousText()
        val rawCharCount = continuousSegments.sumOf { it.length }
        val dedupCharCount = deduplicatedText.length
        val compressionPercent = if (rawCharCount > 0) {
            ((rawCharCount - dedupCharCount).toFloat() / rawCharCount * 100).toInt().coerceAtLeast(0)
        } else {
            0
        }

        var previewSubTab by remember { mutableStateOf("unified") } // "unified" or "separate"
        var savedSeparateFrames by remember { mutableStateOf(setOf<Int>()) }

        AlertDialog(
            onDismissRequest = { showEvaluationPopup = false },
            icon = {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Evaluate Extracted Note Content",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Statistics metrics badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Frames", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${continuousSegments.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Saved Space", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$compressionPercent%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Final Chars", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$dedupCharCount", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    // Tab Selector inside Preview Popup
                    TabRow(
                        selectedTabIndex = if (previewSubTab == "unified") 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = previewSubTab == "unified",
                            onClick = { previewSubTab = "unified" },
                            text = { Text("Unified Note", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = previewSubTab == "separate",
                            onClick = { previewSubTab = "separate" },
                            text = { Text("Separate Frames (${continuousSegments.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (previewSubTab == "unified") {
                        Text(
                            text = "Aggregated & Deduplicated Output Buffer:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Scrollable container for the unified text preview
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                Text(
                                    text = deduplicatedText.ifEmpty { "No text content has been captured yet. Use the scrolling viewport above to capture overlap frames." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (deduplicatedText.isEmpty()) Color.Gray else Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Individual Captured Frames (Separated):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (continuousSegments.isEmpty()) {
                                Text(
                                    text = "No segments captured yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                continuousSegments.forEachIndexed { idx, segmentText ->
                                    val frameNum = idx + 1
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Frame #$frameNum",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                val isSaved = savedSeparateFrames.contains(frameNum)
                                                Button(
                                                    onClick = {
                                                        viewModel.saveSingleFrameSeparately(frameNum, segmentText)
                                                        savedSeparateFrames = savedSeparateFrames + frameNum
                                                    },
                                                    enabled = !isSaved,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondary,
                                                        contentColor = Color.White
                                                    ),
                                                    modifier = Modifier.height(26.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (isSaved) "Saved ✓" else "Save Separately",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Black, RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Text(
                                                    text = segmentText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.LightGray,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 4,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = "Verify that text blocks were successfully merged and overlapping duplications were pruned by the intelligent window filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { showEvaluationPopup = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", maxLines = 1)
                    }
                    Button(
                        onClick = {
                            showEvaluationPopup = false
                            viewModel.saveRawCapturedNote()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Save Raw", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Button(
                        onClick = {
                            showEvaluationPopup = false
                            viewModel.stopAndProcessContinuousSession()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("AI Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionOverlay(
    cameraPermissionState: com.google.accompanist.permissions.PermissionState,
    onUseSimulation: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
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
                    onClick = onUseSimulation,
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

@Composable
fun SelectTemplatePrompt() {
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

@Composable
fun ScanningProgressOverlay(statusMessage: String, scanProgress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
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
