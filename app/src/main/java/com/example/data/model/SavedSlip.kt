package com.example.data.model

import com.example.data.easyslip.VerifySlipResponse

data class SavedSlip(
    val payload: String,
    val amount: Double? = null,
    val transRef: String? = null,
    val senderName: String? = null,
    val receiverName: String? = null,
    val date: String? = null,
    val time: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val slipData: VerifySlipResponse? = null,
    val isMoneyIn: Boolean = false,
    val savedAt: Long = System.currentTimeMillis(),
    val photoPath: String? = null,
    val amountMismatch: Boolean = false
)
