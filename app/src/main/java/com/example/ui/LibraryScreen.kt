package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CapturedDocument
import com.example.viewmodel.BrainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: BrainViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.filteredDocuments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    
    val allCategories by viewModel.allCategories.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    val activeSelectedDoc by viewModel.selectedDoc.collectAsState()

    // Date formatter
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()) }

    Box(modifier = modifier.fillMaxSize()) {
        if (activeSelectedDoc != null) {
            // Document Detail Viewer (Overlay Fullscreen Pane)
            DocumentDetailsPane(
                doc = activeSelectedDoc!!,
                viewModel = viewModel,
                dateFormatter = dateFormatter,
                onClose = { viewModel.selectedDoc.value = null }
            )
        } else {
            // Main Library Workspace
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search title, text, or tags inside brain...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )

                // Category Chips Selector
                Text(
                    text = "Filter Categories:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectedCategory.value = null },
                            label = { Text("All") }
                        )
                    }
                    items(allCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.selectedCategory.value = if (selectedCategory == cat) null else cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Tag Filter Row
                if (allTags.isNotEmpty()) {
                    Text(
                        text = "Filter Tags:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { viewModel.selectedTag.value = null },
                                label = { Text("None") }
                            )
                        }
                        items(allTags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { viewModel.selectedTag.value = if (selectedTag == tag) null else tag },
                                label = { Text("#$tag") }
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Library List
                if (documents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Matching Brain Files",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Try clearing filters, searching different keywords, or capturing a new file using the scanner.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(documents) { doc ->
                            LibraryDocumentCard(
                                doc = doc,
                                dateText = dateFormatter.format(Date(doc.createdAt)),
                                onToggleFavorite = { viewModel.toggleFavorite(doc) },
                                onDelete = { viewModel.deleteDocument(doc) },
                                onClick = { viewModel.selectedDoc.value = doc }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryDocumentCard(
    doc: CapturedDocument,
    dateText: String,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("doc_item_card_${doc.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = doc.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Bookmark",
                            tint = if (doc.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = doc.title,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = doc.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Tags
            if (doc.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    doc.tags.split(",").take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#${tag.trim()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun DocumentDetailsPane(
    doc: CapturedDocument,
    viewModel: BrainViewModel,
    dateFormatter: SimpleDateFormat,
    onClose: () -> Unit
) {
    var detailTab by remember { mutableStateOf("markdown") } // "markdown", "flashcards", "todos"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Pane Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Brain Ingest Workspace",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = {
                viewModel.toggleFavorite(doc)
            }) {
                Icon(
                    imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (doc.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

        // Document Identity Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = doc.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = dateFormatter.format(Date(doc.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = doc.title,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Sub-tabs inside Details Pane
        TabRow(
            selectedTabIndex = when (detailTab) {
                "markdown" -> 0
                "flashcards" -> 1
                else -> 2
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = detailTab == "markdown",
                onClick = { detailTab = "markdown" },
                text = { Text("MD Document") },
                icon = { Icon(Icons.Default.Description, contentDescription = "Markdown") }
            )
            Tab(
                selected = detailTab == "flashcards",
                onClick = { detailTab = "flashcards" },
                text = { Text("Study Cards") },
                icon = { Icon(Icons.Default.Quiz, contentDescription = "Flashcards") }
            )
            Tab(
                selected = detailTab == "todos",
                onClick = { detailTab = "todos" },
                text = { Text("Task Items") },
                icon = { Icon(Icons.Default.Checklist, contentDescription = "Tasks") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (detailTab) {
                "markdown" -> DocumentMarkdownView(doc)
                "flashcards" -> StudyFlashcardsView(doc, viewModel)
                "todos" -> ActionItemsTodoView(doc, viewModel)
            }
        }

        // Bottom contextual action
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    viewModel.chatNotebookMode.value = true
                    viewModel.activeTab.value = "chat"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Query Document")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notebook Chat: Query this document with AI", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DocumentMarkdownView(doc: CapturedDocument) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🧠 AI Core Synthesis",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = doc.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Render Markdown Content simply and cleanly
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Structured Markdown Summary",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Render lines simply
                    doc.markdown.lines().forEach { line ->
                        if (line.isNotBlank()) {
                            val trimmed = line.trim()
                            when {
                                trimmed.startsWith("# ") -> {
                                    Text(
                                        text = trimmed.replace("# ", ""),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    )
                                }
                                trimmed.startsWith("## ") -> {
                                    Text(
                                        text = trimmed.replace("## ", ""),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                    )
                                }
                                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = trimmed.substring(2),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = trimmed,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun StudyFlashcardsView(doc: CapturedDocument, viewModel: BrainViewModel) {
    val flashcards = remember(doc) { viewModel.deserializeFlashcards(doc.flashcards) }

    if (flashcards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No study flashcards generated for this note.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Interactive Memory Recall Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Test your retention with AI-generated memory hooks. Click cards to reveal answers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(flashcards) { card ->
                var revealed by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { revealed = !revealed },
                    colors = CardDefaults.cardColors(
                        containerColor = if (revealed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "FLASHCARD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Visibility",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = card.question,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        AnimatedVisibility(
                            visible = revealed,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Divider(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Text(
                                    text = card.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionItemsTodoView(doc: CapturedDocument, viewModel: BrainViewModel) {
    val items = remember(doc) { viewModel.deserializeList(doc.actionItems) }

    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No actionable item checklists found in this document.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Actionable Tasks Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Check off captured tasks directly. Your updates persist instantly offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(items) { index, item ->
                val isChecked = item.startsWith("[x]") || item.startsWith("[X]")
                val cleanItem = item.replace("[ ]", "").replace("[x]", "").replace("[X]", "").trim()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    viewModel.updateActionItemCheck(doc, index, checked)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cleanItem,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
