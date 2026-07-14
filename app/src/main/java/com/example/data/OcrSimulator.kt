package com.example.data

import android.graphics.Rect

data class OcrTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val text: String,
    val imageResName: String
)

data class ScrollSegment(
    val id: String,
    val title: String,
    val pageNumber: Int,
    val text: String
)

class OcrSimulator : OcrService {

    override val engineType: OcrEngineType = OcrEngineType.SIMULATOR

    val templates: List<OcrTemplate> get() = Companion.templates
    val scrollSessions: List<List<ScrollSegment>> get() = Companion.scrollSessions

    private var isInitialized = false

    override suspend fun initialize(): Boolean {
        isInitialized = true
        return true
    }

    override suspend fun release() {
        isInitialized = false
    }

    override suspend fun isAvailable(): Boolean = true

    override suspend fun extractText(imageIdentifier: String): String {
        return processImage(imageIdentifier).rawText
    }

    override suspend fun processImage(imageIdentifier: String, options: OcrOptions): OcrResult {
        val template = templates.firstOrNull { it.id == imageIdentifier || it.title == imageIdentifier }
            ?: return OcrResult(
                rawText = "No text captured.",
                confidence = 0f,
                durationMs = 15,
                engineUsed = OcrEngineType.SIMULATOR,
                isSuccess = false,
                errorMessage = "Template matching failed for identifier '$imageIdentifier'"
            )

        // Generate mock structured layout blocks
        val lines = template.text.split("\n").filter { it.isNotBlank() }
        val blocks = lines.mapIndexed { idx, lineText ->
            OcrBlock(
                text = lineText.trim(),
                confidence = 0.99f,
                boundingBox = Rect(10, idx * 50, 500, (idx + 1) * 50),
                level = OcrElementLevel.LINE
            )
        }

        return OcrResult(
            rawText = template.text,
            confidence = 0.99f,
            durationMs = 80,
            engineUsed = OcrEngineType.SIMULATOR,
            blocks = blocks,
            isSuccess = true
        )
    }

    companion object {
        val templates = listOf(
            OcrTemplate(
                id = "sys_design",
                title = "Distributed Chat Architecture",
                description = "Whiteboard sketch with architecture diagrams, boxes, and connection arrows.",
                category = "Whiteboard",
                imageResName = "img_onboarding_brain_1783818582428",
                text = """
                    WHITEBOARD NOTES:
                    Topic: Real-time Distributed Chat Architecture
                    - Client Connections: WebSocket protocol with Client-side Keepalive (Heartbeat every 30s).
                    - Gateway Layer: High-performance Go microservice, handles connection termination and authentication.
                    - Message Queue: Apache Kafka for reliable, high-throughput log-structured buffering. Partition key: room_id to preserve absolute order.
                    - Active Users cache: Redis Cluster storing mapping of user_id -> active_gateway_ip.
                    - Main Database: CockroachDB (SQL, distributed transactions, multi-region replication).
                    - Metrics & Telemetry: Prometheus pulling stats, Grafana visualization.
                    TODO Items:
                    - [ ] Implement exponential backoff retry in client reconnection. @John
                    - [ ] Run load tests with 10k concurrent websocket connections. @Sarah
                """.trimIndent()
            ),
            OcrTemplate(
                id = "kotlin_coroutines",
                title = "Kotlin Coroutines Textbook",
                description = "A printed page from a technical software development textbook.",
                category = "Book Scan",
                imageResName = "img_onboarding_brain_1783818582428",
                text = """
                    BOOK PAGE 142: ADVANCED KOTLIN COROUTINES
                    In Kotlin, structured concurrency ensures that new coroutines are only launched in a specific CoroutineScope, which delimits the coroutine's lifetime.
                    Key mechanisms include:
                    1. coroutineScope { ... } - creates a sub-scope. If any child coroutine fails, the scope is immediately cancelled along with all other children.
                    2. supervisorScope { ... } - similar to coroutineScope, but child failures do not cascade. A failing child coroutine does not cancel its siblings or the parent scope. This is extremely useful for running independent concurrent tasks like image processing or database writes.
                    3. Dispatchers:
                       - Dispatchers.Default: optimized for CPU-intensive work (sorting lists, parsing complex JSON).
                       - Dispatchers.IO: optimized for blocking I/O (network requests, file operations, Room database writes).
                    Exercises:
                    - [ ] Implement a custom supervisor scope to fetch 5 API endpoints concurrently.
                    - [ ] Explain why Dispatchers.Main should never be blocked in Android.
                """.trimIndent()
            ),
            OcrTemplate(
                id = "ai_strategy",
                title = "Generative AI Platform Strategy",
                description = "Presentation slide from an enterprise AI deployment blueprint.",
                category = "Meeting Slide",
                imageResName = "img_onboarding_brain_1783818582428",
                text = """
                    PRESENTATION SLIDE: ENTERPRISE AI ROADMAP (Q3-Q4)
                    Overview: Integrating LLMs into business workflows securely and cost-effectively.
                    Core Pillars:
                    - Local Execution: Deploying open GGUF models (e.g. Llama 3 8B, Qwen 2.5) via Ollama and vLLM to minimize cloud API costs and guarantee 100% data privacy.
                    - Hybrid Search (RAG): Combining dense vector retrieval (Nomic Embeddings, Qdrant DB) with traditional lexical indexing (Full-Text SQLite) to produce context-aware, verifiable answers.
                    - Modular Middleware: Vendor-agnostic ports and adapters framework, ensuring zero vendor lock-in.
                    Financial Projections:
                    - 70% reduction in API token fees through prompt caching and local offline routing.
                    - [ ] Draft security evaluation report for local GGUF models. @SecurityTeam
                """.trimIndent()
            ),
            OcrTemplate(
                id = "meeting_sync",
                title = "Athena Sprint Planning Sync",
                description = "Handwritten meeting minutes on a physical paper pad.",
                category = "Meeting Minutes",
                imageResName = "img_onboarding_brain_1783818582428",
                text = """
                    MEETING MINUTES - PROJECT ATHENA SYNC
                    Date: July 11, 2026
                    Attendants: Alex, Chloe, Marcus, Zoe
                    Decisions Made:
                    - Choose SQLite + Room for the local-first storage layer to support robust offline editing on tablet and mobile.
                    - Use Retrofit to connect to standard REST APIs for Gemini or other pluggable models.
                    - Embeddings will use the pluggable SentenceTransformers or Gemini Embeddings interface depending on offline/online toggle.
                    Action Items:
                    - [ ] Set up the Room database entities for notes, relationships, and chat. @Marcus (Target: Friday)
                    - [ ] Design the adaptive M3 Compose dashboard with the custom Cosmic Slate dark theme. @Alex
                """.trimIndent()
            )
        )

        val scrollSessions = listOf(
            listOf(
                ScrollSegment(
                    id = "jira_scroll",
                    title = "Jira Ticket #ATH-291 (Part 1)",
                    pageNumber = 1,
                    text = """
                        JIRA-291: REAL-TIME FRAME DEDUPLICATOR ENGINE
                        Status: In Progress | Priority: Critical | Assignee: Alex
                        Description: Point camera at screen while scrolling. The system should continuously capture frames, perform OCR, and merge them into a single Second Brain document.
                        Acceptance Criteria:
                        - Real-time video frame extraction (using CameraX Analyzer)
                        - Automatic sliding-window Jaccard distance or Cosine similarity to skip redundant frames
                    """.trimIndent()
                ),
                ScrollSegment(
                    id = "jira_scroll",
                    title = "Jira Ticket #ATH-291 (Part 2)",
                    pageNumber = 2,
                    text = """
                        Acceptance Criteria (continued):
                        - Extract text blocks using local OCR or Gemini Vision
                        - Merge overlapping text pieces to form a continuous markdown note
                        - Extract task items automatically and append them to the checklist
                        Technical Architecture:
                        - Use CameraX `ImageAnalysis` to analyze frames every 500ms
                        - Crop central bounding box to improve text legibility
                    """.trimIndent()
                ),
                ScrollSegment(
                    id = "jira_scroll",
                    title = "Jira Ticket #ATH-291 (Part 3)",
                    pageNumber = 3,
                    text = """
                        Technical Architecture (continued):
                        - Buffer extracted text frames in a sliding history queue
                        - Compare Jaccard index: words_intersection / words_union. If index > 0.82, discard frame as redundant.
                        - Once scrolling is complete, dispatch combined payload to Gemini 3.5 Flash for summarization and tag linking.
                        TODO items:
                        - [ ] Run end-to-end integration test of frame deduplicator. @Alex
                        - [ ] Design custom adaptive Jetpack Compose viewport HUD. @Zoe
                    """.trimIndent()
                )
            ),
            listOf(
                ScrollSegment(
                    id = "github_scroll",
                    title = "GitHub PR #409 (Part 1)",
                    pageNumber = 1,
                    text = """
                        GITHUB PR #409: FEAT: LOCAL VECTOR STORAGE INTEGRATION
                        Branches: `feature/local-rag` -> `main` | Reviewers: Zoe, Chloe
                        Changes: Integrated SQLite FTS5 extension and SQLite-Vec extension to support offline embedding storage and semantic search.
                        Files Modified:
                        - `AppDatabase.kt`
                        - `DocumentDao.kt`
                        - `VectorSearchEngine.kt`
                    """.trimIndent()
                ),
                ScrollSegment(
                    id = "github_scroll",
                    title = "GitHub PR #409 (Part 2)",
                    pageNumber = 2,
                    text = """
                        Key Implementations:
                        - Added `vector_embeddings` column to `CapturedDocument` table.
                        - Implemented local cosine similarity calculation for offline search fallback.
                        - Created `TagCloud` dynamic visualizer on the dashboard.
                        Code Sample:
                        ```kotlin
                        fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
                            var dotProduct = 0f
                            var normA = 0f
                            var normB = 0f
                            for (i in v1.indices) {
                                dotProduct += v1[i] * v2[i]
                                normA += v1[i] * v1[i]
                                normB += v2[i] * v2[i]
                            }
                            return dotProduct / (sqrt(normA) * sqrt(normB))
                        }
                        ```
                    """.trimIndent()
                ),
                ScrollSegment(
                    id = "github_scroll",
                    title = "GitHub PR #409 (Part 3)",
                    pageNumber = 3,
                    text = """
                        Local Cosine Similarity Testing results:
                        - Vector search completes in <5ms for 500 records on device.
                        - Matches semantic keywords successfully without calling remote APIs.
                        TODO items:
                        - [ ] Write unit tests for `cosineSimilarity` mathematical correctness. @Zoe
                        - [ ] Implement incremental indexing on background work manager. @Chloe
                    """.trimIndent()
                )
            )
        )
    }
}
