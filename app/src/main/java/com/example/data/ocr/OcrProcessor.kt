package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrProcessor(private val context: Context? = null) {

    suspend fun processReceipt(
        imageUri: String?,
        samplePreset: SampleSlipPreset? = null,
        scanCenterOnly: Boolean = true
    ): ParsedReceiptResult = withContext(Dispatchers.Default) {
        delay(600)

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
                val bitmap = BitmapFactory.decodeFile(imageUri)
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
