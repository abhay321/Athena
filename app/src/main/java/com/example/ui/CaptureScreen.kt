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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    var isSimulatedScanner by remember { mutableStateOf(true) }
    var scannerTabIndex by remember { mutableStateOf(1) } // 0 = Single Snapshot, 1 = Live Scroll Capture (matches Image 1 as default)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Custom High-Fidelity Pill Segment Controller ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13141F), RoundedCornerShape(24.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSimulatedScanner) Color(0xFF2C2D3B) else Color.Transparent)
                    .clickable { isSimulatedScanner = true }
                    .padding(vertical = 10.dp)
                    .testTag("segment_simulated_scanner"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = if (isSimulatedScanner) Color.White else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Simulated Scanner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isSimulatedScanner) Color.White else Color.Gray
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (!isSimulatedScanner) Color(0xFF2C2D3B) else Color.Transparent)
                    .clickable { isSimulatedScanner = false }
                    .padding(vertical = 10.dp)
                    .testTag("segment_direct_input"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = if (!isSimulatedScanner) Color.White else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Direct Input",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (!isSimulatedScanner) Color.White else Color.Gray
                    )
                }
            }
        }

        // --- 2. Multi-Level Navigation Render ---
        if (isSimulatedScanner) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Secondary Underline TabRow (Matches Screenshot 1)
                TabRow(
                    selectedTabIndex = scannerTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF3F8CFF),
                    divider = {},
                    indicator = { tabPositions ->
                        if (scannerTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[scannerTabIndex]),
                                height = 3.dp,
                                color = Color(0xFF3F8CFF)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = scannerTabIndex == 0,
                        onClick = { scannerTabIndex = 0 },
                        text = { Text("Single Snapshot", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        icon = { Icon(Icons.Default.CropFree, contentDescription = "Single Snapshot") },
                        selectedContentColor = Color(0xFF3F8CFF),
                        unselectedContentColor = Color.Gray
                    )
                    Tab(
                        selected = scannerTabIndex == 1,
                        onClick = { scannerTabIndex = 1 },
                        text = { Text("Live Scroll Capture", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        icon = { Icon(Icons.Default.Layers, contentDescription = "Live Scroll Capture") },
                        selectedContentColor = Color(0xFF3F8CFF),
                        unselectedContentColor = Color.Gray
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (scannerTabIndex) {
                        0 -> SingleScanView(viewModel)
                        1 -> LiveScrollCaptureView(viewModel)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                DirectInputView(viewModel)
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
                            color = if (isSelected) Color(0xFF3F8CFF) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF3F8CFF).copy(alpha = 0.12f)
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
                                tint = Color(0xFF3F8CFF),
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

                Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Box(modifier = Modifier.align(Alignment.TopStart).size(20.dp).border(2.dp, Color(0xFF3F8CFF), RoundedCornerShape(topStart = 6.dp)))
                    Box(modifier = Modifier.align(Alignment.TopEnd).size(20.dp).border(2.dp, Color(0xFF3F8CFF), RoundedCornerShape(topEnd = 6.dp)))
                    Box(modifier = Modifier.align(Alignment.BottomStart).size(20.dp).border(2.dp, Color(0xFF3F8CFF), RoundedCornerShape(bottomStart = 6.dp)))
                    Box(modifier = Modifier.align(Alignment.BottomEnd).size(20.dp).border(2.dp, Color(0xFF3F8CFF), RoundedCornerShape(bottomEnd = 6.dp)))

                    Text(
                        text = "TARGET: ${selectedTemplate!!.title.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF3F8CFF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

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
                                    containerColor = if (!useSimulationFallback) Color(0xFF3F8CFF) else Color.Transparent,
                                    contentColor = if (!useSimulationFallback) Color.White else Color.Gray
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
                                    containerColor = if (useSimulationFallback) Color(0xFF3F8CFF) else Color.Transparent,
                                    contentColor = if (useSimulationFallback) Color.White else Color.Gray
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
                                        colors = listOf(Color.Transparent, Color(0xFF3F8CFF), Color.Transparent)
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

        Button(
            onClick = { selectedTemplate?.let { viewModel.startOcrScan(it) } },
            enabled = selectedTemplate != null && !isScanning,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("initiate_ocr_scan_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F8CFF))
        ) {
            Icon(Icons.Default.DocumentScanner, contentDescription = "Scan", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Capture & Process Document", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveScrollCaptureView(viewModel: BrainViewModel) {
    val ocrSimulator = remember { OcrSimulator() }
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val statusMessage by viewModel.scanningStatusMessage.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val isContinuousActive by viewModel.isContinuousActive.collectAsState()
    val continuousSegments by viewModel.continuousSegments.collectAsState()
    val continuousStatus by viewModel.continuousStatus.collectAsState()
    val continuousTitle by viewModel.continuousTitle.collectAsState()
    val continuousCategory by viewModel.continuousCategory.collectAsState()

    var localTitleInput by remember { mutableStateOf("Jira & Confluence Scroll Ingest1") }
    var localCategoryInput by remember { mutableStateOf("Project Plans") }

    var activeScrollSessionIndex by remember { mutableStateOf(0) }
    var currentScrollFrameIndex by remember { mutableStateOf(0) }
    var showEvaluationPopup by remember { mutableStateOf(false) }

    if (!isContinuousActive) {
        // --- Ingestion Configuration Form ---
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
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFF3F8CFF))
                    Text(
                        "Live Scrolling Work Ingest",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Point camera or align your workspace feeds. Athena compiles overlapping captured segments, filters redundancies, to assemble a complete synchronized text block.",
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F8CFF))
                ) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Continuous Ingestion Session", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // --- Redesigned Active Session Layout (Matches Screenshot 1 exactly) ---
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Session Title (In blue, matches Image 1)
            Text(
                text = "Active Session: $continuousTitle",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF3F8CFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic Simulated Feed Selector (Allows user to select which data feed they scroll through!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF13141F), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "FEED SOURCE:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeScrollSessionIndex == 0) Color(0xFF2C2D3B) else Color.Transparent)
                        .clickable {
                            activeScrollSessionIndex = 0
                            currentScrollFrameIndex = 0
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Jira Feed", fontSize = 11.sp, color = if (activeScrollSessionIndex == 0) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeScrollSessionIndex == 1) Color(0xFF2C2D3B) else Color.Transparent)
                        .clickable {
                            activeScrollSessionIndex = 1
                            currentScrollFrameIndex = 0
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("GitHub Feed", fontSize = 11.sp, color = if (activeScrollSessionIndex == 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                }
            }

            // Big Blue Trigger Button
            Button(
                onClick = {
                    val activeStream = ocrSimulator.scrollSessions[activeScrollSessionIndex]
                    val activeSegment = activeStream[currentScrollFrameIndex]
                    viewModel.captureContinuousSegment(activeSegment.text)
                    currentScrollFrameIndex = (currentScrollFrameIndex + 1) % activeStream.size
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F8CFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Scroll Screen & Capture Overlapping Frame", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }

            // Console Window Card "LIVE ATHENA PIPELINE CONSOLE"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF07080D)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE ATHENA PIPELINE CONSOLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8C96A8),
                                letterSpacing = 1.sp
                            )
                        )
                        Surface(
                            color = Color.Red.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                                Text("RECORDING", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val logLines = remember(continuousSegments.size, continuousStatus) {
                            val list = mutableListOf<String>()
                            if (continuousSegments.isEmpty()) {
                                list.add("• Continuous scan session initialized. Ready for feed...")
                                list.add("• [System] Standby mode active. Press 'Scroll Screen' above to ingest.")
                            } else {
                                list.add("• Frame #${continuousSegments.size} Captured! Detected text segments. Continue scrolling...")
                                if (continuousSegments.size > 1) {
                                    list.add("• Comparing Jaccard index sliding overlap window... Match > 85%")
                                    list.add("• Pruned duplicate strings from previous frame segment buffer.")
                                }
                                list.add("• [Frame #1] Extracted segment text into second brain merge buffer. Status: OK.")
                            }
                            list
                        }
                        logLines.forEach { line ->
                            Text(
                                text = line,
                                color = Color(0xFF00FF66),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Side-by-Side Action Buttons (Discard Session & Merge & Sync)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Discard Session
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable { viewModel.cancelContinuousSession() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF3F8CFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Discard Session",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text("Discard", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F8CFF))
                            Text("Session", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F8CFF))
                        }
                    }
                }

                // Merge & Sync to AI Note
                Card(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(56.dp)
                        .clickable(enabled = continuousSegments.isNotEmpty()) {
                            viewModel.stopAndProcessContinuousSession()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (continuousSegments.isNotEmpty()) Color(0xFF00C853) else Color(0xFF00C853).copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Merge & Sync to AI Note",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text("Merge & Sync to", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("AI Note", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Bottom Stitched Evaluation & Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (continuousSegments.isNotEmpty()) {
                            showEvaluationPopup = true
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonChecked,
                            contentDescription = null,
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Stitched Evaluation & Preview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF00C853)
                        )
                    }
                    Text(
                        text = "Evaluate and verify the aggregated continuous text buffer before running the final structured AI analysis.",
                        fontSize = 11.sp,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (continuousSegments.isEmpty()) 0f else 1f },
                        color = Color(0xFF00C853),
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }

    if (showEvaluationPopup) {
        val deduplicatedText = viewModel.getDeduplicatedContinuousText()

        AlertDialog(
            onDismissRequest = { showEvaluationPopup = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Pageview,
                    contentDescription = "Review",
                    tint = Color(0xFF3F8CFF),
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
                        color = Color(0xFF3F8CFF),
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F8CFF)),
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F8CFF))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Processing Ingestion...")
                } else {
                    Icon(Icons.Default.SaveAlt, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analyze & Sync to Second Brain", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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
                tint = Color(0xFF3F8CFF),
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F8CFF))
                ) {
                    Text("Grant Permission")
                }
                OutlinedButton(
                    onClick = onUseSimulation,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
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
            tint = Color(0xFF3F8CFF),
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
                color = Color(0xFF3F8CFF),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "EXTRACTING KNOWLEDGE...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3F8CFF)
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
                color = Color(0xFF3F8CFF),
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
