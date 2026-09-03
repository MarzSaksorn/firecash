package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.ocr.OcrProcessor
import com.example.data.ocr.SlipDocumentDetector
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the document-detection + perspective-flatten pipeline against a
 * perspective-skewed real slip photo pushed to the app files dir as slip4.jpg.
 * Logs under the FireCashOCR tag so results can be read via logcat.
 */
@RunWith(AndroidJUnit4::class)
class OcrProcessorTextTest {

    @Test
    fun textRecognitionExtractsSlipText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val processor = OcrProcessor(context)

        val filesDir = context.getExternalFilesDir(null) ?: context.filesDir
        val realSlips = listOf("slip1.jpeg", "slip2.png", "slip3.jpeg")
            .map { File(filesDir, it) }
            .filter { it.exists() }

        realSlips.forEach { file ->
            val text = runBlocking { processor.recognizeText(file.absolutePath, scanCenterOnly = false) }
            val qr = runBlocking { processor.processReceipt(file.absolutePath, scanCenterOnly = false).rawText }
            android.util.Log.d(
                "FireCashOCR",
                "SLIP ${file.name}: text=[${text.replace("\n", " | ")}] qr=[${qr.take(120)}]"
            )
        }

        // Flatten test: skewed slip (slip4.jpg) — raw OCR vs OCR after perspective flatten
        val skewed = File(filesDir, "slip4.jpg")
        if (skewed.exists()) {
            val rawText = runBlocking { processor.recognizeText(skewed.absolutePath, scanCenterOnly = false) }
            val flatPath = runBlocking { processor.flattenedCopy(skewed.absolutePath) }
            val flatText = if (flatPath != null) {
                runBlocking { processor.recognizeText(flatPath, scanCenterOnly = false) }
            } else ""
            android.util.Log.d(
                "FireCashOCR",
                "FLATTEN slip4: raw=[${rawText.replace("\n", " | ")}] flatPath=$flatPath flat=[${flatText.replace("\n", " | ")}]"
            )
        }
    }

    @Test
    fun documentDetectorFindsSlipOnSkewedPhoto() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val filesDir = context.getExternalFilesDir(null) ?: context.filesDir
        val skewed = File(filesDir, "slip4.jpg")
        if (!skewed.exists()) return

        val bitmap = BitmapFactory.decodeFile(skewed.absolutePath)
        val quad = SlipDocumentDetector.detect(bitmap)
        val flat = SlipDocumentDetector.flatten(bitmap)
        android.util.Log.d(
            "FireCashOCR",
            "DETECT slip4: quad=${quad?.points?.joinToString { "(${it.x.toInt()},${it.y.toInt()})" }} flat=${flat?.width}x${flat?.height}"
        )
        bitmap.recycle()
        flat?.recycle()
    }
}
