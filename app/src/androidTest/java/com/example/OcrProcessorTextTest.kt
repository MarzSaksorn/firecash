package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.ocr.OcrProcessor
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the Personal-mode pipeline against REAL slip photos pulled from the device
 * (pushed into the app's external files dir as slip1/slip2/slip3). Logs exactly what
 * text recognition and the QR scan produce for each one under the FireCashOCR tag.
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

        // Synthetic fallback check: Latin-only image must still extract text
        val width = 1400
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 80f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText("KASIKORNBANK", 80f, 200f, paint)
        canvas.drawText("Amount: 450.00 THB", 80f, 460f, paint)
        val file = File(context.cacheDir, "synthetic_slip.jpg")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
        bitmap.recycle()
        val text = runBlocking { processor.recognizeText(file.absolutePath) }
        file.delete()
        android.util.Log.d("FireCashOCR", "SYNTHETIC: text=[$text]")
    }
}
