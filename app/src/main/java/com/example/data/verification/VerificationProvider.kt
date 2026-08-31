package com.example.data.verification

enum class VerificationProvider(val id: String, val label: String) {
    EASYSLIP("easyslip", "EasySlip"),
    THUNDER("thunder", "ThunderAPI"),
    SLIP2GO("slip2go", "Slip2Go");

    companion object {
        fun fromId(id: String?): VerificationProvider =
            entries.firstOrNull { it.id == id } ?: EASYSLIP
    }
}
