package com.example

import com.example.data.export.ExportManager
import com.example.data.model.Expense
import com.example.data.model.SourceType
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportManagerTest {

    @Test
    fun testGenerateCsv() = runBlocking {
        val testExpenses = listOf(
            Expense(
                id = 101,
                merchant = "Kasikorn Transfer, \"Special\"",
                amount = 250.00,
                date = "2023-10-24",
                time = "10:15 AM",
                category = "Transfer",
                tags = "Bank Slip",
                crc = "A8F4",
                sendingBank = "004",
                transRef = "TXN-1234",
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                sourceType = SourceType.CAMERA,
                currency = "THB"
            )
        )

        val csv = ExportManager.generateCsv(testExpenses, "2023-10-01", "2023-10-31")

        assertTrue(csv.startsWith("ID,Date,Time,Merchant,Category,Amount,Currency,Tags,Status,CRC,Bank,TransactionRef"))
        assertTrue(csv.contains("101,2023-10-24,10:15 AM,\"Kasikorn Transfer, \"\"Special\"\"\",\"Transfer\",250.00,THB,\"Bank Slip\",VERIFIED,A8F4,004,TXN-1234"))
    }

    @Test
    fun testGeneratePdfSummary() = runBlocking {
        val testExpenses = listOf(
            Expense(
                id = 1,
                merchant = "Cloud Services",
                amount = 99.00,
                date = "2023-10-24",
                category = "Software",
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                sourceType = SourceType.CAMERA
            )
        )

        val summary = ExportManager.generatePdfSummary(testExpenses, "2023-10-01", "2023-10-31", "$")

        assertTrue(summary.contains("FIRECASH EXPENSE & SLIP REPORT"))
        assertTrue(summary.contains("Total Transactions: 1"))
        assertTrue(summary.contains("Total Expenditure: $99.00"))
        assertTrue(summary.contains("Software"))
    }
}
