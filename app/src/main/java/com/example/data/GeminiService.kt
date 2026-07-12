package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Models ---

@JsonClass(generateAdapter = true)
data class Part(val text: String? = null)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>?)

// --- Document Understanding Structured JSON ---

@JsonClass(generateAdapter = true)
data class StructuredDocResult(
    val title: String,
    val summary: String,
    val markdown: String,
    val tags: List<String>,
    val actionItems: List<String>,
    val flashcards: List<OcrFlashcard>
)

@JsonClass(generateAdapter = true)
data class OcrFlashcard(
    val question: String,
    val answer: String
)

// --- Retrofit Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Service Interface ---

interface GeminiService {
    suspend fun analyzeDocument(rawText: String): StructuredDocResult
    suspend fun askBrain(messages: List<ChatMessage>, contextDocs: List<CapturedDocument>): String
}

// --- Service Implementation ---

class GeminiServiceImpl : GeminiService {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    override suspend fun analyzeDocument(rawText: String): StructuredDocResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // No API Key, use local fallback parsing
            return@withContext runLocalFallbackAnalysis(rawText)
        }

        val prompt = """
            Analyze the following captured text from a scanned document, whiteboard, or presentation slide.
            Provide a complete, structured analysis in JSON. Return ONLY the JSON object, fitting this schema:
            {
              "title": "String, clear title of the note",
              "summary": "String, 2-3 sentence overview of what this note is about",
              "markdown": "String, fully styled markdown with headers, bullet points, code blocks, bold text, lists",
              "tags": ["Array of Strings", "List of relevant tags or categories"],
              "actionItems": ["Array of Strings", "List of action items, checklist items, or TODOs identified in the text, e.g., 'Draft evaluation report @SecurityTeam'"],
              "flashcards": [
                {
                  "question": "Question text based on the note",
                  "answer": "Answer explaining the answer clearly"
                }
              ]
            }
            
            Captured Text:
            $rawText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You are an advanced Document Understanding Pipeline. Extract text, summarize, style markdown, identify action items and flashcards, and return ONLY a valid JSON object.")))
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val adapter = moshi.adapter(StructuredDocResult::class.java)
                adapter.fromJson(jsonText) ?: runLocalFallbackAnalysis(rawText)
            } else {
                runLocalFallbackAnalysis(rawText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runLocalFallbackAnalysis(rawText)
        }
    }

    override suspend fun askBrain(
        messages: List<ChatMessage>,
        contextDocs: List<CapturedDocument>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "I am running in offline mode because the Gemini API key is not configured in the Secrets panel. To enable intelligent brain chat, please add your GEMINI_API_KEY."
        }

        // Build context from documents
        val contextText = if (contextDocs.isNotEmpty()) {
            "Use the following notes and documents from the user's personal knowledge base to answer their questions. Always cite the note title you used in brackets, e.g. [Distributed Chat Architecture].\n\n" +
                    contextDocs.joinToString("\n\n---\n\n") { doc ->
                        "Note: ${doc.title}\nCategory: ${doc.category}\nContent:\n${doc.rawText}"
                    }
        } else {
            "The user has no notes or documents in their knowledge base yet. Advise them to scan some documents or create notes first."
        }

        // Map conversational history
        val contents = mutableListOf<Content>()
        messages.takeLast(10).forEach { msg ->
            contents.add(
                Content(
                    parts = listOf(Part(text = msg.content))
                )
            )
        }

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = """
                            You are the intelligent personal "Second Brain" AI assistant of Project Athena.
                            Your job is to help the user query, synthesize, and reason over their captured notes, whiteboards, and presentation slides.
                            
                            $contextText
                            
                            Be concise, professional, helpful, and insightful. Highlight connections between different notes. Always output clean markdown.
                        """.trimIndent()
                    )
                )
            )
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I apologize, but I couldn't generate a response."
        } catch (e: Exception) {
            e.printStackTrace()
            "Error querying brain chat: ${e.localizedMessage ?: "Connection failed."}"
        }
    }

    // High fidelity offline fallback parsing
    private fun runLocalFallbackAnalysis(rawText: String): StructuredDocResult {
        // Simple heuristic extraction
        val lines = rawText.lines()
        val titleLine = lines.firstOrNull { it.isNotBlank() } ?: "Unnamed Note"
        val title = titleLine.replace(Regex("[#*:\\-\\[\\]]"), "").trim()

        val summary = "Offline analysis of '$title'. Captured ${lines.size} lines of text. (Configure your GEMINI_API_KEY in the Secrets panel for fully structured AI-powered summaries, action items, and flashcards)."

        // Build markdown representation
        val markdown = StringBuilder()
        markdown.append("# ").append(title).append("\n\n")
        var inList = false
        lines.drop(1).forEach { line ->
            if (line.isNotBlank()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                    markdown.append(trimmed).append("\n")
                    inList = true
                } else if (trimmed.firstOrNull()?.isDigit() == true && trimmed.contains(".")) {
                    markdown.append(trimmed).append("\n")
                    inList = true
                } else {
                    if (inList) {
                        markdown.append("\n")
                        inList = false
                    }
                    markdown.append(trimmed).append("\n\n")
                }
            }
        }

        // Heuristically extract tags
        val tags = mutableListOf<String>()
        if (rawText.contains("whiteboard", ignoreCase = true)) tags.add("Whiteboard")
        if (rawText.contains("coroutines", ignoreCase = true) || rawText.contains("kotlin", ignoreCase = true)) tags.add("Kotlin")
        if (rawText.contains("distributed", ignoreCase = true) || rawText.contains("architecture", ignoreCase = true)) tags.add("Architecture")
        if (rawText.contains("meeting", ignoreCase = true)) tags.add("Meeting")
        if (rawText.contains("generative", ignoreCase = true) || rawText.contains("ai", ignoreCase = true)) tags.add("AI Strategy")
        if (tags.isEmpty()) tags.add("Personal")

        // Heuristically extract action items
        val actionItems = mutableListOf<String>()
        lines.forEach { line ->
            if (line.contains("[ ]") || line.contains("TODO", ignoreCase = true) || line.contains("action item", ignoreCase = true)) {
                val clean = line.replace("[ ]", "").replace("TODO:", "").replace("-", "").trim()
                if (clean.isNotBlank()) actionItems.add(clean)
            }
        }
        if (actionItems.isEmpty()) {
            actionItems.add("Review offline capture of '$title'")
        }

        // Heuristically generate 1-2 flashcards
        val flashcards = listOf(
            OcrFlashcard(
                question = "What is the main topic of '$title'?",
                answer = "The main topic covers details about: " + lines.take(5).joinToString(" ").take(100) + "..."
            ),
            OcrFlashcard(
                question = "When was this note captured?",
                answer = "This note was saved locally using the offline-first fallback engine."
            )
        )

        return StructuredDocResult(
            title = title,
            summary = summary,
            markdown = markdown.toString(),
            tags = tags,
            actionItems = actionItems,
            flashcards = flashcards
        )
    }
}
