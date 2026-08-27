package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VerificationStatus {
    VERIFIED,
    UNVERIFIED,
    DUPLICATE_DETECTED,
    AMOUNT_MISMATCH,
    SLIP_NOT_FOUND,
    RATE_LIMITED
}

enum class SourceType {
    CAMERA,
    GALLERY,
    PDF_UPLOAD,
    MANUAL
}

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchant: String,
    val amount: Double,
    val date: String, // e.g. "2023-10-24"
    val time: String = "08:42 AM",
    val category: String, // e.g. "Food & Dining", "Travel", "Office Supplies", "Software", "Retail", "Other"
    val tags: String = "Business Trip", // comma separated
    val imageUrl: String? = null,
    val receiptText: String? = null,
    val isVerified: Boolean = true,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val crc: String? = null,
    val sendingBank: String? = null, // e.g. "004" (Kasikornbank), "014" (Siam Commercial)
    val transRef: String? = null,
    val isDuplicate: Boolean = false,
    val matchedAccount: String? = null,
    val isAmountMatched: Boolean = true,
    val sourceType: SourceType = SourceType.CAMERA,
    val currency: String = "USD",
    val dateGroup: String = "Today" // "Today", "Yesterday", "Previous"
)
