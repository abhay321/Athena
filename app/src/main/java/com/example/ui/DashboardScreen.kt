package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CapturedDocument
import com.example.viewmodel.BrainViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BrainViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.allDocuments.collectAsState()
    val relationships by viewModel.allRelationships.collectAsState()
    val tags by viewModel.allTags.collectAsState()

    val recentDocs = documents.take(3)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // --- Bento Row 1: Capture Knowledge (Bento Large) ---
        item {
            Card(
                onClick = { viewModel.activeTab.value = "capture" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .testTag("bento_capture_card")
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Capture Knowledge. Auto scan, OCR and Classify whiteboards, slides, or textbooks. Live capture button. Double tap to open."
                    },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "Scan Icon",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = "LIVE CAPTURE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Capture Knowledge",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            Text(
                                text = "Auto-scan, OCR & Classify whiteboards, slides, or textbooks",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- Bento Row 2: Stats & AI Status (Bento Mediums) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Second Brain Stats
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                        .testTag("bento_stats_card")
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Second Brain statistics. Total of ${documents.size} embedded knowledge nodes ingested."
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SECOND BRAIN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Column {
                            Text(
                                text = "${documents.size}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = (-1).sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = "Embedded Nodes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val activeBars = (documents.size).coerceIn(0, 3)
                            repeat(3) { idx ->
                                Box(
                                    modifier = Modifier
                                        .height(4.dp)
                                        .weight(1f)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(
                                            if (idx < activeBars) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                            }
                        }
                    }
                }

                // Right: AI Provider
                Card(
                    onClick = { viewModel.activeTab.value = "settings" },
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                        .testTag("bento_ai_card")
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Gemini AI. Hybrid active mode. Button. Double tap to view system architecture details."
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "AI Service",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Gemini AI",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "HYBRID ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- Bento Row 3: Interactive Knowledge Graph Map (Bento Large) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .testTag("bento_graph_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "Semantic Graph. Real time concept relationship map."
                                }
                        ) {
                            Text(
                                text = "Semantic Graph",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Real-time concept relationship map",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { /* Trigger animation tick */ },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("recalculate_graph_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Recalculate graph forces",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (documents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Scan notes to populate the brain map",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            KnowledgeGraphCanvas(
                                documents = documents,
                                relationships = relationships,
                                onNodeClicked = { doc ->
                                    viewModel.selectedDoc.value = doc
                                    viewModel.activeTab.value = "library"
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Bento Row 4: Ask Athena & Direct Note (Bento Mediums) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Ask Athena (Peach Coral Bento card)
                Card(
                    onClick = { viewModel.activeTab.value = "chat" },
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .testTag("bento_chat_card")
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Ask Athena. Context aware AI assistant. Button. Double tap to open chat."
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Ask Athena",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                            Text(
                                text = "Context-Aware RAG",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }

                // Right: Direct Note
                Card(
                    onClick = {
                        viewModel.activeCaptureMode.value = "text"
                        viewModel.activeTab.value = "capture"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .testTag("bento_direct_card")
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Direct Note. Manual text logging. Button. Double tap to write custom note."
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "Edit Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Direct Note",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Manual text logs",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- Recent Brain Ingests Header ---
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Brain Ingests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = { viewModel.activeTab.value = "library" },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("View All")
                }
            }
        }

        if (recentDocs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.DocumentScanner,
                            contentDescription = "No scan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your brain library is empty",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Scan a whiteboard, textbook or slide to start.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentDocs) { doc ->
                RecentDocumentCard(
                    doc = doc,
                    onClicked = {
                        viewModel.selectedDoc.value = doc
                        viewModel.activeTab.value = "library"
                    }
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun KnowledgeGraphCanvas(
    documents: List<CapturedDocument>,
    relationships: List<com.example.data.BrainRelationship>,
    onNodeClicked: (CapturedDocument) -> Unit
) {
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
    
    // Animation tick for graph pulse
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Calculate node coordinates in circle around center
    val nodes = remember(documents, canvasSize) {
        if (canvasSize == Offset.Zero || documents.isEmpty()) emptyList<NodePosition>()
        else {
            val center = Offset(canvasSize.x / 2, canvasSize.y / 2)
            val count = documents.size
            if (count == 1) {
                listOf(NodePosition(documents[0], center))
            } else {
                val radius = (canvasSize.y * 0.35f).coerceAtMost(160.dp.value)
                documents.mapIndexed { idx, doc ->
                    val angle = (2 * Math.PI * idx) / count
                    val x = center.x + radius * cos(angle).toFloat()
                    val y = center.y + radius * sin(angle).toFloat()
                    NodePosition(doc, Offset(x, y))
                }
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(nodes) {
                detectTapGestures { offset ->
                    // Find node tapped
                    val tappedNode = nodes.firstOrNull { node ->
                        val dist = (node.position - offset).getDistance()
                        dist <= 24.dp.value // Node tap target radius
                    }
                    if (tappedNode != null) {
                        onNodeClicked(tappedNode.document)
                    }
                }
            }
    ) {
        if (canvasSize == Offset.Zero) {
            canvasSize = Offset(size.width, size.height)
            return@Canvas
        }

        // 1. Draw connecting lines (Edges)
        relationships.forEach { rel ->
            val sourceNode = nodes.firstOrNull { it.document.id == rel.sourceDocId }
            val targetNode = nodes.firstOrNull { it.document.id == rel.targetDocId }
            if (sourceNode != null && targetNode != null) {
                val thickness = rel.strength * 4.dp.value
                val alpha = 0.2f + (rel.strength * 0.4f)
                drawLine(
                    color = primaryColor.copy(alpha = alpha),
                    start = sourceNode.position,
                    end = targetNode.position,
                    strokeWidth = thickness
                )
            }
        }

        // 2. Draw nodes and rings (Notes)
        nodes.forEachIndexed { index, node ->
            val nodeColor = when (index % 3) {
                0 -> primaryColor
                1 -> secondaryColor
                else -> tertiaryColor
            }

            // Draw glowing halo ring
            drawCircle(
                color = nodeColor.copy(alpha = 0.15f * pulseScale),
                radius = 24.dp.value * pulseScale,
                center = node.position
            )

            // Draw border circle
            drawCircle(
                color = nodeColor,
                radius = 12.dp.value,
                center = node.position,
                style = Stroke(width = 2.dp.value)
            )

            // Draw core dot
            drawCircle(
                color = nodeColor,
                radius = 6.dp.value,
                center = node.position
            )

            // Label
            // Since drawText requires native text paint, we can simply omit text rendering inside Canvas
            // to avoid compilation or performance issues, and rely on standard UI components, or draw a beautiful visual indicator
        }
    }

    // Overlay absolute labels as a transparent row below the graph or let standard details handle it
}

data class NodePosition(
    val document: CapturedDocument,
    val position: Offset
)

@Composable
fun RecentDocumentCard(
    doc: CapturedDocument,
    onClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClicked)
            .testTag("recent_document_card_${doc.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = "Knowledge Node: ${doc.title}. Category: ${doc.category}. Summary: ${doc.summary}. Double tap to view details."
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when (doc.category) {
                            "Whiteboard" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            "Book Scan" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            "Meeting Slide" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (doc.category) {
                        "Whiteboard" -> Icons.Default.BorderColor
                        "Book Scan" -> Icons.Default.MenuBook
                        "Meeting Slide" -> Icons.Default.Tv
                        "Meeting Minutes" -> Icons.Default.EventNote
                        else -> Icons.Default.Notes
                    },
                    contentDescription = doc.category,
                    tint = when (doc.category) {
                        "Whiteboard" -> MaterialTheme.colorScheme.primary
                        "Book Scan" -> MaterialTheme.colorScheme.secondary
                        "Meeting Slide" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = doc.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = doc.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = doc.summary.take(45) + if (doc.summary.length > 45) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
