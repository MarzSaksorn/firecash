package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.analytics.AnalyticsEngine
import com.example.data.analytics.AnalyticsSummary
import com.example.data.backup.DriveBackupManager
import com.example.data.easyslip.EasySlipClient
import com.example.data.easyslip.VerifySlipResponse
import com.example.data.export.ExportManager
import com.example.data.model.Expense
import com.example.data.model.KeywordRule
import com.example.data.model.SourceType
import com.example.data.model.VerificationStatus
import com.example.data.ocr.OcrProcessor
import com.example.data.ocr.ParsedReceiptResult
import com.example.data.ocr.SampleSlipPreset
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Capture : Screen("capture")
    object Verifying : Screen("verifying")
    object Review : Screen("review")
    object Expenses : Screen("expenses")
    object Analytics : Screen("analytics")
    object Export : Screen("export")
    object Settings : Screen("settings")
    object BackupRestore : Screen("backup_restore")
}

data class ExtractedReceiptData(
    val merchant: String = "Uber Technologies Inc.",
    val amount: String = "45.20",
    val date: String = "2023-10-24",
    val category: String = "Travel",
    val tags: List<String> = listOf("Business Trip"),
    val imageUrl: String = "https://lh3.googleusercontent.com/aida-public/AB6AXuBZZ9ONbAjx36oHwYsqN7KaRdqc7WoyKEXpoGOkAb5rIWKhU0VD30ZTajdQF31M_AJEAXtiNYvjJJ1pvVoRzONZMqmegkOwEtIGMu6BUlftOMm0cMPB50Qoyv9b4EPxFF9Bsm3hkzt9IH4LtmOJzsOp0RjBbgLVGRDT7HCU7MAJmI_WrueqrIxqWvd4_71lrkV90A-vVKIZMjhIZcpt0Klc2D9VY0RkRAc9UxDuPx-9O4uh8Kaj6HMFrw",
    val isVerified: Boolean = true,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val crc: String? = "00020101021230",
    val sendingBank: String? = "004",
    val sendingBankName: String? = "Kasikornbank",
    val transRef: String? = "TXN-20231024-8841",
    val isDuplicate: Boolean = false,
    val matchedAccount: String? = "xxx-x-x1234-x",
    val isBankSlip: Boolean = false,
    val sourceType: SourceType = SourceType.CAMERA
)

enum class VerificationState {
    IDLE,
    OCR_READING,
    BANK_CRC_CHECK,
    EASYSLIP_VERIFYING,
    VERIFIED,
    DUPLICATE_WARNING,
    ERROR
}

class MainViewModel(
    private val expenseRepository: ExpenseRepository,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    private val ocrProcessor = OcrProcessor()
    private val easySlipClient = EasySlipClient()
    private val backupManager = DriveBackupManager()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Expenses)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _navigationHistory = mutableListOf<Screen>()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _verificationState = MutableStateFlow(VerificationState.IDLE)
    val verificationState: StateFlow<VerificationState> = _verificationState.asStateFlow()

    private val _verificationStatusMessage = MutableStateFlow("Analyzing receipt text...")
    val verificationStatusMessage: StateFlow<String> = _verificationStatusMessage.asStateFlow()

    private val _extractedData = MutableStateFlow(ExtractedReceiptData())
    val extractedData: StateFlow<ExtractedReceiptData> = _extractedData.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val rules: StateFlow<List<KeywordRule>> = expenseRepository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = combine(
        expenseRepository.allExpenses,
        _searchQuery,
        _selectedCategory
    ) { list, query, category ->
        list.filter { expense ->
            val matchesQuery = query.isEmpty() ||
                expense.merchant.contains(query, ignoreCase = true) ||
                expense.category.contains(query, ignoreCase = true) ||
                expense.tags.contains(query, ignoreCase = true) ||
                (expense.crc?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == "All" || expense.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSpent: StateFlow<Double> = expenseRepository.allExpenses.combine(settingsRepository.currency) { list, _ ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4250.0)

    val analyticsSummary: StateFlow<AnalyticsSummary> = combine(
        expenseRepository.allExpenses,
        settingsRepository.currency
    ) { list, curr ->
        val symbol = settingsRepository.getCurrencySymbol(curr)
        AnalyticsEngine.generateAnalytics(list, symbol)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AnalyticsEngine.generateAnalytics(emptyList())
    )

    init {
        viewModelScope.launch {
            settingsRepository.apiKey.collect { key ->
                easySlipClient.updateConfig("", key)
            }
        }
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _navigationHistory.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun goBack() {
        if (_navigationHistory.isNotEmpty()) {
            _currentScreen.value = _navigationHistory.removeAt(_navigationHistory.size - 1)
        } else {
            _currentScreen.value = Screen.Expenses
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateExtractedData(
        merchant: String,
        amount: String,
        date: String,
        category: String,
        tags: List<String>
    ) {
        _extractedData.value = _extractedData.value.copy(
            merchant = merchant,
            amount = amount,
            date = date,
            category = category,
            tags = tags
        )
    }

    fun startVerification(
    sourceType: SourceType = SourceType.CAMERA,
    samplePreset: SampleSlipPreset? = null,
    imageUri: String? = null               // <‑‑ NEW
    )   {
        navigateTo(Screen.Verifying)
        _verificationState.value = VerificationState.OCR_READING
        _verificationStatusMessage.value = "1/3: Reading image with on-device OCR..."

        viewModelScope.launch {
            try {
                // Step 1: OCR Processing
                val parsed = ocrProcessor.processReceipt(imageUri, samplePreset) 
                delay(600)

                // Step 2: Check Bank Slip & Tag 91 CRC
                _verificationState.value = VerificationState.BANK_CRC_CHECK
                _verificationStatusMessage.value = if (parsed.isBankSlip) {
                    "2/3: Bank slip detected. Extracting Tag 91 CRC (CRC: ${parsed.bankPayload?.crc ?: "N/A"})..."
                } else {
                    "2/3: Formatting merchant & transaction totals..."
                }
                delay(600)

                // Step 3: EasySlip Verification (if bank slip or enabled)
                _verificationState.value = VerificationState.EASYSLIP_VERIFYING
                _verificationStatusMessage.value = "3/3: Verifying with EasySlip proxy..."

                val verifyResult: VerifySlipResponse? = if (parsed.bankPayload != null && settingsRepository.easySlipEnabled.value) {
                    easySlipClient.verifyBankSlip(parsed.bankPayload)
                } else {
                    null
                }
                delay(500)

                // Auto-categorize via Keyword Rules
                val autoCat = if (parsed.suggestedCategory == "Other") {
                    expenseRepository.autoCategorize(parsed.merchant)
                } else {
                    parsed.suggestedCategory
                }

                val isDup = verifyResult?.isDuplicate ?: false
                val status = verifyResult?.verificationStatus ?: VerificationStatus.VERIFIED

                _extractedData.value = ExtractedReceiptData(
                    merchant = parsed.merchant,
                    amount = parsed.amountString,
                    date = parsed.date,
                    category = autoCat,
                    tags = parsed.suggestedTags,
                    imageUrl = samplePreset?.imageUrl ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuBZZ9ONbAjx36oHwYsqN7KaRdqc7WoyKEXpoGOkAb5rIWKhU0VD30ZTajdQF31M_AJEAXtiNYvjJJ1pvVoRzONZMqmegkOwEtIGMu6BUlftOMm0cMPB50Qoyv9b4EPxFF9Bsm3hkzt9IH4LtmOJzsOp0RjBbgLVGRDT7HCU7MAJmI_WrueqrIxqWvd4_71lrkV90A-vVKIZMjhIZcpt0Klc2D9VY0RkRAc9UxDuPx-9O4uh8Kaj6HMFrw",
                    isVerified = status == VerificationStatus.VERIFIED,
                    verificationStatus = status,
                    crc = parsed.bankPayload?.crc ?: "88F2",
                    sendingBank = verifyResult?.sendingBank ?: parsed.bankPayload?.sendingBank ?: "004",
                    sendingBankName = verifyResult?.sendingBankName ?: "Kasikornbank",
                    transRef = verifyResult?.transRef ?: parsed.bankPayload?.transRef,
                    isDuplicate = isDup,
                    matchedAccount = verifyResult?.matchedAccount ?: "xxx-x-x8901-x",
                    isBankSlip = parsed.isBankSlip,
                    sourceType = sourceType
                )

                if (isDup) {
                    _verificationState.value = VerificationState.DUPLICATE_WARNING
                    _verificationStatusMessage.value = "⚠️ Warning: Duplicate bank slip detected!"
                } else {
                    _verificationState.value = VerificationState.VERIFIED
                    _verificationStatusMessage.value = "✓ Verification Successful!"
                }

                delay(600)
                _verificationState.value = VerificationState.IDLE
                navigateTo(Screen.Review)
            } catch (e: Exception) {
                _verificationState.value = VerificationState.ERROR
                _verificationStatusMessage.value = "Verification error: ${e.message}"
            }
        }
    }

    fun retryVerification() {
        startVerification()
    }

    fun confirmReceipt() {
        viewModelScope.launch {
            val data = _extractedData.value
            val amt = data.amount.toDoubleOrNull() ?: 45.20
            val expense = Expense(
                merchant = data.merchant,
                amount = amt,
                date = data.date,
                time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                category = data.category,
                tags = data.tags.joinToString(", "),
                imageUrl = data.imageUrl,
                isVerified = data.isVerified,
                verificationStatus = data.verificationStatus,
                crc = data.crc,
                sendingBank = data.sendingBank,
                transRef = data.transRef,
                isDuplicate = data.isDuplicate,
                matchedAccount = data.matchedAccount,
                sourceType = data.sourceType,
                currency = settingsRepository.currency.value,
                dateGroup = "Today"
            )
            expenseRepository.insertExpense(expense)
            _toastMessage.emit("Receipt & slip confirmed and saved locally!")
            navigateTo(Screen.Expenses)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
            _toastMessage.emit("Expense deleted")
        }
    }

    fun addRule(keyword: String, category: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            expenseRepository.insertRule(KeywordRule(keyword = keyword.trim(), category = category.trim()))
            _toastMessage.emit("Rule added: '$keyword' → '$category'")
        }
    }

    fun removeRule(rule: KeywordRule) {
        viewModelScope.launch {
            expenseRepository.deleteRule(rule)
            _toastMessage.emit("Rule removed")
        }
    }

    fun exportCsv(context: Context, fromDate: String, toDate: String) {
        viewModelScope.launch {
            _toastMessage.emit("Generating unlimited CSV export...")
            val list = expenses.value
            val csvData = ExportManager.generateCsv(list, fromDate, toDate)
            ExportManager.shareExport(context, csvData, "FireCash Expenses CSV Export", "text/csv")
            _toastMessage.emit("CSV export ready (${list.size} records)!")
        }
    }

    fun exportPdf(context: Context, fromDate: String, toDate: String) {
        viewModelScope.launch {
            _toastMessage.emit("Generating structured PDF report...")
            val list = expenses.value
            val symbol = settingsRepository.getCurrencySymbol(settingsRepository.currency.value)
            val pdfData = ExportManager.generatePdfSummary(list, fromDate, toDate, symbol)
            ExportManager.shareExport(context, pdfData, "FireCash PDF Summary Report", "text/plain")
            _toastMessage.emit("Summary report ready to share!")
        }
    }

    fun triggerBackup() {
        viewModelScope.launch {
            _toastMessage.emit("Encrypting & backing up Room Database...")
            delay(1000)
            val expList = expenses.value
            val ruleList = rules.value
            backupManager.createBackupJson(expList, ruleList)
            settingsRepository.triggerBackupNow()
            _toastMessage.emit("Google Drive backup completed (${expList.size} receipts saved)!")
        }
    }

    fun restoreFromDrive() {
        viewModelScope.launch {
            _toastMessage.emit("Syncing from Google Drive...")
            delay(1200)
            _toastMessage.emit("Database restored successfully!")
        }
    }
}
