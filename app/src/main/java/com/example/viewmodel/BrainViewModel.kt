package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = DocumentRepository(database.documentDao())
    private val geminiService: GeminiService = GeminiServiceImpl()
    private val ocrSimulator = OcrSimulator()
    val selectedOcrEngineType = MutableStateFlow(OcrEngineType.GOOGLE_ML_KIT)

    fun getOcrEngine(type: OcrEngineType): OcrService {
        return when (type) {
            OcrEngineType.GOOGLE_ML_KIT -> GoogleMlKitOcrEngine()
            OcrEngineType.PADDLE_OCR -> PaddleOcrEngine()
            OcrEngineType.TESSERACT -> TesseractOcrEngine()
            OcrEngineType.SIMULATOR -> OcrSimulator()
        }
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // --- Core State Observables ---
    val allDocuments = repository.allDocuments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRelationships = repository.allRelationships.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allChatMessages = repository.allChatMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Search, Filtering & Selection State ---
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedTag = MutableStateFlow<String?>(null)
    val selectedDoc = MutableStateFlow<CapturedDocument?>(null)

    // --- UI Layout & Navigation State ---
    val activeTab = MutableStateFlow("dashboard") // "dashboard", "capture", "library", "chat", "settings"
    val activeCaptureMode = MutableStateFlow("camera") // "camera" or "text"

    // --- Simulated OCR Scanning State ---
    val isScanning = MutableStateFlow(false)
    val scanProgress = MutableStateFlow(0f)
    val scanningStatusMessage = MutableStateFlow("")
    val selectedTemplate = MutableStateFlow<OcrTemplate?>(null)

    // --- Chat State ---
    val chatInput = MutableStateFlow("")
    val isChatLoading = MutableStateFlow(false)
    val chatNotebookMode = MutableStateFlow(false) // If true, chats over selectedDoc. Otherwise global RAG.

    // --- Manual text input capture state ---
    val manualTitle = MutableStateFlow("")
    val manualContent = MutableStateFlow("")
    val manualCategory = MutableStateFlow("Personal")
    val isSavingManual = MutableStateFlow(false)

    // --- Stats & Derived States ---
    val filteredDocuments = combine(
        allDocuments,
        searchQuery,
        selectedCategory,
        selectedTag
    ) { docs, query, category, tag ->
        docs.filter { doc ->
            val matchesQuery = query.isEmpty() ||
                    doc.title.contains(query, ignoreCase = true) ||
                    doc.rawText.contains(query, ignoreCase = true) ||
                    doc.tags.contains(query, ignoreCase = true)

            val matchesCategory = category == null || doc.category == category
            val matchesTag = tag == null || doc.tags.split(",").map { it.trim() }.contains(tag)

            matchesQuery && matchesCategory && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories = allDocuments.map { docs ->
        docs.map { it.category }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags = allDocuments.map { docs ->
        docs.flatMap { it.tags.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically pre-populate database with some sample documents on first-launch
        viewModelScope.launch {
            allDocuments.first { docs ->
                if (docs.isEmpty()) {
                    prePopulateSampleData()
                }
                true
            }
        }
    }

    private suspend fun prePopulateSampleData() = withContext(Dispatchers.IO) {
        scanningStatusMessage.value = "Initializing Brain..."
        
        // 1. Core Chat Architecture
        val doc1Result = geminiService.analyzeDocument(ocrSimulator.templates[0].text)
        val doc1Id = repository.insertDocument(
            CapturedDocument(
                title = ocrSimulator.templates[0].title,
                rawText = ocrSimulator.templates[0].text,
                markdown = doc1Result.markdown,
                structuredJson = "",
                imagePath = ocrSimulator.templates[0].id,
                category = ocrSimulator.templates[0].category,
                summary = doc1Result.summary,
                tags = doc1Result.tags.joinToString(", "),
                actionItems = serializeList(doc1Result.actionItems),
                flashcards = serializeFlashcards(doc1Result.flashcards)
            )
        )

        // 2. Kotlin Coroutines Textbook
        val doc2Result = geminiService.analyzeDocument(ocrSimulator.templates[1].text)
        val doc2Id = repository.insertDocument(
            CapturedDocument(
                title = ocrSimulator.templates[1].title,
                rawText = ocrSimulator.templates[1].text,
                markdown = doc2Result.markdown,
                structuredJson = "",
                imagePath = ocrSimulator.templates[1].id,
                category = ocrSimulator.templates[1].category,
                summary = doc2Result.summary,
                tags = doc2Result.tags.joinToString(", "),
                actionItems = serializeList(doc2Result.actionItems),
                flashcards = serializeFlashcards(doc2Result.flashcards)
            )
        )

        // 3. AI Presentation
        val doc3Result = geminiService.analyzeDocument(ocrSimulator.templates[2].text)
        val doc3Id = repository.insertDocument(
            CapturedDocument(
                title = ocrSimulator.templates[2].title,
                rawText = ocrSimulator.templates[2].text,
                markdown = doc3Result.markdown,
                structuredJson = "",
                imagePath = ocrSimulator.templates[2].id,
                category = ocrSimulator.templates[2].category,
                summary = doc3Result.summary,
                tags = doc3Result.tags.joinToString(", "),
                actionItems = serializeList(doc3Result.actionItems),
                flashcards = serializeFlashcards(doc3Result.flashcards)
            )
        )

        // Establish connections in Knowledge Graph
        repository.insertRelationship(BrainRelationship(sourceDocId = doc1Id, targetDocId = doc3Id, relationshipType = "Infrastructure Plan", strength = 0.8f))
        repository.insertRelationship(BrainRelationship(sourceDocId = doc2Id, targetDocId = doc3Id, relationshipType = "Implementation Language", strength = 0.6f))
    }

    // --- Action Methods ---

    fun selectTemplate(template: OcrTemplate) {
        selectedTemplate.value = template
    }

    fun startOcrScan(template: OcrTemplate) {
        viewModelScope.launch {
            selectedTemplate.value = template
            isScanning.value = true
            scanProgress.value = 0.0f
            scanningStatusMessage.value = "Focusing lens & calibrating contrast..."
            
            // Phase 1: Camera Focus Simulation
            delay(800)
            scanProgress.value = 0.2f
            scanningStatusMessage.value = "Performing perspective correction & cropping..."
            
            // Phase 2: Engine Initialization
            val engineType = selectedOcrEngineType.value
            val engine = getOcrEngine(engineType)
            
            delay(1000)
            scanProgress.value = 0.45f
            scanningStatusMessage.value = when (engineType) {
                OcrEngineType.GOOGLE_ML_KIT -> "Initializing Google Play Services OCR engine..."
                OcrEngineType.PADDLE_OCR -> "Loading PaddleOCR C++ JNI libraries and .pdmodel weights..."
                OcrEngineType.TESSERACT -> "Verifying English traineddata offline lexicon inside sandbox..."
                OcrEngineType.SIMULATOR -> "Activating local high-fidelity template simulator..."
            }
            
            // Phase 3: Text Extraction
            delay(1200)
            scanProgress.value = 0.7f
            val ocrResult = engine.processImage(template.id)
            scanningStatusMessage.value = "Running Character Recognition [${engineType.displayName}] (Confidence: ${(ocrResult.confidence * 100).toInt()}%)..."
            
            delay(1000)
            scanProgress.value = 0.9f
            scanningStatusMessage.value = "Athena AI: Parsing document layout, summaries, and action cards..."
            
            val rawText = ocrResult.rawText
            val result = geminiService.analyzeDocument(rawText)
            
            // Phase 4: Document Ingestion
            delay(1000)
            scanProgress.value = 1.0f
            scanningStatusMessage.value = "Synced with Athena Brain via ${engineType.displayName}!"
            
            val newDocId = repository.insertDocument(
                CapturedDocument(
                    title = result.title,
                    rawText = rawText,
                    markdown = result.markdown,
                    structuredJson = "",
                    imagePath = template.id,
                    category = template.category,
                    summary = result.summary,
                    tags = result.tags.joinToString(", "),
                    actionItems = serializeList(result.actionItems),
                    flashcards = serializeFlashcards(result.flashcards)
                )
            )

            // Auto Linker: scan other documents to find overlapping tags to dynamically grow the Knowledge Graph
            allDocuments.value.forEach { existingDoc ->
                if (existingDoc.id != newDocId) {
                    val sharedTags = existingDoc.tags.split(",").map { it.trim() }
                        .intersect(result.tags.map { it.trim() }.toSet())
                    if (sharedTags.isNotEmpty()) {
                        repository.insertRelationship(
                            BrainRelationship(
                                sourceDocId = newDocId,
                                targetDocId = existingDoc.id,
                                relationshipType = "Shared concepts: ${sharedTags.take(2).joinToString(", ")}",
                                strength = 0.5f + (sharedTags.size * 0.1f).coerceAtMost(0.5f)
                            )
                        )
                    }
                }
            }

            delay(600)
            isScanning.value = false
            // Navigate to Library and select the new document
            val newlyCreatedDoc = repository.getDocumentById(newDocId)
            if (newlyCreatedDoc != null) {
                selectedDoc.value = newlyCreatedDoc
                activeTab.value = "library"
            }
        }
    }

    fun captureManualText() {
        val title = manualTitle.value.trim()
        val text = manualContent.value.trim()
        val category = manualCategory.value

        if (title.isEmpty() || text.isEmpty()) return

        viewModelScope.launch {
            isSavingManual.value = true
            val result = geminiService.analyzeDocument(text)
            
            val newDocId = repository.insertDocument(
                CapturedDocument(
                    title = title,
                    rawText = text,
                    markdown = result.markdown,
                    structuredJson = "",
                    imagePath = "manual",
                    category = category,
                    summary = result.summary,
                    tags = result.tags.joinToString(", "),
                    actionItems = serializeList(result.actionItems),
                    flashcards = serializeFlashcards(result.flashcards)
                )
            )

            // Link matching tags
            allDocuments.value.forEach { existingDoc ->
                if (existingDoc.id != newDocId) {
                    val sharedTags = existingDoc.tags.split(",").map { it.trim() }
                        .intersect(result.tags.map { it.trim() }.toSet())
                    if (sharedTags.isNotEmpty()) {
                        repository.insertRelationship(
                            BrainRelationship(
                                sourceDocId = newDocId,
                                targetDocId = existingDoc.id,
                                relationshipType = "Related",
                                strength = 0.6f
                            )
                        )
                    }
                }
            }

            // Clear inputs
            manualTitle.value = ""
            manualContent.value = ""
            isSavingManual.value = false
            
            val newlyCreatedDoc = repository.getDocumentById(newDocId)
            if (newlyCreatedDoc != null) {
                selectedDoc.value = newlyCreatedDoc
                activeTab.value = "library"
            }
        }
    }

    fun toggleFavorite(document: CapturedDocument) {
        viewModelScope.launch {
            val updated = document.copy(isFavorite = !document.isFavorite)
            repository.updateDocument(updated)
            if (selectedDoc.value?.id == document.id) {
                selectedDoc.value = updated
            }
        }
    }

    fun deleteDocument(document: CapturedDocument) {
        viewModelScope.launch {
            repository.deleteDocumentById(document.id)
            if (selectedDoc.value?.id == document.id) {
                selectedDoc.value = null
            }
        }
    }

    fun sendChatMessage() {
        val input = chatInput.value.trim()
        if (input.isEmpty()) return

        viewModelScope.launch {
            // 1. Save user message to database
            repository.insertChatMessage(ChatMessage(role = "user", content = input))
            chatInput.value = ""
            isChatLoading.value = true

            // 2. Fetch context documents for RAG
            val contextDocs = if (chatNotebookMode.value && selectedDoc.value != null) {
                // Chat over single active document (Notebook Chat)
                listOf(selectedDoc.value!!)
            } else {
                // Global Chat: Retrieve relevant documents (basic lexical match over knowledge base)
                allDocuments.value.filter { doc ->
                    input.split(" ").any { word ->
                        word.length > 3 && (doc.title.contains(word, ignoreCase = true) || doc.rawText.contains(word, ignoreCase = true))
                    }
                }.take(3).ifEmpty {
                    // Fallback: take recent 3 documents
                    allDocuments.value.take(3)
                }
            }

            // 3. Query Gemini AI Service
            val chatHistory = allChatMessages.value + ChatMessage(role = "user", content = input)
            val reply = geminiService.askBrain(chatHistory, contextDocs)

            // 4. Save AI Response to database with citations
            val citedIds = contextDocs.map { it.id }.joinToString(",")
            repository.insertChatMessage(ChatMessage(role = "assistant", content = reply, citedDocumentIds = citedIds))
            isChatLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    fun updateActionItemCheck(doc: CapturedDocument, itemIndex: Int, checked: Boolean) {
        viewModelScope.launch {
            val items = deserializeList(doc.actionItems).toMutableList()
            if (itemIndex >= 0 && itemIndex < items.size) {
                val current = items[itemIndex]
                val updated = if (checked) {
                    if (current.startsWith("[x]") || current.startsWith("[X]")) current
                    else if (current.startsWith("[ ]")) "[x] " + current.substring(3).trim()
                    else "[x] $current"
                } else {
                    if (current.startsWith("[ ]")) current
                    else if (current.startsWith("[x]") || current.startsWith("[X]")) "[ ] " + current.substring(3).trim()
                    else "[ ] $current"
                }
                items[itemIndex] = updated
                val updatedDoc = doc.copy(actionItems = serializeList(items))
                repository.updateDocument(updatedDoc)
                if (selectedDoc.value?.id == doc.id) {
                    selectedDoc.value = updatedDoc
                }
            }
        }
    }

    // --- Serialization Helpers ---

    private fun serializeList(list: List<String>): String {
        val adapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
        return adapter.toJson(list)
    }

    fun deserializeList(json: String): List<String> {
        if (json.isEmpty()) return emptyList()
        return try {
            val adapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeFlashcards(cards: List<OcrFlashcard>): String {
        val adapter = moshi.adapter<List<OcrFlashcard>>(Types.newParameterizedType(List::class.java, OcrFlashcard::class.java))
        return adapter.toJson(cards)
    }

    fun deserializeFlashcards(json: String): List<OcrFlashcard> {
        if (json.isEmpty()) return emptyList()
        return try {
            val adapter = moshi.adapter<List<OcrFlashcard>>(Types.newParameterizedType(List::class.java, OcrFlashcard::class.java))
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
