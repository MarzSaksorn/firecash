package com.example

import com.example.data.ocr.SlipDataParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ── Edge case batch ──────────────────────────────────────────────────────

    @Test
    fun testParseEmptyTextReturnsDefaultAmountAndTodayDate() {
        val result = SlipDataParser.parse("")
        assertEquals("Merchant", result.merchant)
        assertEquals(45.20, result.amount, 0.01)
        assertNotNull(result.date)
        assertFalse(result.isBankSlip)
    }

    @Test
    fun testParseWhitespaceOnlyReturnsDefaults() {
        val result = SlipDataParser.parse("   \n  \n  ")
        assertEquals("Merchant", result.merchant)
        assertEquals(45.20, result.amount, 0.01)
    }

    @Test
    fun testAmountWithThousandSeparatorCommas() {
        val slip = """
            TOTAL: ฿1,234,567.89
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals(1234567.89, result.amount, 0.01)
    }

    @Test
    fun testAmountSingleDecimalPlaceParsedAsWholeNumber() {
        val slip = """
            TOTAL: ฿45.5
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals(45.0, result.amount, 0.01)
    }

    @Test
    fun testAmountWithTwoDecimalsViaGenericPattern() {
        val slip = """
            50.75
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals(50.75, result.amount, 0.01)
    }

    @Test
    fun testZeroAmount() {
        val slip = """
            TOTAL: ฿0.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals(0.00, result.amount, 0.01)
    }

    @Test
    fun testNegativeAmountHyphenBreaksCurrencyMatch() {
        val slip = """
            TOTAL: ฿-50.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals(50.00, result.amount, 0.01)
    }

    @Test
    fun testDateSlashSeparatedNormalized() {
        val slip = """
            Date: 24/10/2023
            Amount: 100.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("2023-10-24", result.date)
    }

    @Test
    fun testDateDotSeparatedNormalized() {
        val slip = """
            Date: 24.10.2023
            Amount: 100.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("2023-10-24", result.date)
    }

    @Test
    fun testDateDashSeparatedNormalized() {
        val slip = """
            Date: 24-10-2023
            Amount: 100.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("2023-10-24", result.date)
    }

    @Test
    fun testParseDateNoDateInTextReturnsToday() {
        val result = SlipDataParser.parse("Just some random text with no date pattern")
        assertNotNull(result.date)
        assertEquals(10, result.date.length)
    }

    @Test
    fun testThaiDateSingleDigitDay() {
        val slip = """
            SCB
            1 ส.ค. 2569
            ยอดรวม ฿120.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("2026-08-01", result.date)
    }

    @Test
    fun testThaiDateNonBuddhistYearDoesNotShift() {
        val slip = """
            DATE: 15 ม.ค. 2026
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("2026-01-15", result.date)
    }

    @Test
    fun testExtractSlipDateReturnsNullForNoDate() {
        assertNull(SlipDataParser.extractSlipDate("Just random noise without a date"))
        assertNull(SlipDataParser.extractSlipDate(""))
        assertNull(SlipDataParser.extractSlipDate("ABCDEFG 12345 @#\$%^&"))
    }

    @Test
    fun testExtractSlipDateThaiOnlyText() {
        val date = SlipDataParser.extractSlipDate("31 ส.ค. 2569")
        assertEquals("2026-08-31", date)
    }

    @Test
    fun testCrcLowercaseHexUppercased() {
        val slip = """
            KASIKORNBANK
            9104abcd
            จำนวนเงิน: ฿200.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertNotNull(result.bankPayload)
        assertEquals("ABCD", result.bankPayload?.crc)
    }

    @Test
    fun testBankDetectionKbankByCode() {
        val slip = """
            004
            9104A8F4
            จำนวนเงิน: ฿100.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("004", result.bankPayload?.sendingBank)
    }

    @Test
    fun testBankDetectionSCB() {
        val slip = """
            SCB
            9104A8F4
            จำนวนเงิน: ฿300.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("014", result.bankPayload?.sendingBank)
    }

    @Test
    fun testBankDetectionBangkokBank() {
        val slip = """
            Bangkok Bank
            9104A8F4
            จำนวนเงิน: ฿500.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("002", result.bankPayload?.sendingBank)
    }

    @Test
    fun testBankDetectionKTB() {
        val slip = """
            Krungthai
            9104A8F4
            จำนวนเงิน: ฿250.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("006", result.bankPayload?.sendingBank)
    }

    @Test
    fun testMerchantTruncatedAt32Chars() {
        val slip = """
            ThisMerchantNameIsWayTooLongForTheAllowedLimitOf
            TOTAL: ${'$'}100.00
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals(32, result.merchant.length)
        assertEquals("ThisMerchantNameIsWayTooLongForT", result.merchant)
    }

    @Test
    fun testMerchantFallsBackToFirstLineWhenAllNoiseKeywords() {
        val slip = """
            RECEIPT
            welcome to our store
            TAX INVOICE
            โอนเงินสำเร็จ
            ActualStoreName
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("RECEIPT", result.merchant)
    }

    @Test
    fun testMultipleCrcCandidatesTakesFirstTag91() {
        val slip = """
            KBank
            9104A8F4
            6304BEEF
        """.trimIndent()
        val result = SlipDataParser.parse(slip)
        assertEquals("A8F4", result.bankPayload?.crc)
    }

    @Test
    fun testExtractQrAmountLengthMismatchReturnsNull() {
        assertNull(SlipDataParser.extractQrAmount("000201010211540350.005802TH6304ABCD"))
    }

    @Test
    fun testExtractQrAmountInvalidLengthDigitsReturnsNull() {
        assertNull(SlipDataParser.extractQrAmount("00020101021154XX50.005802TH6304ABCD"))
    }

    @Test
    fun testExtractQrAmountDeclaredLengthLongerThanActualReturnsNull() {
        assertNull(SlipDataParser.extractQrAmount("000201010211540850.1234005802TH6304ABCD"))
    }

    @Test
    fun testPromptPayByThaiWord() {
        val slip = """
            โอนเงินสำเร็จ
            จำนวนเงิน: ฿75.00
        """.trimIndent()
        assertTrue(SlipDataParser.parse(slip).isBankSlip)
    }

    @Test
    fun testPromptPayByEnglishWord() {
        val slip = """
            PromptPay Transfer Successful
            Amount: 75.00
        """.trimIndent()
        assertTrue(SlipDataParser.parse(slip).isBankSlip)
    }

    @Test
    fun testThaiBahtCurrencyInference() {
        val result = SlipDataParser.parse("ยอดรวม: ฿200.00")
        assertEquals("THB", result.currency)
    }

    @Test
    fun testBahtTextCurrencyInference() {
        val result = SlipDataParser.parse("Total 200.00 บาท")
        assertEquals("THB", result.currency)
    }
}