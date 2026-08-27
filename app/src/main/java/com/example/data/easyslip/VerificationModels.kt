package com.example.data.easyslip

import com.example.data.model.VerificationStatus

data class BankPayload(
    val crc: String,
    val sendingBank: String? = null,
    val transRef: String? = null,
    val checkDuplicate: Boolean = true,
    val matchAmount: Double? = null
)

data class VerifySlipResponse(
    val success: Boolean,
    val isDuplicate: Boolean = false,
    val matchedAccount: String? = null,
    val isAmountMatched: Boolean = true,
    val transRef: String? = null,
    val sendingBank: String? = null,
    val sendingBankName: String? = null,
    val receivingBank: String? = null,
    val receivingBankName: String? = null,
    val receiverName: String? = null,
    val senderName: String? = null,
    val amount: Double? = null,
    val transDate: String? = null,
    val transTime: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED
)

data class EasySlipRateLimitInfo(
    val remainingQuota: Int = 980,
    val totalQuota: Int = 1000,
    val resetEpochSeconds: Long = 0L,
    val isThrottled: Boolean = false
)
