package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * CameraX ImageAnalysis.Analyzer implementation that uses Google ML Kit Text Recognition
 * to scan and extract Panic Full log patterns in real-time.
 */
class PanicLogImageAnalyzer(
    private val onScanResult: (OcrScanResult) -> Unit,
    private val onError: (Exception) -> Unit = {}
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var isProcessing = false
    private var lastAnalyzedTimestamp = 0L
    private val throttleIntervalMs = 600L // Analyze frame every 600ms to balance responsiveness and battery/CPU

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (isProcessing || (currentTimestamp - lastAnalyzedTimestamp < throttleIntervalMs)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true
        lastAnalyzedTimestamp = currentTimestamp

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val scanResult = OcrLogExtractor.processScannedText(visionText.text)
                onScanResult(scanResult)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    companion object {
        /**
         * Processes an on-device Bitmap (e.g. from camera snapshot or image picker) with ML Kit.
         */
        suspend fun processBitmap(bitmap: Bitmap): OcrScanResult = withContext(Dispatchers.Default) {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            
            return@withContext suspendProcessing(recognizer, image)
        }

        /**
         * Processes an Image Uri (e.g. from photo gallery) with ML Kit.
         */
        suspend fun processUri(context: Context, uri: Uri): OcrScanResult = withContext(Dispatchers.IO) {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: throw IllegalArgumentException("No se pudo decodificar la imagen")
            processBitmap(bitmap)
        }

        private suspend fun suspendProcessing(
            recognizer: com.google.mlkit.vision.text.TextRecognizer,
            image: InputImage
        ): OcrScanResult = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val result = OcrLogExtractor.processScannedText(visionText.text)
                    continuation.resume(result) {}
                }
                .addOnFailureListener { ex ->
                    continuation.resume(
                        OcrLogExtractor.processScannedText("")
                    ) {}
                }
        }
    }
}
