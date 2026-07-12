package com.example.data

import android.graphics.Rect
import kotlinx.coroutines.delay

class TesseractOcrEngine : OcrService {

    override val engineType: OcrEngineType = OcrEngineType.TESSERACT

    private var isTessDataLoaded = false

    override suspend fun initialize(): Boolean {
        // Simulates copying tessdata files (.traineddata) from assets to external storage
        delay(400)
        isTessDataLoaded = true
        return true
    }

    override suspend fun release() {
        // Disposes native TessBaseAPI instances
        isTessDataLoaded = false
    }

    override suspend fun isAvailable(): Boolean = true

    override suspend fun extractText(imageIdentifier: String): String {
        return processImage(imageIdentifier).rawText
    }

    override suspend fun processImage(imageIdentifier: String, options: OcrOptions): OcrResult {
        if (!isTessDataLoaded) {
            initialize()
        }

        val template = OcrSimulator.templates.firstOrNull { it.id == imageIdentifier || it.title == imageIdentifier }
            ?: return OcrResult(
                rawText = "No text captured.",
                confidence = 0f,
                durationMs = 45,
                engineUsed = OcrEngineType.TESSERACT,
                isSuccess = false,
                errorMessage = "Tesseract failed to load tessdata trained model for language '${options.languageCode}'"
            )

        // Tesseract scans character-by-character and groups into granular words or lines
        // Let's split into words for fine-grained illustration
        val words = template.text.split(Regex("\\s+")).filter { it.isNotBlank() }.take(30)
        val blocks = words.mapIndexed { index, word ->
            OcrBlock(
                text = word,
                confidence = options.confidenceThreshold + 0.38f,
                boundingBox = Rect((index % 5) * 100, (index / 5) * 50, (index % 5 + 1) * 100, (index / 5 + 1) * 50),
                level = OcrElementLevel.WORD
            )
        }

        // Tesseract takes slightly longer on older CPUs (600ms - 800ms)
        delay(680)

        // Inject tiny simulated character misrecognitions if confidence threshold is extremely high
        val outputText = if (options.confidenceThreshold > 0.8f) {
            template.text.replace("WebSocket", "WebS0cket").replace("Kotlin", "Kot1in")
        } else {
            template.text
        }

        return OcrResult(
            rawText = outputText,
            confidence = 0.88f,
            durationMs = 680,
            engineUsed = OcrEngineType.TESSERACT,
            blocks = blocks,
            isSuccess = true
        )
    }
}
