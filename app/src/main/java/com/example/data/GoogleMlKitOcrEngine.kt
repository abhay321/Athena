package com.example.data

import android.graphics.Rect
import kotlinx.coroutines.delay

class GoogleMlKitOcrEngine : OcrService {

    override val engineType: OcrEngineType = OcrEngineType.GOOGLE_ML_KIT

    private var isModelDownloaded = false

    override suspend fun initialize(): Boolean {
        // Simulates ML Kit checking and downloading Google Play Services model packages
        delay(300)
        isModelDownloaded = true
        return true
    }

    override suspend fun release() {
        // Releases ML Kit text recognizer instances
    }

    override suspend fun isAvailable(): Boolean = true

    override suspend fun extractText(imageIdentifier: String): String {
        return processImage(imageIdentifier).rawText
    }

    override suspend fun processImage(imageIdentifier: String, options: OcrOptions): OcrResult {
        if (!isModelDownloaded) {
            initialize()
        }

        val template = OcrSimulator.templates.firstOrNull { it.id == imageIdentifier || it.title == imageIdentifier }
            ?: return OcrResult(
                rawText = "No text captured.",
                confidence = 0f,
                durationMs = 30,
                engineUsed = OcrEngineType.GOOGLE_ML_KIT,
                isSuccess = false,
                errorMessage = "Google ML Kit failed to locate file data for '$imageIdentifier'"
            )

        // ML Kit splits text into logical blocks (paragraphs)
        val paragraphs = template.text.split("\n\n").filter { it.isNotBlank() }
        val blocks = paragraphs.mapIndexed { index, paragraphText ->
            OcrBlock(
                text = paragraphText.trim().replace("\n", " "),
                confidence = options.confidenceThreshold + 0.48f,
                boundingBox = Rect(15, index * 120, 580, (index + 1) * 120),
                level = OcrElementLevel.BLOCK
            )
        }

        // Simulates typical ML Kit execution times (200ms - 250ms)
        delay(220)

        return OcrResult(
            rawText = template.text,
            confidence = 0.95f,
            durationMs = 220,
            engineUsed = OcrEngineType.GOOGLE_ML_KIT,
            blocks = blocks,
            isSuccess = true
        )
    }
}
