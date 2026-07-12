package com.example.data

import android.graphics.Rect

/**
 * Supported OCR Engine Types for Project Athena's swappable architecture.
 */
enum class OcrEngineType(val displayName: String, val description: String) {
    GOOGLE_ML_KIT(
        displayName = "Google ML Kit",
        description = "On-device Google Play Services OCR. Fastest, highly optimized for Latin scripts."
    ),
    PADDLE_OCR(
        displayName = "PaddleOCR",
        description = "Deep learning-based OCR. Superior layout analysis and multilingual/Chinese parsing."
    ),
    TESSERACT(
        displayName = "Tesseract OCR",
        description = "Classic open-source LSTM OCR engine. Highly customizable with localized traineddata files."
    ),
    SIMULATOR(
        displayName = "Athena Simulator",
        description = "High-fidelity mock engine matching actual scan templates to ground-truth text."
    )
}

/**
 * Parameters to configure the active OCR engine's recognition pipeline.
 */
data class OcrOptions(
    val languageCode: String = "en",
    val detectLanguage: Boolean = true,
    val pageSegmentationMode: Int = 3, // Tesseract PSM option
    val enableLayoutAnalysis: Boolean = true,
    val confidenceThreshold: Float = 0.45f
)

/**
 * Level of structural hierarchy for a recognized text element.
 */
enum class OcrElementLevel {
    BLOCK, LINE, WORD
}

/**
 * A segment of recognized text with semantic metadata, confidence, and visual positioning.
 */
data class OcrBlock(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect?,
    val level: OcrElementLevel
)

/**
 * Standardized OCR result container returned by all swappable engines.
 */
data class OcrResult(
    val rawText: String,
    val confidence: Float,
    val durationMs: Long,
    val engineUsed: OcrEngineType,
    val blocks: List<OcrBlock> = emptyList(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Abstract interface for a single OCR engine implementation.
 * Ensures strict decoupling of low-level native libraries from presentation layers.
 */
interface OcrEngine {
    val engineType: OcrEngineType

    /**
     * Initializes any heavy native resources, shared libraries, or model weights.
     */
    suspend fun initialize(): Boolean

    /**
     * Releases system native memory allocations.
     */
    suspend fun release()

    /**
     * Assesses whether the model weights, services, or architecture packages are active on this device.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Analyzes the target image and extracts the raw text and spatial layout structures.
     */
    suspend fun processImage(imageIdentifier: String, options: OcrOptions = OcrOptions()): OcrResult
}

/**
 * Main OCR domain service.
 * Adapts individual swappable OcrEngines into the application's ingestion workflows.
 */
interface OcrService : OcrEngine {
    suspend fun extractText(imageIdentifier: String): String
}
