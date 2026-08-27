package com.example

import com.example.data.easyslip.BankPayload
import com.example.data.easyslip.EasySlipClient
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EasySlipClientTest {

    @Test
    fun testMockVerificationPipeline() = runBlocking {
        val client = EasySlipClient()

        val validPayload = BankPayload(
            crc = "A8F4",
            sendingBank = "004",
            transRef = "TXN-12345678",
            checkDuplicate = true,
            matchAmount = 450.00
        )

        val result = client.verifyBankSlip(validPayload)

        assertTrue(result.success)
        assertEquals(VerificationStatus.VERIFIED, result.verificationStatus)
        assertEquals(450.00, result.amount ?: 0.0, 0.01)
    }

    @Test
    fun testDuplicateDetection() = runBlocking {
        val client = EasySlipClient()

        val duplicatePayload = BankPayload(
            crc = "A8F49999", // Trigger duplicate mock pattern
            sendingBank = "004",
            transRef = "TXN-88889999",
            checkDuplicate = true,
            matchAmount = 250.00
        )

        val result = client.verifyBankSlip(duplicatePayload)

        assertEquals(VerificationStatus.DUPLICATE_DETECTED, result.verificationStatus)
        assertTrue(result.isDuplicate)
    }
}
