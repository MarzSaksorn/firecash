package com.example

import com.example.data.ocr.SlipDataParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun testParseNormalizesEnglishMonthDate() {
        val slip = """
            TrueMoney
            B 49.79
            Total amount
            Wallet
            31 Aug 2026 21:12:23
        """.trimIndent()

        val result = SlipDataParser.parse(slip)
        assertEquals("2026-08-31", result.date)
        assertEquals(49.79, result.amount, 0.01)
    }

    @Test
    fun testParseThaiMonthDateConvertsBuddhistEra() {
        val slip = """
            KASIKORNBANK
            31 ส.ค. 2569 21:11
            ยอดรวม: ฿60.00
        """.trimIndent()

        val result = SlipDataParser.parse(slip)
        assertEquals("2026-08-31", result.date)
    }

    @Test
    fun testParseAllThaiMonthAbbreviations() {
        val expected = mapOf(
            "ม.ค." to "2026-01-15", "ก.พ." to "2026-02-15", "มี.ค." to "2026-03-15",
            "เม.ย." to "2026-04-15", "พ.ค." to "2026-05-15", "มิ.ย." to "2026-06-15",
            "ก.ค." to "2026-07-15", "ส.ค." to "2026-08-15", "ก.ย." to "2026-09-15",
            "ต.ค." to "2026-10-15", "พ.ย." to "2026-11-15", "ธ.ค." to "2026-12-15"
        )
        expected.forEach { (abbr, iso) ->
            val result = SlipDataParser.parse("15 $abbr 2569")
            assertEquals("$abbr should map to $iso", iso, result.date)
        }
    }

    @Test
    fun testExtractQrAmount() {
        assertEquals(50.0, SlipDataParser.extractQrAmount("000201010211540550.005802TH6304ABCD")!!, 0.01)
        assertEquals(1250.0, SlipDataParser.extractQrAmount("00020101021254071250.005802TH6304ABCD")!!, 0.01)
        assertNull(SlipDataParser.extractQrAmount("0041000600000101030040220046"))
        assertNull(SlipDataParser.extractQrAmount(""))
    }
}
