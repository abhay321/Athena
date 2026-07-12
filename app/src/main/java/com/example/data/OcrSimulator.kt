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

class OcrSimulator : OcrService {

    override val engineType: OcrEngineType = OcrEngineType.SIMULATOR

    val templates: List<OcrTemplate> get() = Companion.templates

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
    }
}
