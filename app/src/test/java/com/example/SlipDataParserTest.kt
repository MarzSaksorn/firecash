package com.example

import com.example.data.ocr.SlipDataParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlipDataParserTest {

    @Test
    fun testPromptPaySlipParsing() {
        val samplePromptPaySlip = """
            โอนเงินสำเร็จ
            PromptPay Transfer
            KASIKORNBANK 004
            Ref: TXN-89231401
            9104A8F4
            จำนวนเงิน: ฿450.00
            2023-10-24 14:30:15
        """.trimIndent()

        val result = SlipDataParser.parse(samplePromptPaySlip)

        assertTrue(result.isBankSlip)
        assertEquals("THB", result.currency)
        assertEquals(450.00, result.amount, 0.01)
        assertNotNull(result.bankPayload)
        assertEquals("A8F4", result.bankPayload?.crc)
        assertEquals("004", result.bankPayload?.sendingBank)
    }

    @Test
    fun testStandardReceiptParsing() {
        val sampleReceipt = """
            STARBUCKS COFFEE
            Store #1492
            Date: 2023-10-24
            Time: 08:30 AM
            1x Caramel Macchiato   $6.50
            1x Butter Croissant     $4.75
            TOTAL: $11.25
            Thank you for visiting!
        """.trimIndent()

        val result = SlipDataParser.parse(sampleReceipt)

        assertEquals("STARBUCKS COFFEE", result.merchant)
        assertEquals("USD", result.currency)
        assertEquals(11.25, result.amount, 0.01)
        assertEquals("Food & Dining", result.suggestedCategory)
    }

    @Test
    fun testEMVCoQrCrcExtraction() {
        val emvCoSlip = """
            Transfer Confirmation
            00020101021229370016A000000677010111011300668123456785802TH5303764540550.00630489AB
            Amount: 50.00 THB
        """.trimIndent()

        val payload = SlipDataParser.extractBankSlipPayload(emvCoSlip, 50.00)
        assertNotNull(payload)
        assertEquals("89AB", payload?.crc)
    }
}
