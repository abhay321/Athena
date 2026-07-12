package com.example.data

import android.graphics.Rect
import kotlinx.coroutines.delay

class PaddleOcrEngine : OcrService {

    override val engineType: OcrEngineType = OcrEngineType.PADDLE_OCR

    private var isNativeInferenceEngineLoaded = false

    override suspend fun initialize(): Boolean {
        // Simulates loading PaddleOCR lightweight native C++ JNI libraries and .pdmodel weights
        delay(600)
        isNativeInferenceEngineLoaded = true
        return true
    }

    override suspend fun release() {
        // Unloads JNI model weights from C++ heap
        isNativeInferenceEngineLoaded = false
    }

    override suspend fun isAvailable(): Boolean = true

    override suspend fun extractText(imageIdentifier: String): String {
        return processImage(imageIdentifier).rawText
    }

    override suspend fun processImage(imageIdentifier: String, options: OcrOptions): OcrResult {
        if (!isNativeInferenceEngineLoaded) {
            initialize()
        }

        val template = OcrSimulator.templates.firstOrNull { it.id == imageIdentifier || it.title == imageIdentifier }
            ?: return OcrResult(
                rawText = "No text captured.",
                confidence = 0f,
                durationMs = 50,
                engineUsed = OcrEngineType.PADDLE_OCR,
                isSuccess = false,
                errorMessage = "PaddleOCR failed to locate model or file for '$imageIdentifier'"
            )

        // PaddleOCR has excellent layout analysis. It decomposes text line-by-line
        val lines = template.text.split("\n").filter { it.isNotBlank() }
        val blocks = lines.mapIndexed { index, lineText ->
            OcrBlock(
                text = lineText.trim(),
                confidence = options.confidenceThreshold + 0.51f,
                boundingBox = Rect(20, index * 40, 620, (index + 1) * 40),
                level = OcrElementLevel.LINE
            )
        }

        // Simulates deep learning multi-stage inference latencies (approx. 450ms)
        delay(450)

        return OcrResult(
            rawText = template.text,
            confidence = 0.97f,
            durationMs = 450,
            engineUsed = OcrEngineType.PADDLE_OCR,
            blocks = blocks,
            isSuccess = true
        )
    }
}
