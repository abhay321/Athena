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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    viewModel: BrainViewModel,
    modifier: Modifier = Modifier
) {
    val activeMode by viewModel.activeCaptureMode.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    
    // Unified tab index: 0 = Single Scan, 1 = Live Scroll Ingest, 2 = Manual Input
    var selectedSubTabIndex by remember(activeMode) {
        mutableStateOf(if (activeMode == "text") 2 else 0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Unified Single TabRow for clean navigation & zero visual noise ---
        TabRow(
            selectedTabIndex = selectedSubTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedSubTabIndex == 0,
                onClick = { 
                    if (!isScanning) {
                        selectedSubTabIndex = 0
                        viewModel.activeCaptureMode.value = "camera"
                    }
                },
                text = { Text("Single Scan", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Default.FilterFrames, contentDescription = "Single", modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTabIndex == 1,
                onClick = { 
                    if (!isScanning) {
                        selectedSubTabIndex = 1
                        viewModel.activeCaptureMode.value = "camera"
                    }
                },
                text = { Text("Live Scroll Ingest", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Default.DynamicFeed, contentDescription = "Continuous", modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTabIndex == 2,
                onClick = { 
                    if (!isScanning) {
                        selectedSubTabIndex = 2
                        viewModel.activeCaptureMode.value = "text"
                    }
                },
                text = { Text("Manual Text", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Default.EditNote, contentDescription = "Manual Input", modifier = Modifier.size(18.dp)) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSubTabIndex) {
                0 -> SingleScanView(viewModel)
                1 -> LiveScrollIngestView(viewModel)
                2 -> DirectInputView(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SingleScanView(viewModel: BrainViewModel) {
    val ocrSimulator = remember { OcrSimulator() }
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val statusMessage by viewModel.scanningStatusMessage.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var useSimulationFallback by remember { mutableStateOf(true) }
    var cameraInitError by remember { mutableStateOf<String?>(null) }

    // Laser scanning animation offset
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Select Document Template:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ocrSimulator.templates) { template ->
                val isSelected = selectedTemplate?.id == template.id
                Card(
                    modifier = Modifier
                        .width(170.dp)
                        .clickable(enabled = !isScanning) { viewModel.selectTemplate(template) }
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
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
                                modifier = Modifier.size(28.dp)
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
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Viewport representation (Responsive, fills leftover space beautifully)
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
                            cameraInitError = error.localizedMessage ?: "Camera failed"
                        }
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_onboarding_brain_1783818582428),
                        contentDescription = "Simulated Scan Target",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isScanning) 0.5f else 1.0f
                    )
                }

                // HUD boundaries & overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Box(modifier = Modifier.align(Alignment.TopStart).size(20.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 6.dp)))
                    Box(modifier = Modifier.align(Alignment.TopEnd).size(20.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topEnd = 6.dp)))
                    Box(modifier = Modifier.align(Alignment.BottomStart).size(20.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomStart = 6.dp)))
                    Box(modifier = Modifier.align(Alignment.BottomEnd).size(20.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomEnd = 6.dp)))

                    Text(
                        text = "TARGET: ${selectedTemplate!!.title.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    // Integrated, sleek Feed Selector pill
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { useSimulationFallback = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!useSimulationFallback) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (!useSimulationFallback) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.height(26.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { useSimulationFallback = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (useSimulationFallback) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (useSimulationFallback) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.height(26.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Monitor, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simulator", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = 180.dp * laserOffset)
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

            if (isScanning) {
                ScanningProgressOverlay(statusMessage = statusMessage, scanProgress = scanProgress)
            }
        }

        // Capture trigger button
        Button(
            onClick = { selectedTemplate?.let { viewModel.startOcrScan(it) } },
            enabled = selectedTemplate != null && !isScanning,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("initiate_ocr_scan_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.DocumentScanner, contentDescription = "Scan", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Capture & Process Document",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveScrollIngestView(viewModel: BrainViewModel) {
    val ocrSimulator = remember { OcrSimulator() }
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val statusMessage by viewModel.scanningStatusMessage.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var useSimulationFallback by remember { mutableStateOf(true) }
    var cameraInitError by remember { mutableStateOf<String?>(null) }

    val isContinuousActive by viewModel.isContinuousActive.collectAsState()
    val continuousSegments by viewModel.continuousSegments.collectAsState()
    val continuousStatus by viewModel.continuousStatus.collectAsState()
    val continuousTitle by viewModel.continuousTitle.collectAsState()
    val continuousCategory by viewModel.continuousCategory.collectAsState()

    var localTitleInput by remember { mutableStateOf("Jira & Confluence Scroll Ingest") }
    var localCategoryInput by remember { mutableStateOf("Project Plans") }

    var activeScrollSessionIndex by remember { mutableStateOf(0) }
    var currentScrollFrameIndex by remember { mutableStateOf(0) }
    var showEvaluationPopup by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition()
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    if (!isContinuousActive) {
        // --- Session Configuration form ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Live Scrolling Work Ingest",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Point your camera at a monitor, document, or spreadsheet, and scroll through natively. Athena compiles overlapping captured segments, filtering redundancies, to assemble a complete synchronized text block.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = localTitleInput,
                    onValueChange = { localTitleInput = it },
                    label = { Text("Second Brain Document Title") },
                    placeholder = { Text("e.g., Team Wiki Sync") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = localCategoryInput,
                    onValueChange = { localCategoryInput = it },
                    label = { Text("Category Group") },
                    placeholder = { Text("e.g., Work Notes") },
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
        // --- Active Scrolling Session view ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACTIVE SESSION: ${continuousTitle.uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Category: $continuousCategory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = Color.Red.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                        Text("RECORDING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    }
                }
            }

            // High-fidelity Viewport with embedded controls
            Box(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (cameraPermissionState.status.isGranted && !useSimulationFallback && cameraInitError == null) {
                    CameraPreviewView(modifier = Modifier.fillMaxSize())
                } else {
                    // Simulated Desktop Screen inside viewport
                    val activeStream = ocrSimulator.scrollSessions[activeScrollSessionIndex]
                    val activeSegment = activeStream[currentScrollFrameIndex]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF161616))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Embedded feed switch directly in simulated header bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF262626), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                                Box(modifier = Modifier.size(6.dp).background(Color.Yellow, CircleShape))
                                Box(modifier = Modifier.size(6.dp).background(Color.Green, CircleShape))
                            }
                            
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Jira Feed",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeScrollSessionIndex == 0) Color.White else Color.Gray,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (activeScrollSessionIndex == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            activeScrollSessionIndex = 0
                                            currentScrollFrameIndex = 0
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = "GitHub Feed",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeScrollSessionIndex == 1) Color.White else Color.Gray,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (activeScrollSessionIndex == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            activeScrollSessionIndex = 1
                                            currentScrollFrameIndex = 0
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Simulated text viewport content
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = activeSegment.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activeSegment.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCREEN POSITION: PAGE ${currentScrollFrameIndex + 1}/${activeStream.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                // HUD visual overlays
                Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    Box(modifier = Modifier.align(Alignment.TopStart).size(14.dp).border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp)))
                    Box(modifier = Modifier.align(Alignment.TopEnd).size(14.dp).border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topEnd = 4.dp)))
                    Box(modifier = Modifier.align(Alignment.BottomStart).size(14.dp).border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomStart = 4.dp)))
                    Box(modifier = Modifier.align(Alignment.BottomEnd).size(14.dp).border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomEnd = 4.dp)))

                    // Camera/Simulator switch inside viewport HUD
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = if (!useSimulationFallback) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (!useSimulationFallback) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { useSimulationFallback = false }
                                    .padding(3.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Monitor,
                                contentDescription = null,
                                tint = if (useSimulationFallback) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (useSimulationFallback) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { useSimulationFallback = true }
                                    .padding(3.dp)
                            )
                        }
                    }

                    if (isScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = 160.dp * laserOffset)
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

                if (isScanning) {
                    ScanningProgressOverlay(statusMessage = statusMessage, scanProgress = scanProgress)
                }
            }

            // Scroll / Capture trigger
            Button(
                onClick = {
                    val activeStream = ocrSimulator.scrollSessions[activeScrollSessionIndex]
                    val activeSegment = activeStream[currentScrollFrameIndex]
                    viewModel.captureContinuousSegment(activeSegment.text)
                    currentScrollFrameIndex = (currentScrollFrameIndex + 1) % activeStream.size
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scroll Screen & Capture Overlapping Frame", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Real-time console status
            Text(
                text = "Console Logs: $continuousStatus",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Green,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Horizontal Frame list view (Directly visible, resolves "preview missing")
            Text(
                text = "Captured Segments History (${continuousSegments.size}):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            var savedSeparateFrames by remember { mutableStateOf(setOf<Int>()) }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (continuousSegments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                Text("No captured segments yet. Point camera or simulator at a feed and tap Scroll & Capture.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    itemsIndexed(continuousSegments) { idx, segmentText ->
                        val frameNum = idx + 1
                        val isSaved = savedSeparateFrames.contains(frameNum)
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .height(95.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Segment #$frameNum",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.saveSingleFrameSeparately(frameNum, segmentText)
                                            savedSeparateFrames = savedSeparateFrames + frameNum
                                        },
                                        enabled = !isSaved,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.SaveAlt,
                                            contentDescription = "Save segment separately",
                                            tint = if (isSaved) Color.Green else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = segmentText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Color.LightGray,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Primary control actions at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancelContinuousSession() },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "Discard", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard", fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = { showEvaluationPopup = true },
                    enabled = continuousSegments.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "Preview", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview", fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = { viewModel.stopAndProcessContinuousSession() },
                    enabled = continuousSegments.isNotEmpty(),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = "AI Sync", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }

    // Consolidated Session Preview dialog
    if (showEvaluationPopup) {
        val deduplicatedText = viewModel.getDeduplicatedContinuousText()

        AlertDialog(
            onDismissRequest = { showEvaluationPopup = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Pageview,
                    contentDescription = "Review",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Consolidated Session Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total captured segments: ${continuousSegments.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

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
                                text = deduplicatedText.ifEmpty { "No text content has been captured yet." },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (deduplicatedText.isEmpty()) Color.Gray else Color.White,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            )
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
                        Text("Close", maxLines = 1)
                    }
                    Button(
                        onClick = {
                            showEvaluationPopup = false
                            viewModel.saveRawCapturedNote()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("AI Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Text(
                text = "Manual Document Ingestion:",
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
                placeholder = { Text("e.g. Codebase Review Specs") },
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
                    label = { Text("Category Group") },
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
                label = { Text("Document Body Text") },
                placeholder = { Text("Type or paste document content here. Athena's AI engine will automatically structure headings, generate checkboxes, write concise summaries, and link matching tags.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
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
                    .height(48.dp)
                    .testTag("save_manual_note_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Processing Ingestion...")
                } else {
                    Icon(Icons.Default.SaveAlt, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Analyze & Sync to Second Brain",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
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
                contentDescription = "Permission Required",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Camera Viewfinder Access",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Grant camera permission to see live feed, or use high-fidelity device simulation.",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
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
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Simulation Viewport Ready",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Text(
            text = "Select one of the document templates above to align simulated viewfinder.",
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
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "EXTRACTING KNOWLEDGE...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusMessage,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
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
