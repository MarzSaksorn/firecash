package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(val context: Context? = null) {
    private val _currency = MutableStateFlow("THB")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _googleDriveSyncEnabled = MutableStateFlow(true)
    val googleDriveSyncEnabled: StateFlow<Boolean> = _googleDriveSyncEnabled.asStateFlow()
    val googleDriveSync: StateFlow<Boolean> = _googleDriveSyncEnabled.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(true)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()
    val autoBackup: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("2 hours ago")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    // EasySlip Integration Settings
    private val _easySlipEnabled = MutableStateFlow(true)
    val easySlipEnabled: StateFlow<Boolean> = _easySlipEnabled.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _checkDuplicates = MutableStateFlow(true)
    val checkDuplicates: StateFlow<Boolean> = _checkDuplicates.asStateFlow()

    fun setCurrency(newCurrency: String) {
        _currency.value = newCurrency
    }

    fun setGoogleDriveSync(enabled: Boolean) {
        _googleDriveSyncEnabled.value = enabled
    }

    fun setAutoBackup(enabled: Boolean) {
        _autoBackupEnabled.value = enabled
    }

    fun setEasySlipEnabled(enabled: Boolean) {
        _easySlipEnabled.value = enabled
    }

    fun setApiKey(key: String) {
        _apiKey.value = key
    }

    fun setCheckDuplicates(enabled: Boolean) {
        _checkDuplicates.value = enabled
    }

    fun triggerBackupNow() {
        _lastSyncTime.value = "Just now"
    }

    fun getCurrencySymbol(curr: String = _currency.value): String {
        return when (curr) {
            "THB" -> "฿"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            else -> "$"
        }
    }
}
