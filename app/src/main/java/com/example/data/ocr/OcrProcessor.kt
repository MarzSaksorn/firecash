package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrProcessor(private val context: Context? = null) {

    /**
     * Runs ML Kit text recognition over the photo and returns the raw recognized text.
     * Used by Personal mode, which logs the slip from the photo text instead of calling a
     * verification API. The on-device recognizer covers the Latin script (digits, dates,
     * English bank/merchant names); Thai-only glyphs are not supported by ML Kit on-device.
     */
    suspend fun recognizeText(
        imageUri: String?,
        scanCenterOnly: Boolean = false
    ): String = withContext(Dispatchers.Default) {
        if (imageUri.isNullOrEmpty()) return@withContext ""
        try {
            val bitmap = decodeRotated(imageUri) ?: return@withContext ""
            val image = if (scanCenterOnly) InputImage.fromBitmap(cropToCenter(bitmap), 0)
            else InputImage.fromBitmap(bitmap, 0)

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                val text = runRecognition(recognizer, image)
                android.util.Log.d("FireCashOCR", "recognizeText(file=${imageUri.takeLast(30)}, len=${text.length})")
                text
            } catch (e: Exception) {
                android.util.Log.w("FireCashOCR", "text recognition failed: ${e.message}")
                ""
            } finally {
                recognizer.close()
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * If a flat slip/document is detectable in the photo, writes a perspective-flattened copy
     * to the app's Pictures directory and returns its path — the caller should store THAT as
     * the slip's photo (the slip photo is the crop, not the full frame) and run OCR + QR
     * decoding on it. Returns null when no slip region is found (caller keeps the original).
     *
     * The output file name is deterministic (derived from the source file name), so
     * re-processing the same photo (e.g. a tracked-folder Force Sync) overwrites the same
     * crop file instead of leaving duplicate copies behind.
     */
    suspend fun flattenedCopy(imageUri: String, outName: String? = null): String? = withContext(Dispatchers.Default) {
        if (imageUri.isBlank()) return@withContext null
        runCatching {
            val bitmap = decodeRotated(imageUri) ?: return@withContext null
            val flat = SlipDocumentDetector.flatten(bitmap)
            if (flat == null) {
                // The slip region wasn't found as a clean 4-corner polygon, but the photo
                // still needs to be easier for OCR than the full frame.  Crop to the center
                // 60 % square — the camera guide box already puts the slip there, and this
                // removes most background noise that would confuse text recognition.
                android.util.Log.d("FireCashOCR", "no quad — center-cropping as fallback")
                val cropped = cropToCenter(bitmap)
                bitmap.recycle()
                if (cropped == null) return@withContext null
                val ctx = context ?: return@withContext null
                val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: ctx.filesDir
                if (!dir.exists()) dir.mkdirs()
                val base = outName?.takeIf { it.isNotBlank() }
                    ?: runCatching { File(imageUri).nameWithoutExtension }
                        .getOrDefault("slip")
                        .replace(Regex("[^A-Za-z0-9._-]"), "_")
                val out = File(dir, "${base}_flat.jpg")
                android.util.Log.d("FireCashOCR", "center-crop fallback ${cropped.width}x${cropped.height} -> ${out.name}")
                FileOutputStream(out).use { cropped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                cropped.recycle()
                return@withContext out.absolutePath
            }
            bitmap.recycle()
            val ctx = context ?: return@withContext null
            val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: ctx.filesDir
            if (!dir.exists()) dir.mkdirs()
            val base = outName?.takeIf { it.isNotBlank() }
                ?: runCatching { File(imageUri).nameWithoutExtension }
                    .getOrDefault("slip")
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val out = File(dir, "${base}_flat.jpg")
            android.util.Log.d("FireCashOCR", "flattened slip ${flat.width}x${flat.height} -> ${out.absolutePath}")
            FileOutputStream(out).use { flat.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            flat.recycle()
            out.absolutePath
        }.getOrNull()
    }

    private suspend fun runRecognition(recognizer: TextRecognizer, image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    suspend fun processReceipt(
        imageUri: String?,
        samplePreset: SampleSlipPreset? = null,
        scanCenterOnly: Boolean = true
    ): ParsedReceiptResult = withContext(Dispatchers.Default) {
        if (samplePreset != null) {
            return@withContext SlipDataParser.parse(samplePreset.rawOcr)
        }

        if (!imageUri.isNullOrEmpty()) {
            try {
                val image = InputImage.fromFilePath(
                    context ?: throw IllegalStateException("Context required"),
                    android.net.Uri.fromFile(java.io.File(imageUri))
                )

                // Crop the image to the QR frame area (matching the on‑screen overlay)
                val bitmap = decodeRotated(imageUri)
                val scanImage = when {
                    bitmap == null -> null
                    scanCenterOnly -> InputImage.fromBitmap(cropToCenter(bitmap), 0)
                    else -> InputImage.fromBitmap(bitmap, 0)
                }

                // First try QR code detection (Barcode scanning)
                val barcodeScanner = BarcodeScanning.getClient()
                val qrResult = if (scanImage != null) {
                    suspendCancellableCoroutine<String> { cont ->
                        barcodeScanner.process(scanImage)
                            .addOnSuccessListener { barcodes ->
                                if (barcodes.isNotEmpty() && barcodes[0].rawValue != null) {
                                    cont.resume(barcodes[0].rawValue!!)
                                } else {
                                    // No QR found, fall back to OCR
                                    cont.resume("")
                                }
                            }
                            .addOnFailureListener { e -> cont.resumeWithException(e) }
                    }
                } else {
                    ""
                }
                if (qrResult.isNotEmpty()) {
                    return@withContext ParsedReceiptResult(
    merchant = "QR Payload",
    amount = 0.0,
    amountString = "0",
    currency = "",
    date = "",
    time = "",
    rawText = qrResult,
    suggestedCategory = "QR",
    suggestedTags = emptyList(),
    bankPayload = null,
    isBankSlip = false
)
                }

                return@withContext ParsedReceiptResult(
                    merchant = "No QR Code Found",
                    amount = 0.0,
                    amountString = "0",
                    currency = "",
                    date = "",
                    time = "",
                    rawText = "",
                    suggestedCategory = "",
                    suggestedTags = emptyList(),
                    bankPayload = null,
                    isBankSlip = false
                )
            } catch (e: Exception) {
                // Fallback to default if OCR fails
            }
        }

        ParsedReceiptResult(
            merchant = "No QR Code Found",
            amount = 0.0,
            amountString = "0",
            currency = "",
            date = "",
            time = "",
            rawText = "",
            suggestedCategory = "",
            suggestedTags = emptyList(),
            bankPayload = null,
            isBankSlip = false
        )
    }

    private fun cropToCenter(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val side = minOf(w, h).toFloat() * 0.6f
        val left = ((w - side) / 2f).toInt()
        val top = ((h - side) / 2f).toInt()
        return Bitmap.createBitmap(bitmap, left, top, side.toInt(), side.toInt())
    }

    /**
     * Decodes [imageUri] with its EXIF rotation applied. JPEGs from the camera (and most phone
     * galleries) store orientation in EXIF metadata; [BitmapFactory.decodeFile] ignores it, so
     * without this every portrait capture would be handed to OpenCV/ML Kit sideways — and
     * document detection + OCR would fail or mis-flatten depending on how the phone was held.
     */
    private fun decodeRotated(imageUri: String): Bitmap? {
        val raw = BitmapFactory.decodeFile(imageUri) ?: return null
        val degrees = runCatching {
            val exif = android.media.ExifInterface(imageUri)
            when (exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }.getOrDefault(0)
        if (degrees == 0) return raw
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (rotated != raw) raw.recycle()
        return rotated
    }
}

enum class SampleSlipPreset(
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val rawOcr: String
) {
    UBER_RIDE(
        title = "Uber Ride (Receipt)",
        subtitle = "Travel • $45.20 • Visa",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBZZ9ONbAjx36oHwYsqN7KaRdqc7WoyKEXpoGOkAb5rIWKhU0VD30ZTajdQF31M_AJEAXtiNYvjJJ1pvVoRzONZMqmegkOwEtIGMu6BUlftOMm0cMPB50Qoyv9b4EPxFF9Bsm3hkzt9IH4LtmOJzsOp0RjBbgLVGRDT7HCU7MAJmI_WrueqrIxqWvd4_71lrkV90A-vVKIZMjhIZcpt0Klc2D9VY0RkRAc9UxDuPx-9O4uh8Kaj6HMFrw",
        rawOcr = """
            Uber Technologies Inc.
            Trip Receipt
            Date: 2023-10-24 08:42 AM
            Total Amount: $45.20
            Payment: Visa **** 8821
        """.trimIndent()
    ),
    KBANK_TRANSFER(
        title = "Kasikornbank (PromptPay Slip)",
        subtitle = "Transfer • ฿1,250.00 • CRC: 910488F2",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCM6lTYNqsa0-EHu-FFKmSk_kH8_bkt83PGtRN27Z_UwztDsWqdGicAJeTrsKOhhbsbdrlEK8zF6y2pJR2R5RhMhIfpq5HfgCQsiPHirPnjj-rF0A6DJWHj7uXiLEvjLeWdfbBSmYxZt2odp1X6JYWHP5EFuge6h8Mxn6Oo6s66z3eWrfeNXJ0KtUvYpjf-TU4ife6e7i6bs1agtHI_rPrkS88RgtGbZ_d14M8GocYQ0CqgkT62zDH9w",
        rawOcr = """
            ธนาคารกสิกรไทย KASIKORNBANK (004)
            โอนเงินสำเร็จ PromptPay
            วันที่: 2023-10-24 11:30:15
            จาก: นาย สมชาย ย. (xxx-x-x1123-x)
            ถึง: บจก. อาร์ทิซาน โรสเตอร์
            ยอดรวม: ฿1,250.00
            Tag 91 CRC: 910488F2
            Ref: 2023102488771234
        """.trimIndent()
    ),
    SCB_SLIP_DUPLICATE(
        title = "SCB Slip (Duplicate Test)",
        subtitle = "Transfer • ฿850.00 • CRC: 91049999",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAstveoykS_j7n5Zio6dxX4QQOWsw3i46sTCyBptuEKbSFMByGgj02VEEgMOnP42dWochUKjsLiRBetIxN72xWOjLa4sLtM6fZPcPAatOFTh0TTHmmxW66nCUp2glMbAt_O3-qf2qxcEBumU6p2Wp_V6HmvMBAfWZHdq5ZT2Ng_FR9t3vv6utxXlhEjMkmFGfTng34vgoyDETu10DUJ6-tP58zH54PMkQ-B1KjEW2dBYaRHnWu_nB6p-g",
        rawOcr = """
            ธนาคารไทยพาณิชย์ SCB (014)
            โอนเงินสำเร็จ
            วันที่: 2023-10-23 15:45:00
            ยอดรวม: ฿850.00
            Tag 91 CRC: 91049999
            Ref: 2023102399990001
        """.trimIndent()
    ),
    STARBUCKS_RECEIPT(
        title = "Starbucks Coffee (Paper Receipt)",
        subtitle = "Food & Dining • $14.50 • Cash",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBRg5UMxh0fNnHsk40bUW8ffJWpm0hSUNdXsZWxXTHXwNEyJ8VisSPWBai7mLzUu6nDrQ62goyUAZNZn7L5W7D0tTRLv7Sg-L58CZwTBtqWVsLFQlH4wau347uh4vJeFUoKjkkJq7SWGZDfsMo1DaCzhcUo0MhkBiQKCDn6T6JAR7DOGPUai_KH64f_fJO9f9ZjdIttcpRFxQZPfB6TJvylLnVlS-VXOlNzQ006OTGaRcTKRDsDUwLow",
        rawOcr = """
            STARBUCKS COFFEE #1042
            1 CARAMEL MACCHIATO $6.50
            1 CROISSANT $4.50
            1 EXTRA SHOT $3.50
            TOTAL AMOUNT: $14.50
            DATE: 2023-10-24 09:12 AM
            THANK YOU FOR VISITING
        """.trimIndent()
    )
}
