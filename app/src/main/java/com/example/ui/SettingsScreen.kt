package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OcrEngineType
import com.example.viewmodel.BrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrainViewModel,
    modifier: Modifier = Modifier
) {
    // Local pluggable options selection
    val activeOcrEngine by viewModel.selectedOcrEngineType.collectAsState()
    var selectedLlm by remember { mutableStateOf("Gemini 3.5 Flash") }
    var selectedEmbeddings by remember { mutableStateOf("Nomic Embeddings") }
    var selectedVectorDb by remember { mutableStateOf("SQLite Vector") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Core Architecture Intro
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SettingsInputComponent, contentDescription = "Modularity", tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Modular Ports & Adapters Architecture",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Project Athena is engineered with high-level decoupling. All intelligence, storage, database, and hardware layers are hidden behind strict interfaces. You can hot-swap any provider without modifying core business logic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- OCR Module Provider Settings ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("OCR Extraction Engines", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    
                    val ocrProviders = OcrEngineType.values()
                    ocrProviders.forEach { engineType ->
                        val isActive = activeOcrEngine == engineType
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Active",
                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(engineType.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (isActive) "Active • ${engineType.description}" else engineType.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            RadioButton(
                                selected = isActive,
                                onClick = { viewModel.selectedOcrEngineType.value = engineType },
                                modifier = Modifier.testTag("ocr_radio_${engineType.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // --- LLM Model Provider Settings ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Pluggable LLM Providers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    
                    val llmProviders = listOf("Gemini 3.5 Flash", "Ollama (Local GGUF)", "OpenAI GPT-4o", "Anthropic Claude")
                    llmProviders.forEach { provider ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (provider == "Gemini 3.5 Flash") Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Active",
                                    tint = if (provider == "Gemini 3.5 Flash") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(provider, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = when (provider) {
                                            "Gemini 3.5 Flash" -> "Active • Multi-modal network REST adapter"
                                            "Ollama (Local GGUF)" -> "Pluggable • Local offline reasoning (Future)"
                                            else -> "Future pluggable API adapter"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            RadioButton(
                                selected = selectedLlm == provider,
                                onClick = { selectedLlm = provider },
                                enabled = provider == "Gemini 3.5 Flash" || provider == "Ollama (Local GGUF)"
                            )
                        }
                    }
                }
            }
        }

        // --- Embedding Layer Settings ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Semantic Vector Embeddings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    
                    val embeds = listOf("Nomic Embeddings", "Gemini Embeddings", "SentenceTransformers")
                    embeds.forEach { provider ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (provider == "Nomic Embeddings") Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Active",
                                    tint = if (provider == "Nomic Embeddings") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(provider, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (provider == "Nomic Embeddings") "Active • Default local vector map" else "Pluggable adapter",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            RadioButton(
                                selected = selectedEmbeddings == provider,
                                onClick = { selectedEmbeddings = provider },
                                enabled = provider == "Nomic Embeddings"
                            )
                        }
                    }
                }
            }
        }

        // --- Vector Database Settings ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Vector Database Layer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    
                    val databases = listOf("SQLite Vector", "Qdrant", "Chroma", "Milvus")
                    databases.forEach { db ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (db == "SQLite Vector") Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Active",
                                    tint = if (db == "SQLite Vector") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(db, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (db == "SQLite Vector") "Active • Fast local client side index" else "Future enterprise cloud service",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            RadioButton(
                                selected = selectedVectorDb == db,
                                onClick = { selectedVectorDb = db },
                                enabled = db == "SQLite Vector"
                            )
                        }
                    }
                }
            }
        }

        // --- Security, Privacy & Local Storage Integrity ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Local Security & Privacy Guarantee", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Private", tint = MaterialTheme.colorScheme.secondary)
                        Text("100% Client-Side SQLite Room database. Data is never mined.", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = "Offline first", tint = MaterialTheme.colorScheme.secondary)
                        Text("Fully functional offline fallbacks ensure core notes search, checklists and studies work without an internet connection.", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = "Encrypted", tint = MaterialTheme.colorScheme.secondary)
                        Text("Configured securely using Secrets Gradle Plugin via injected environment variables.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Platform Specs
        item {
            Text(
                text = "Project Athena • Version 1.0.0 (Enterprise MVP)\nEngineered with Jetpack Compose & SQLite",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
