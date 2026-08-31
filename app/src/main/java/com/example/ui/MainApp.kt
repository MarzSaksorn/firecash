package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.documentfile.provider.DocumentFile
import com.example.service.BackgroundListenerService
import com.example.data.ocr.OcrProcessor
import com.example.data.ocr.SlipDataParser
import com.example.data.easyslip.VerifySlipResponse
import com.example.data.model.SavedSlip
import com.example.data.model.VerificationStatus
import com.example.data.verification.SlipVerificationManager
import com.example.data.verification.VerificationProvider
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import com.example.ui.screens.PhotoCaptureScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.QrPayloadScreen
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.theme.FireCashBackground

@Composable
fun MainApp(modifier: Modifier = Modifier) {
    var showCapture by remember { mutableStateOf(false) }
    var showPayload by remember { mutableStateOf(false) }
    var showSavedSlips by remember { mutableStateOf(true) }
    var showAnalytics by remember { mutableStateOf(false) }
    var qrPayload by remember { mutableStateOf("") }
    var slipData by remember { mutableStateOf<VerifySlipResponse?>(null) }
    var slipWarning by remember { mutableStateOf("") }
    var qrPhotoPath by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isBackgroundSyncing by remember { mutableStateOf(false) }
    var isUserSyncing by remember { mutableStateOf(false) }
    var batteryOptIgnored by remember { mutableStateOf(false) }
    var notificationAccessGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val verificationManager = remember { SlipVerificationManager() }
    val prefs = remember { context.getSharedPreferences("firecash_settings", Context.MODE_PRIVATE) }
    // Seed default notification whitelist presets on first launch (no-op once seeded / user-customized)
    com.example.service.NotificationPresets.seedIfNeeded(prefs)
    var backgroundListening by remember { mutableStateOf(prefs.getBoolean("background_listening", false)) }
    // App mode: "personal" (manual entry button on home card) or "shop" (camera button on home card)
    var appMode by remember { mutableStateOf(prefs.getString("app_mode", "personal") ?: "personal") }

    // Refresh statuses whenever the activity resumes (e.g. returning from system settings)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                batteryOptIgnored = (context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
                    .isIgnoringBatteryOptimizations(context.packageName)
                notificationAccessGranted = com.example.service.IncomeNotificationService.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Restart background listener on app launch if it was enabled
    val postNotifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && backgroundListening) {
            runCatching {
                context.startForegroundService(Intent(context, BackgroundListenerService::class.java))
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (backgroundListening && !isListenerRunning(context)) {
            runCatching {
                context.startForegroundService(Intent(context, BackgroundListenerService::class.java))
            }
        }
    }

    val savedSlips = remember {
        mutableStateListOf<SavedSlip>().apply { addAll(loadSlips(prefs)) }
    }

    var easySlipEnabled by remember {
        mutableStateOf(prefs.getBoolean("easy_slip_enabled", false))
    }
    var verificationProvider by remember {
        mutableStateOf(VerificationProvider.fromId(prefs.getString("verification_provider", null)))
    }
    var apiKey by remember {
        mutableStateOf(
            prefs.getString(providerKeyPref(verificationProvider), null)
                ?: prefs.getString("api_key", null)
                ?: ""
        )
    }
    var checkDuplicates by remember {
        mutableStateOf(prefs.getBoolean("check_duplicates", false))
    }
    var trackedFolderUris by remember {
        mutableStateOf(loadTrackedFolders(prefs))
    }
    var knownNames by remember {
        mutableStateOf(loadKnownNames(prefs))
    }
    var notificationIncomeEnabled by remember {
        mutableStateOf(prefs.getBoolean("notification_income_enabled", false))
    }
    var notificationExpenseEnabled by remember {
        mutableStateOf(prefs.getBoolean("notification_expense_enabled", false))
    }
    var disabledIncomePresets by remember {
        mutableStateOf(com.example.service.NotificationPresets.loadDisabledIncome(prefs))
    }
    var disabledExpensePresets by remember {
        mutableStateOf(com.example.service.NotificationPresets.loadDisabledExpense(prefs))
    }
    var notificationWhitelist by remember {
        mutableStateOf(com.example.service.NotificationPresets.mergeIncome(loadNotificationWhitelist(prefs), disabledIncomePresets))
    }
    var notificationExpenseWhitelist by remember {
        mutableStateOf(com.example.service.NotificationPresets.mergeExpense(loadNotificationWhitelistExpense(prefs), disabledExpensePresets))
    }
    val seenPayloads = remember {
        mutableSetOf<String>().apply { addAll(loadSeenPayloads(prefs)) }
    }
    // Cache of already-processed tracked-folder files (by content:// uri) — only new files get OCR'd/synced
    val processedFiles = remember {
        mutableSetOf<String>().apply { addAll(loadProcessedFiles(prefs)) }
    }

    // System back handling — mirrors in-app navigation, returns to previous state
    BackHandler(enabled = showPayload) {
        showPayload = false
        showSavedSlips = true
        showCapture = false
        showAnalytics = false
    }
    BackHandler(enabled = showAnalytics) {
        showAnalytics = false
        showSavedSlips = true
    }
    BackHandler(enabled = showCapture) {
        // Capture is now secondary (homepage is Account) → back returns to Account
        showCapture = false
        showSavedSlips = true
    }
    BackHandler(enabled = !showPayload && !showCapture && !showSavedSlips && !showAnalytics) {
        // Settings (unified) → back to Account (homepage)
        showSavedSlips = true
    }

    // Keep Account list live when IncomeNotificationService writes to prefs in background
    androidx.compose.runtime.DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREFS_SLIPS) {
                val fresh = loadSlips(prefs)
                // Simple diff check — replace if differs
                if (fresh.size != savedSlips.size || fresh.toSet() != savedSlips.toSet()) {
                    savedSlips.clear()
                    savedSlips.addAll(fresh)
                }
                val freshSeen = loadSeenPayloads(prefs)
                if (freshSeen != seenPayloads) {
                    seenPayloads.clear()
                    seenPayloads.addAll(freshSeen)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    suspend fun verifyWithEasySlip(payload: String): VerifySlipResponse? {
        if (appMode == "personal") return null // personal mode never calls the verification API
        if (!easySlipEnabled || apiKey.isBlank()) {
            slipWarning = if (easySlipEnabled) {
                "No API key set — add your ${verificationProvider.label} API key in Settings to verify this slip."
            } else {
                "Slip verification is disabled — enable it in Settings and add your API key to verify slips."
            }
            return null
        }
        slipWarning = ""
        verificationManager.updateConfig(verificationProvider, apiKey)
        return verificationManager.verifyPayload(payload, checkDuplicate = checkDuplicates)
    }

    fun isKnownName(name: String?, known: List<String>): Boolean {
        if (name.isNullOrBlank()) return false
        val norm = name.trim().lowercase(java.util.Locale.ROOT)
        return known.any { it.trim().lowercase(java.util.Locale.ROOT) == norm }
    }

    fun extractAmount(text: String): Double? {
        val regex = Regex("""\d{1,3}(?:,\d{3})*(?:\.\d+)?|\d+(?:\.\d+)?""")
        return regex.find(text)?.value?.replace(",", "")?.toDoubleOrNull()
    }

    suspend fun addSlip(payload: String, isMoneyIn: Boolean = false, photoPath: String? = null) {
        val verified = runCatching { verifyWithEasySlip(payload) }.getOrNull()
        // Fallback when EasySlip disabled/offline: keep amount from raw payload so details page still shows data
        val fallbackAmount = extractAmount(payload)
        val now = System.currentTimeMillis()
        val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
        val result = verified ?: VerifySlipResponse(
            success = false,
            isDuplicate = false,
            transRef = null,
            amount = fallbackAmount,
            transDate = null,
            transTime = null,
            senderName = null,
            receiverName = null,
            sendingBank = null,
            sendingBankName = null,
            receivingBank = null,
            receivingBankName = null,
            isAmountMatched = false,
            verificationStatus = VerificationStatus.UNVERIFIED,
            errorMessage = slipWarning
        )
        slipData = result
        // Auto-resolve isMoneyIn based on known names:
        // - if both sender & receiver are known -> transfer (neutral, stored as false, UI shows Transfer)
        // - if receiver is known -> income
        // - if sender is known -> expense
        // - else fallback to requested isMoneyIn (manual toggle / default)
        val senderKnown = isKnownName(result.senderName, knownNames)
        val receiverKnown = isKnownName(result.receiverName, knownNames)
        val resolvedIsMoneyIn = when {
            senderKnown && receiverKnown -> false // transfer - will be excluded from balance
            receiverKnown -> true
            senderKnown -> false
            else -> isMoneyIn
        }
        val slip = SavedSlip(
            payload = payload,
            amount = result.amount ?: fallbackAmount,
            transRef = result.transRef,
            senderName = result.senderName,
            receiverName = result.receiverName,
            date = result.transDate ?: sdfDate.format(java.util.Date(now)),
            time = result.transTime ?: sdfTime.format(java.util.Date(now)),
            verificationStatus = result.verificationStatus,
            slipData = result,
            isMoneyIn = resolvedIsMoneyIn,
            photoPath = photoPath
        )

        // Dedupe: re-scanning the same slip updates the existing entry instead of adding a log
        val existingIndex = savedSlips.indexOfFirst { saved ->
            if (result?.transRef?.isNotBlank() == true) {
                saved.transRef == result.transRef
            } else {
                saved.payload == payload
            }
        }
        val finalSlip = if (existingIndex >= 0 && savedSlips[existingIndex].photoPath != null && photoPath == null) {
            // keep existing photo when re-adding without one
            slip.copy(photoPath = savedSlips[existingIndex].photoPath)
        } else {
            slip
        }
        if (existingIndex >= 0) {
            savedSlips[existingIndex] = finalSlip
        } else {
            savedSlips.add(finalSlip)
        }

        seenPayloads.add(payload)
        saveSlips(prefs, savedSlips)
        saveSeenPayloads(prefs, seenPayloads)
    }

    fun handlePayload(payload: String, photoPath: String? = null) {
        if (payload.isBlank()) return
        qrPayload = payload
        qrPhotoPath = photoPath
        isLoading = true
        scope.launch {
            addSlip(payload, photoPath = photoPath)
            isLoading = false
            showCapture = false
            showPayload = true
        }
    }

    fun savePayload() {
        if (qrPayload.isBlank()) return
        isLoading = true
        scope.launch {
            addSlip(qrPayload)
            isLoading = false
            showPayload = false
            showSavedSlips = true
            showCapture = false
        }
    }

    fun addManualSlip(amount: Double, isMoneyIn: Boolean, note: String) {
        if (amount <= 0) return
        val now = System.currentTimeMillis()
        val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
        val slip = SavedSlip(
            payload = "manual:${now}",
            amount = amount,
            transRef = "MANUAL-${now}",
            senderName = if (!isMoneyIn) note.ifBlank { "Manual" } else null,
            receiverName = if (isMoneyIn) note.ifBlank { "Manual" } else null,
            date = sdfDate.format(java.util.Date(now)),
            time = sdfTime.format(java.util.Date(now)),
            verificationStatus = VerificationStatus.UNVERIFIED,
            slipData = null,
            isMoneyIn = isMoneyIn,
            savedAt = now
        )
        savedSlips.add(slip)
        seenPayloads.add(slip.payload)
        saveSlips(prefs, savedSlips)
        saveSeenPayloads(prefs, seenPayloads)
    }

    // Personal mode: log a slip from the recognized text of a slip photo, without calling
    // any verification API. Amount/date/merchant come from SlipDataParser; from/to lines
    // (จาก/ถึง or FROM/TO) decide money in/out via the known-names logic.
    fun addOcrSlip(rawText: String, photoPath: String? = null) {
        if (rawText.isBlank()) return
        val parsed = SlipDataParser.parse(rawText)
        val (sender, receiver) = SlipDataParser.extractParties(rawText)
        val now = System.currentTimeMillis()
        val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
        // Trust the parsed amount only if the text actually contains a number; otherwise
        // SlipDataParser's 45.20 fallback would fabricate an amount for unreadable slips.
        val amount = if (parsed.amount > 0.0 && extractAmount(rawText) != null) parsed.amount else null
        // Counterparty: use the from/to lines when recognized, else the merchant line
        val senderName = sender ?: if (parsed.isBankSlip) null else parsed.merchant
        val receiverName = receiver ?: parsed.merchant
        val result = VerifySlipResponse(
            success = false,
            isDuplicate = false,
            transRef = parsed.bankPayload?.transRef,
            amount = amount,
            transDate = parsed.date,
            transTime = parsed.time,
            senderName = senderName,
            receiverName = receiverName,
            sendingBank = parsed.bankPayload?.sendingBank,
            isAmountMatched = false,
            verificationStatus = VerificationStatus.UNVERIFIED,
            errorMessage = "Personal mode — read from slip photo, not API-verified"
        )
        val senderKnown = isKnownName(result.senderName, knownNames)
        val receiverKnown = isKnownName(result.receiverName, knownNames)
        val resolvedIsMoneyIn = when {
            senderKnown && receiverKnown -> false // transfer
            receiverKnown -> true // money in
            senderKnown -> false // money out
            else -> false // unknown direction defaults to expense
        }
        val slip = SavedSlip(
            payload = "ocr:$now",
            amount = result.amount,
            transRef = result.transRef,
            senderName = result.senderName,
            receiverName = result.receiverName,
            date = result.transDate ?: sdfDate.format(java.util.Date(now)),
            time = result.transTime ?: sdfTime.format(java.util.Date(now)),
            verificationStatus = result.verificationStatus,
            slipData = result,
            isMoneyIn = resolvedIsMoneyIn,
            photoPath = photoPath
        )
        savedSlips.add(slip)
        seenPayloads.add(slip.payload)
        saveSlips(prefs, savedSlips)
        saveSeenPayloads(prefs, seenPayloads)
        android.util.Log.d("FireCashOCR", "addOcrSlip amount=$amount merchant=${receiverName ?: senderName ?: "?"} isIn=$resolvedIsMoneyIn")
    }

    // Export everything (slips + settings + prefs) to a JSON file and share it
    fun exportAllData() {
        try {
            val root = JSONObject()
            root.put("app", "FireCash")
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())

            val slipsArr = JSONArray()
            savedSlips.forEach { slipsArr.put(slipToJson(it)) }
            root.put("slips", slipsArr)

            root.put("seenPayloads", JSONArray(seenPayloads.toList()))
            root.put("processedFiles", JSONArray(processedFiles.toList()))
            root.put("trackedFolders", JSONArray(trackedFolderUris))
            root.put("knownNames", JSONArray(knownNames))

            fun wlArr(list: List<com.example.service.WhitelistedApp>): JSONArray {
                val arr = JSONArray()
                list.forEach { e -> arr.put(JSONObject().put("package", e.packageName).put("prefix", e.prefix)) }
                return arr
            }
            root.put("notificationWhitelist", wlArr(notificationWhitelist))
            root.put("notificationExpenseWhitelist", wlArr(notificationExpenseWhitelist))

            val settings = JSONObject()
            settings.put("easy_slip_enabled", easySlipEnabled)
            settings.put("verification_provider", verificationProvider.id)
            settings.put("api_key_easyslip", prefs.getString("api_key_easyslip", null) ?: prefs.getString("api_key", null) ?: "")
            settings.put("api_key_thunder", prefs.getString("api_key_thunder", "") ?: "")
            settings.put("api_key_slip2go", prefs.getString("api_key_slip2go", "") ?: "")
            settings.put("check_duplicates", checkDuplicates)
            settings.put("notification_income_enabled", notificationIncomeEnabled)
            settings.put("notification_expense_enabled", notificationExpenseEnabled)
            settings.put("background_listening", backgroundListening)
            settings.put("app_mode", appMode)
            root.put("settings", settings)

            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, "FireCash_Backup_${System.currentTimeMillis()}.json")
            file.writeText(root.toString(2))

            val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(Intent.createChooser(intent, "Export FireCash data")) }
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Import everything from an exported JSON file (phone-to-phone transfer)
    fun importAllData(path: String) {
        try {
            val text = java.io.File(path).readText()
            val root = JSONObject(text)

            root.optJSONArray("slips")?.let { arr ->
                val newSlips = mutableListOf<SavedSlip>()
                for (i in 0 until arr.length()) {
                    slipFromJson(arr.getJSONObject(i))?.let { newSlips.add(it) }
                }
                savedSlips.clear()
                savedSlips.addAll(newSlips)
                saveSlips(prefs, savedSlips)
            }

            fun readStrArr(name: String): List<String> =
                root.optJSONArray(name)?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()

            seenPayloads.clear(); seenPayloads.addAll(readStrArr("seenPayloads")); saveSeenPayloads(prefs, seenPayloads)
            processedFiles.clear(); processedFiles.addAll(readStrArr("processedFiles")); saveProcessedFiles(prefs, processedFiles)
            trackedFolderUris = readStrArr("trackedFolders"); saveTrackedFolders(prefs, trackedFolderUris)
            knownNames = readStrArr("knownNames"); saveKnownNames(prefs, knownNames)

            fun readWl(name: String): List<com.example.service.WhitelistedApp> =
                root.optJSONArray(name)?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        val o = arr.optJSONObject(i) ?: return@mapNotNull null
                        com.example.service.WhitelistedApp(o.optString("package"), o.optString("prefix"))
                    }
                } ?: emptyList()

            if (root.has("notificationWhitelist")) {
                notificationWhitelist = com.example.service.NotificationPresets.mergeIncome(readWl("notificationWhitelist"), disabledIncomePresets)
                saveNotificationWhitelist(prefs, notificationWhitelist)
            }
            if (root.has("notificationExpenseWhitelist")) {
                notificationExpenseWhitelist = com.example.service.NotificationPresets.mergeExpense(readWl("notificationExpenseWhitelist"), disabledExpensePresets)
                saveNotificationWhitelistExpense(prefs, notificationExpenseWhitelist)
            }

            root.optJSONObject("settings")?.let { s ->
                easySlipEnabled = s.optBoolean("easy_slip_enabled", false); prefs.edit().putBoolean("easy_slip_enabled", easySlipEnabled).apply()
                val importedProvider = VerificationProvider.fromId(s.optString("verification_provider", null).ifEmpty { null })
                if (s.has("api_key_easyslip") || s.has("verification_provider")) {
                    prefs.edit().putString("api_key_easyslip", s.optString("api_key_easyslip", "")).apply()
                    prefs.edit().putString("api_key_thunder", s.optString("api_key_thunder", "")).apply()
                    prefs.edit().putString("api_key_slip2go", s.optString("api_key_slip2go", "")).apply()
                    verificationProvider = importedProvider
                    prefs.edit().putString("verification_provider", importedProvider.id).apply()
                    apiKey = prefs.getString(providerKeyPref(importedProvider), "") ?: ""
                } else {
                    // legacy export: single api_key
                    apiKey = s.optString("api_key", ""); prefs.edit().putString("api_key_easyslip", apiKey).apply()
                }
                checkDuplicates = s.optBoolean("check_duplicates", false); prefs.edit().putBoolean("check_duplicates", checkDuplicates).apply()
                notificationIncomeEnabled = s.optBoolean("notification_income_enabled", false); prefs.edit().putBoolean("notification_income_enabled", notificationIncomeEnabled).apply()
                notificationExpenseEnabled = s.optBoolean("notification_expense_enabled", false); prefs.edit().putBoolean("notification_expense_enabled", notificationExpenseEnabled).apply()
                backgroundListening = s.optBoolean("background_listening", false); prefs.edit().putBoolean("background_listening", backgroundListening).apply()
                appMode = s.optString("app_mode", appMode); prefs.edit().putString("app_mode", appMode).apply()
            }

            if (backgroundListening) {
                runCatching { context.startForegroundService(Intent(context, BackgroundListenerService::class.java)) }
            }
            android.widget.Toast.makeText(context, "Import complete (${savedSlips.size} slips)", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Import failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun importSlips(paths: List<String>) {
        if (paths.isEmpty()) return
        isLoading = true
        scope.launch {
            for (path in paths) {
                if (appMode == "personal") {
                    // Personal mode: extract text from the photo, no QR payload / no server
                    val ocrText = OcrProcessor(context).recognizeText(path, scanCenterOnly = false)
                    if (ocrText.isNotBlank()) addOcrSlip(ocrText, photoPath = path)
                } else {
                    val payload = OcrProcessor(context).processReceipt(path, scanCenterOnly = false).rawText
                    if (payload.isNotBlank()) {
                        addSlip(payload, photoPath = path)
                    }
                }
            }
            isLoading = false
        }
    }

    suspend fun scanFolder(uriStr: String) {
        val root = runCatching {
            DocumentFile.fromTreeUri(context, Uri.parse(uriStr))
        }.getOrNull()
        val files = root?.listFiles()
            ?.filter { it.isFile && it.type?.startsWith("image/") == true }
            .orEmpty()

        for (file in files) {
            val fileKey = file.uri.toString()
            // Cache hit — already processed on a previous open, skip entirely
            if (fileKey in processedFiles) continue

            val tempFile = File(
                context.cacheDir,
                "tracked_${System.currentTimeMillis()}_${Math.random() * 10000}".replace(".", "")
                    .plus(".jpg")
            )
            val copied = runCatching {
                context.contentResolver.openInputStream(file.uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                } != null
            }.getOrDefault(false)
            if (!copied) continue

            if (appMode == "personal") {
                // Personal mode: extract text from the photo, no QR payload / no server
                val ocrText = OcrProcessor(context)
                    .recognizeText(tempFile.absolutePath, scanCenterOnly = false)
                if (ocrText.isNotBlank()) {
                    addOcrSlip(ocrText, photoPath = file.uri.toString())
                }
            } else {
                val payload = OcrProcessor(context)
                    .processReceipt(tempFile.absolutePath, scanCenterOnly = false)
                    .rawText
                if (payload.isNotBlank() && payload !in seenPayloads) {
                    seenPayloads.add(payload)
                    // store the original content:// uri so the slip links to the real photo on device
                    addSlip(payload, photoPath = file.uri.toString())
                }
            }
            // Mark processed (even blank OCR) so future opens only handle genuinely new files
            processedFiles.add(fileKey)
            saveProcessedFiles(prefs, processedFiles)
        }
    }

    fun syncTrackedFolder() {
        if (trackedFolderUris.isEmpty()) return
        if (isLoading) return
        isLoading = true
        scope.launch {
            for (uriStr in trackedFolderUris) {
                scanFolder(uriStr)
            }
            isLoading = false
        }
    }

    fun syncTrackedFolderInBackground() {
        if (trackedFolderUris.isEmpty()) return
        if (isLoading || isBackgroundSyncing) return
        isBackgroundSyncing = true
        isUserSyncing = true
        scope.launch {
            try {
                for (uriStr in trackedFolderUris) {
                    scanFolder(uriStr)
                }
            } finally {
                isBackgroundSyncing = false
                isUserSyncing = false
            }
        }
    }

    // Full resync: clear processed-file cache (re-detect every photo) and re-verify ALL slips on server
    fun fullResync() {
        if (isLoading || isBackgroundSyncing) return
        processedFiles.clear()
        saveProcessedFiles(prefs, processedFiles)
        isLoading = true
        isUserSyncing = true
        scope.launch {
            try {
                for (uriStr in trackedFolderUris) {
                    scanFolder(uriStr)
                }
                val verifyable = savedSlips.filter {
                    !it.payload.startsWith("manual:") && !it.payload.startsWith("notif:")
                }
                for (old in verifyable.toList()) {
                    val result = runCatching { verifyWithEasySlip(old.payload) }.getOrNull() ?: continue
                    val resolvedIsMoneyIn = when {
                        isKnownName(result.senderName, knownNames) && isKnownName(result.receiverName, knownNames) -> false
                        isKnownName(result.receiverName, knownNames) -> true
                        isKnownName(result.senderName, knownNames) -> false
                        else -> old.isMoneyIn
                    }
                    val idx = savedSlips.indexOfFirst { it.payload == old.payload }
                    if (idx >= 0) {
                        savedSlips[idx] = old.copy(
                            amount = result.amount ?: old.amount,
                            transRef = result.transRef ?: old.transRef,
                            senderName = result.senderName ?: old.senderName,
                            receiverName = result.receiverName ?: old.receiverName,
                            date = result.transDate ?: old.date,
                            time = result.transTime ?: old.time,
                            verificationStatus = result.verificationStatus,
                            slipData = result,
                            isMoneyIn = resolvedIsMoneyIn
                        )
                    }
                }
                saveSlips(prefs, savedSlips)
            } finally {
                isLoading = false
            }
        }
    }

    fun resyncUnverifiedSlips() {
        if (appMode == "personal") return // personal mode never verifies via API
        if (!easySlipEnabled || apiKey.isBlank()) return
        if (isLoading || isBackgroundSyncing) return
        val unverified = savedSlips.filter {
            it.verificationStatus == VerificationStatus.UNVERIFIED &&
            !it.payload.startsWith("manual:") && !it.payload.startsWith("notif:")
        }
        if (unverified.isEmpty()) return
        isLoading = true
        scope.launch {
            try {
                for (old in unverified.toList()) {
                    val result = runCatching { verifyWithEasySlip(old.payload) }.getOrNull() ?: continue
                    // Preserve amount/date fallback and re-evaluate income via known names
                    val resolvedIsMoneyIn = when {
                        isKnownName(result.senderName, knownNames) && isKnownName(result.receiverName, knownNames) -> false
                        isKnownName(result.receiverName, knownNames) -> true
                        isKnownName(result.senderName, knownNames) -> false
                        else -> old.isMoneyIn
                    }
                    val updated = old.copy(
                        amount = result.amount ?: old.amount,
                        transRef = result.transRef ?: old.transRef,
                        senderName = result.senderName ?: old.senderName,
                        receiverName = result.receiverName ?: old.receiverName,
                        date = result.transDate ?: old.date,
                        time = result.transTime ?: old.time,
                        verificationStatus = result.verificationStatus,
                        slipData = result,
                        isMoneyIn = resolvedIsMoneyIn
                    )
                    val idx = savedSlips.indexOfFirst { it.payload == old.payload || (old.transRef != null && it.transRef == old.transRef) }
                    if (idx >= 0) savedSlips[idx] = updated
                }
                saveSlips(prefs, savedSlips)
            } finally {
                isLoading = false
                isUserSyncing = false
            }
        }
    }

    fun onFolderSelected(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val uriStr = uri.toString()
        if (uriStr !in trackedFolderUris) {
            trackedFolderUris = trackedFolderUris + uriStr
            saveTrackedFolders(prefs, trackedFolderUris)
        }
        syncTrackedFolder()
    }

    fun onRemoveFolder(uriStr: String) {
        trackedFolderUris = trackedFolderUris - uriStr
        saveTrackedFolders(prefs, trackedFolderUris)
    }

    fun onDeleteSlip(slip: SavedSlip) {
        // Deletable when unverified or the slip has no transaction reference (safety)
        val isDeletable = slip.verificationStatus == VerificationStatus.UNVERIFIED ||
            slip.transRef.isNullOrBlank() ||
            slip.amount == null
        if (!isDeletable) return
        val idx = savedSlips.indexOfFirst { it.payload == slip.payload && it.savedAt == slip.savedAt }
        if (idx >= 0) {
            savedSlips.removeAt(idx)
            saveSlips(prefs, savedSlips)
            // also remove from seen set so it can be re-scanned if valid later
            seenPayloads.remove(slip.payload)
            saveSeenPayloads(prefs, seenPayloads)
        }
    }

    Scaffold(
        containerColor = FireCashBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {
        if (showPayload) {
            QrPayloadScreen(
                payload = qrPayload,
                slipData = slipData,
                warning = slipWarning,
                photoPath = qrPhotoPath,
                showVerification = appMode != "personal",
                onBack = {
                    showPayload = false
                    showSavedSlips = true
                    showCapture = false
                }
            )
        } else if (showCapture) {
            PhotoCaptureScreen(
                onPhotoCaptured = { path ->
                    scope.launch {
                        if (appMode == "personal") {
                            // Personal mode: read the slip text from the photo, no verification API
                            isLoading = true
                            val ocrText = OcrProcessor(context).recognizeText(path)
                            isLoading = false
                            if (ocrText.isBlank()) {
                                Toast.makeText(context, "Couldn't read text from the slip photo", Toast.LENGTH_SHORT).show()
                            } else {
                                addOcrSlip(ocrText, photoPath = path)
                            }
                            showCapture = false
                            showSavedSlips = true
                        } else {
                            val payload = OcrProcessor(context).processReceipt(path).rawText
                            handlePayload(payload, photoPath = path)
                        }
                    }
                },
                onFileSelected = { /* unused – picker handled inside PhotoCaptureScreen */ },
                onImageSelected = { path ->
                    scope.launch {
                        if (appMode == "personal") {
                            isLoading = true
                            val ocrText = OcrProcessor(context).recognizeText(path, scanCenterOnly = false)
                            isLoading = false
                            if (ocrText.isBlank()) {
                                Toast.makeText(context, "Couldn't read text from the slip photo", Toast.LENGTH_SHORT).show()
                            } else {
                                addOcrSlip(ocrText, photoPath = path)
                            }
                            showCapture = false
                            showSavedSlips = true
                        } else {
                            val payload = OcrProcessor(context).processReceipt(path, scanCenterOnly = false).rawText
                            handlePayload(payload, photoPath = path)
                        }
                    }
                },
                onQrDetected = { payload ->
                    if (appMode == "personal") {
                        // Personal mode never verifies via API; only the photo-text path is used
                    } else {
                        handlePayload(payload)
                    }
                },
                isLoading = isLoading,
                onNavigateToSettings = {
                    showCapture = false
                    showSavedSlips = false
                    showAnalytics = false
                },
                onNavigateToAccount = {
                    showCapture = false
                    showSavedSlips = true
                    showAnalytics = false
                },
                payloadText = qrPayload,
                modifier = Modifier.fillMaxSize()
            )
        } else if (showSavedSlips) {
            AccountScreen(
                slips = savedSlips,
                knownNames = knownNames,
                isLoading = isLoading,
                isBackgroundSyncing = isBackgroundSyncing,
                isUserSyncing = isUserSyncing,
                onDeleteSlip = { slip -> onDeleteSlip(slip) },
                onBack = {
                    showSavedSlips = false
                    showCapture = true
                },
                onSlipClick = { slip ->
                    qrPayload = slip.payload
                    slipData = slip.slipData ?: VerifySlipResponse(
                        success = false,
                        isDuplicate = false,
                        amount = slip.amount ?: extractAmount(slip.payload),
                        transRef = slip.transRef,
                        senderName = slip.senderName,
                        receiverName = slip.receiverName,
                        transDate = slip.date,
                        transTime = slip.time,
                        sendingBank = null,
                        sendingBankName = null,
                        receivingBank = null,
                        receivingBankName = null,
                        verificationStatus = slip.verificationStatus,
                        errorMessage = "Not verified — enable EasySlip and Sync unverified in Settings",
                        isAmountMatched = false
                    )
                    qrPhotoPath = slip.photoPath
                    slipWarning = ""
                    showSavedSlips = false
                    showPayload = true
                },
                onOpenSettings = {
                    showSavedSlips = false
                    showCapture = false
                    showAnalytics = false
                },
                onOpenAnalytics = {
                    showSavedSlips = false
                    showAnalytics = true
                },
                onOpenCamera = {
                    showSavedSlips = false
                    showCapture = true
                },
                appMode = appMode,
                onAddManual = { amount, isMoneyIn, note ->
                    addManualSlip(amount, isMoneyIn, note)
                },
                onAutoSync = { syncTrackedFolderInBackground() },
                onSyncNow = { syncTrackedFolderInBackground() },
                onFullResync = { fullResync() }
            )
        } else if (showAnalytics) {
            AnalyticsScreen(
                slips = savedSlips,
                knownNames = knownNames,
                onBack = {
                    showAnalytics = false
                    showSavedSlips = true
                },
                onRefresh = { syncTrackedFolder() }
            )
        } else {
            SettingsScreen(
    rules = emptyList(),
    appMode = appMode,
    onSetAppMode = { mode ->
        appMode = mode
        prefs.edit().putString("app_mode", mode).apply()
    },
    easySlipEnabled = easySlipEnabled,
    apiKey = apiKey,
    verificationProvider = verificationProvider,
    checkDuplicates = checkDuplicates,
    knownNames = knownNames,
    unverifiedCount = savedSlips.count { it.verificationStatus == VerificationStatus.UNVERIFIED },
    onToggleEasySlip = { enabled ->
        easySlipEnabled = enabled
        prefs.edit().putBoolean("easy_slip_enabled", enabled).apply()
        if (enabled && apiKey.isNotBlank()) resyncUnverifiedSlips()
    },
    onProviderChange = { p ->
        prefs.edit().putString(providerKeyPref(verificationProvider), apiKey).apply()
        verificationProvider = p
        prefs.edit().putString("verification_provider", p.id).apply()
        apiKey = prefs.getString(providerKeyPref(p), "") ?: ""
        verificationManager.updateConfig(p, apiKey)
        if (easySlipEnabled && apiKey.isNotBlank()) resyncUnverifiedSlips()
    },
    onUpdateApiKey = { key ->
        apiKey = key
        prefs.edit().putString(providerKeyPref(verificationProvider), key).apply()
        if (easySlipEnabled && key.isNotBlank()) resyncUnverifiedSlips()
    },
    onToggleCheckDuplicates = { enabled ->
        checkDuplicates = enabled
        prefs.edit().putBoolean("check_duplicates", enabled).apply()
    },
                    onAddKnownName = { name ->
                        val trimmed = name.trim()
                        if (trimmed.isNotEmpty() && trimmed !in knownNames) {
                            knownNames = knownNames + trimmed
                            saveKnownNames(prefs, knownNames)
                        }
                    },
                    onRemoveKnownName = { name ->
                        knownNames = knownNames - name
                        saveKnownNames(prefs, knownNames)
                    },
                    onSyncUnverified = { resyncUnverifiedSlips() },
                    isLoading = isLoading,
                    trackedFolders = trackedFolderUris,
                    onFolderSelected = { uri -> onFolderSelected(uri) },
                    onRemoveFolder = { uriStr -> onRemoveFolder(uriStr) },
                    onSyncNow = { syncTrackedFolder() },
                    onImportSlips = { paths -> importSlips(paths) },
                    onExportData = { exportAllData() },
                    onImportData = { path -> importAllData(path) },
                    notificationIncomeEnabled = notificationIncomeEnabled,
                    notificationExpenseEnabled = notificationExpenseEnabled,
                    notificationWhitelist = notificationWhitelist,
                    notificationExpenseWhitelist = notificationExpenseWhitelist,
                    permanentIncomeApps = com.example.service.NotificationPresets.incomePresets,
                    permanentExpenseApps = com.example.service.NotificationPresets.expensePresets,
                    disabledIncomePresets = disabledIncomePresets,
                    disabledExpensePresets = disabledExpensePresets,
                    notificationAccessGranted = notificationAccessGranted,
                    batteryOptIgnored = batteryOptIgnored,
                    backgroundListening = backgroundListening,
                    onToggleBackgroundListening = { enabled ->
                        backgroundListening = enabled
                        prefs.edit().putBoolean("background_listening", enabled).apply()
                        if (enabled) {
                            postNotifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            runCatching {
                                context.startForegroundService(Intent(context, BackgroundListenerService::class.java))
                            }
                        } else {
                            runCatching {
                                context.stopService(Intent(context, BackgroundListenerService::class.java))
                            }
                        }
                    },
                    onRequestDisableBatteryOptimization = {
                        try {
                            context.startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            )
                        } catch (_: Exception) {
                            runCatching {
                                context.startActivity(
                                    Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                            }
                        }
                    },
                    onToggleNotificationIncome = { enabled ->
                        notificationIncomeEnabled = enabled
                        prefs.edit().putBoolean("notification_income_enabled", enabled).apply()
                    },
                    onToggleNotificationExpense = { enabled ->
                        notificationExpenseEnabled = enabled
                        prefs.edit().putBoolean("notification_expense_enabled", enabled).apply()
                    },
                    onAddWhitelistedApp = { pkg, prefix ->
                        val t = pkg.trim()
                        val p = prefix.trim()
                        if (t.isNotEmpty() && notificationWhitelist.none { it.packageName == t && it.prefix == p }) {
                            notificationWhitelist = notificationWhitelist + com.example.service.WhitelistedApp(packageName = t, prefix = p)
                            saveNotificationWhitelist(prefs, notificationWhitelist)
                        }
                    },
                    onRemoveWhitelistedApp = { pkgAndPrefix ->
                        val parts = pkgAndPrefix.split("|", limit = 2)
                        val pkg = parts.getOrNull(0) ?: pkgAndPrefix
                        val pref = parts.getOrNull(1) ?: ""
                        val entry = com.example.service.WhitelistedApp(packageName = pkg, prefix = pref)
                        if (com.example.service.NotificationPresets.isIncomePermanent(entry)) return@SettingsScreen
                        notificationWhitelist = notificationWhitelist.filterNot { it.packageName == pkg && it.prefix == pref }
                        saveNotificationWhitelist(prefs, notificationWhitelist)
                    },
                    onToggleIncomePreset = { entry, enabled ->
                        com.example.service.NotificationPresets.setDisabledIncome(prefs, entry, !enabled)
                        disabledIncomePresets = com.example.service.NotificationPresets.loadDisabledIncome(prefs)
                        notificationWhitelist = com.example.service.NotificationPresets.mergeIncome(
                            loadNotificationWhitelist(prefs), disabledIncomePresets
                        )
                    },
                    onAddExpenseWhitelistedApp = { pkg, prefix ->
                        val t = pkg.trim()
                        val p = prefix.trim()
                        if (t.isNotEmpty() && notificationExpenseWhitelist.none { it.packageName == t && it.prefix == p }) {
                            notificationExpenseWhitelist = notificationExpenseWhitelist + com.example.service.WhitelistedApp(packageName = t, prefix = p)
                            saveNotificationWhitelistExpense(prefs, notificationExpenseWhitelist)
                        }
                    },
                    onRemoveExpenseWhitelistedApp = { pkgAndPrefix ->
                        val parts = pkgAndPrefix.split("|", limit = 2)
                        val pkg = parts.getOrNull(0) ?: pkgAndPrefix
                        val pref = parts.getOrNull(1) ?: ""
                        val entry = com.example.service.WhitelistedApp(packageName = pkg, prefix = pref)
                        if (com.example.service.NotificationPresets.isExpensePermanent(entry)) return@SettingsScreen
                        notificationExpenseWhitelist = notificationExpenseWhitelist.filterNot { it.packageName == pkg && it.prefix == pref }
                        saveNotificationWhitelistExpense(prefs, notificationExpenseWhitelist)
                    },
                    onToggleExpensePreset = { entry, enabled ->
                        com.example.service.NotificationPresets.setDisabledExpense(prefs, entry, !enabled)
                        disabledExpensePresets = com.example.service.NotificationPresets.loadDisabledExpense(prefs)
                        notificationExpenseWhitelist = com.example.service.NotificationPresets.mergeExpense(
                            loadNotificationWhitelistExpense(prefs), disabledExpensePresets
                        )
                    },
                    onRequestNotificationPermission = {
                        try {
                            context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {
                            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
                    },
                    onAddRule = { _, _ -> },
                    onRemoveRule = {},
                    onBack = {
                        showCapture = false
                        showSavedSlips = true
                        showAnalytics = false
                    },
                    onNavigateToCapture = {
                        showCapture = true
                        showSavedSlips = false
                        showAnalytics = false
                    }
                )
            }
        }
    }
}

private fun isListenerRunning(context: Context): Boolean {
    return runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        am.getRunningServices(Int.MAX_VALUE).any { it.service.className == BackgroundListenerService::class.java.name }
    }.getOrDefault(false)
}

private const val PREFS_SLIPS = "saved_slips"
private const val PREFS_SEEN = "seen_payloads"
private const val PREFS_FOLDERS = "tracked_folders"
private const val PREFS_KNOWN_NAMES = "known_names"
private const val PREFS_PROCESSED_FILES = "processed_files"

private fun loadProcessedFiles(prefs: SharedPreferences): Set<String> {
    val raw = prefs.getString(PREFS_PROCESSED_FILES, null) ?: return emptySet()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapTo(mutableSetOf()) { arr.getString(it) }
    }.getOrDefault(emptySet())
}

private fun saveProcessedFiles(prefs: SharedPreferences, files: Set<String>) {
    prefs.edit().putString(PREFS_PROCESSED_FILES, JSONArray(files.toList()).toString()).apply()
}

private fun loadTrackedFolders(prefs: SharedPreferences): List<String> {
    val raw = prefs.getString(PREFS_FOLDERS, null) ?: return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())
}

private fun saveTrackedFolders(prefs: SharedPreferences, folders: List<String>) {
    val arr = JSONArray(folders)
    prefs.edit().putString(PREFS_FOLDERS, arr.toString()).apply()
}

private fun loadKnownNames(prefs: SharedPreferences): List<String> {
    val raw = prefs.getString(PREFS_KNOWN_NAMES, null) ?: return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())
}

private fun saveKnownNames(prefs: SharedPreferences, names: List<String>) {
    val arr = JSONArray(names)
    prefs.edit().putString(PREFS_KNOWN_NAMES, arr.toString()).apply()
}

private const val PREFS_NOTIFICATION_WHITELIST = "notification_whitelist"
private const val PREFS_NOTIFICATION_WHITELIST_EXPENSE = "notification_whitelist_expense"

private fun loadNotificationWhitelist(prefs: SharedPreferences): List<com.example.service.WhitelistedApp> {
    val raw = prefs.getString(PREFS_NOTIFICATION_WHITELIST, null) ?: return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            val el = arr.get(i)
            when (el) {
                is String -> com.example.service.WhitelistedApp(packageName = el, prefix = "")
                is JSONObject -> com.example.service.WhitelistedApp(
                    packageName = el.optString("package"),
                    prefix = el.optString("prefix", "")
                )
                else -> null
            }
        }.filter { it.packageName.isNotBlank() }
    }.getOrDefault(emptyList())
}

private fun saveNotificationWhitelist(prefs: SharedPreferences, list: List<com.example.service.WhitelistedApp>) {
    val arr = JSONArray()
    list.forEach { e ->
        val obj = JSONObject()
        obj.put("package", e.packageName)
        obj.put("prefix", e.prefix)
        arr.put(obj)
    }
    prefs.edit().putString(PREFS_NOTIFICATION_WHITELIST, arr.toString()).apply()
}

private fun loadNotificationWhitelistExpense(prefs: SharedPreferences): List<com.example.service.WhitelistedApp> {
    val raw = prefs.getString(PREFS_NOTIFICATION_WHITELIST_EXPENSE, null) ?: return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            val el = arr.get(i)
            when (el) {
                is String -> com.example.service.WhitelistedApp(packageName = el, prefix = "")
                is JSONObject -> com.example.service.WhitelistedApp(
                    packageName = el.optString("package"),
                    prefix = el.optString("prefix", "")
                )
                else -> null
            }
        }.filter { it.packageName.isNotBlank() }
    }.getOrDefault(emptyList())
}

private fun saveNotificationWhitelistExpense(prefs: SharedPreferences, list: List<com.example.service.WhitelistedApp>) {
    val arr = JSONArray()
    list.forEach { e ->
        val obj = JSONObject()
        obj.put("package", e.packageName)
        obj.put("prefix", e.prefix)
        arr.put(obj)
    }
    prefs.edit().putString(PREFS_NOTIFICATION_WHITELIST_EXPENSE, arr.toString()).apply()
}

private fun loadSlips(prefs: SharedPreferences): List<SavedSlip> {
    val raw = prefs.getString(PREFS_SLIPS, null) ?: return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            slipFromJson(arr.getJSONObject(i))
        }
    }.getOrDefault(emptyList())
}

private fun saveSlips(prefs: SharedPreferences, slips: List<SavedSlip>) {
    val arr = JSONArray()
    slips.forEach { slip -> arr.put(slipToJson(slip)) }
    prefs.edit().putString(PREFS_SLIPS, arr.toString()).apply()
}

private fun loadSeenPayloads(prefs: SharedPreferences): Set<String> {
    val raw = prefs.getString(PREFS_SEEN, null) ?: return emptySet()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapTo(mutableSetOf()) { arr.getString(it) }
    }.getOrDefault(emptySet())
}

private fun saveSeenPayloads(prefs: SharedPreferences, seen: Set<String>) {
    val arr = JSONArray(seen.toList())
    prefs.edit().putString(PREFS_SEEN, arr.toString()).apply()
}

private fun slipToJson(slip: SavedSlip): JSONObject {
    val obj = JSONObject()
    obj.put("payload", slip.payload)
    slip.amount?.let { obj.put("amount", it) }
    slip.transRef?.let { obj.put("transRef", it) }
    slip.senderName?.let { obj.put("senderName", it) }
    slip.receiverName?.let { obj.put("receiverName", it) }
    slip.date?.let { obj.put("date", it) }
    slip.time?.let { obj.put("time", it) }
    obj.put("verificationStatus", slip.verificationStatus.name)
    slip.photoPath?.let { obj.put("photoPath", it) }
    obj.put("isMoneyIn", slip.isMoneyIn)
    obj.put("savedAt", slip.savedAt)
    slip.slipData?.let { obj.put("slipData", responseToJson(it)) }
    return obj
}

private fun slipFromJson(obj: JSONObject): SavedSlip? {
    return runCatching {
        SavedSlip(
            payload = obj.optString("payload"),
            amount = if (obj.has("amount")) obj.optDouble("amount") else null,
            transRef = obj.optString("transRef").ifEmpty { null },
            senderName = obj.optString("senderName").ifEmpty { null },
            receiverName = obj.optString("receiverName").ifEmpty { null },
            date = obj.optString("date").ifEmpty { null },
            time = obj.optString("time").ifEmpty { null },
            verificationStatus = parseStatus(obj.optString("verificationStatus")),
            slipData = if (obj.has("slipData")) responseFromJson(obj.getJSONObject("slipData")) else null,
            isMoneyIn = obj.optBoolean("isMoneyIn", false),
            savedAt = obj.optLong("savedAt", System.currentTimeMillis()),
            photoPath = obj.optString("photoPath").ifEmpty { null }
        )
    }.getOrNull()
}

private fun responseToJson(r: VerifySlipResponse): JSONObject {
    val obj = JSONObject()
    obj.put("success", r.success)
    obj.put("isDuplicate", r.isDuplicate)
    obj.put("isAmountMatched", r.isAmountMatched)
    r.transRef?.let { obj.put("transRef", it) }
    r.sendingBank?.let { obj.put("sendingBank", it) }
    r.sendingBankName?.let { obj.put("sendingBankName", it) }
    r.receivingBank?.let { obj.put("receivingBank", it) }
    r.receivingBankName?.let { obj.put("receivingBankName", it) }
    r.receiverName?.let { obj.put("receiverName", it) }
    r.senderName?.let { obj.put("senderName", it) }
    r.amount?.let { obj.put("amount", it) }
    r.transDate?.let { obj.put("transDate", it) }
    r.transTime?.let { obj.put("transTime", it) }
    r.errorCode?.let { obj.put("errorCode", it) }
    r.errorMessage?.let { obj.put("errorMessage", it) }
    obj.put("verificationStatus", r.verificationStatus.name)
    return obj
}

private fun responseFromJson(obj: JSONObject): VerifySlipResponse {
    return VerifySlipResponse(
        success = obj.optBoolean("success", false),
        isDuplicate = obj.optBoolean("isDuplicate", false),
        isAmountMatched = obj.optBoolean("isAmountMatched", true),
        transRef = obj.optString("transRef").ifEmpty { null },
        sendingBank = obj.optString("sendingBank").ifEmpty { null },
        sendingBankName = obj.optString("sendingBankName").ifEmpty { null },
        receivingBank = obj.optString("receivingBank").ifEmpty { null },
        receivingBankName = obj.optString("receivingBankName").ifEmpty { null },
        receiverName = obj.optString("receiverName").ifEmpty { null },
        senderName = obj.optString("senderName").ifEmpty { null },
        amount = if (obj.has("amount")) obj.optDouble("amount") else null,
        transDate = obj.optString("transDate").ifEmpty { null },
        transTime = obj.optString("transTime").ifEmpty { null },
        errorCode = obj.optString("errorCode").ifEmpty { null },
        errorMessage = obj.optString("errorMessage").ifEmpty { null },
        verificationStatus = parseStatus(obj.optString("verificationStatus"))
    )
}

private fun parseStatus(name: String): VerificationStatus {
    return runCatching { VerificationStatus.valueOf(name) }.getOrDefault(VerificationStatus.UNVERIFIED)
}

private fun providerKeyPref(p: VerificationProvider): String = when (p) {
    VerificationProvider.EASYSLIP -> "api_key_easyslip"
    VerificationProvider.THUNDER -> "api_key_thunder"
    VerificationProvider.SLIP2GO -> "api_key_slip2go"
}