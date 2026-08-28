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
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.documentfile.provider.DocumentFile
import com.example.data.ocr.OcrProcessor
import com.example.data.easyslip.EasySlipClient
import com.example.data.easyslip.VerifySlipResponse
import com.example.data.model.SavedSlip
import com.example.data.model.VerificationStatus
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
    var isLoading by remember { mutableStateOf(false) }
    var isBackgroundSyncing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val easySlipClient = remember { EasySlipClient() }
    val prefs = remember { context.getSharedPreferences("firecash_settings", Context.MODE_PRIVATE) }

    val savedSlips = remember {
        mutableStateListOf<SavedSlip>().apply { addAll(loadSlips(prefs)) }
    }

    var easySlipEnabled by remember {
        mutableStateOf(prefs.getBoolean("easy_slip_enabled", false))
    }
    var apiKey by remember {
        mutableStateOf(prefs.getString("api_key", "") ?: "")
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
    var notificationWhitelist by remember {
        mutableStateOf(loadNotificationWhitelist(prefs))
    }
    var notificationExpenseWhitelist by remember {
        mutableStateOf(loadNotificationWhitelistExpense(prefs))
    }
    val seenPayloads = remember {
        mutableSetOf<String>().apply { addAll(loadSeenPayloads(prefs)) }
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
        if (!easySlipEnabled || apiKey.isBlank()) {
            slipWarning = if (easySlipEnabled) {
                "No API key set — add your EasySlip API key in Settings to verify this slip."
            } else {
                "EasySlip verification is disabled — enable it in Settings and add your API key to verify slips."
            }
            return null
        }
        slipWarning = ""
        easySlipClient.updateConfig("", apiKey)
        return easySlipClient.verifyPayload(payload, checkDuplicate = checkDuplicates)
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

    suspend fun addSlip(payload: String, isMoneyIn: Boolean = false) {
        val verified = runCatching { verifyWithEasySlip(payload) }.getOrNull()
        // Fallback when EasySlip disabled/offline: keep amount from raw payload so details page still shows data
        val fallbackAmount = extractAmount(payload)
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
            date = result.transDate,
            time = result.transTime,
            verificationStatus = result.verificationStatus,
            slipData = result,
            isMoneyIn = resolvedIsMoneyIn
        )

        // Dedupe: re-scanning the same slip updates the existing entry instead of adding a log
        val existingIndex = savedSlips.indexOfFirst { saved ->
            if (result?.transRef?.isNotBlank() == true) {
                saved.transRef == result.transRef
            } else {
                saved.payload == payload
            }
        }
        if (existingIndex >= 0) {
            savedSlips[existingIndex] = slip
        } else {
            savedSlips.add(slip)
        }

        seenPayloads.add(payload)
        saveSlips(prefs, savedSlips)
        saveSeenPayloads(prefs, seenPayloads)
    }

    fun handlePayload(payload: String) {
        if (payload.isBlank()) return
        qrPayload = payload
        isLoading = true
        scope.launch {
            addSlip(payload)
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

    fun importSlips(paths: List<String>) {
        if (paths.isEmpty()) return
        isLoading = true
        scope.launch {
            for (path in paths) {
                val payload = OcrProcessor(context).processReceipt(path, scanCenterOnly = false).rawText
                if (payload.isNotBlank()) {
                    addSlip(payload)
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

            val payload = OcrProcessor(context)
                .processReceipt(tempFile.absolutePath, scanCenterOnly = false)
                .rawText
            if (payload.isNotBlank() && payload !in seenPayloads) {
                seenPayloads.add(payload)
                addSlip(payload)
            }
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
        scope.launch {
            try {
                for (uriStr in trackedFolderUris) {
                    scanFolder(uriStr)
                }
            } finally {
                isBackgroundSyncing = false
            }
        }
    }

    fun resyncUnverifiedSlips() {
        if (!easySlipEnabled || apiKey.isBlank()) return
        if (isLoading || isBackgroundSyncing) return
        val unverified = savedSlips.filter { it.verificationStatus == VerificationStatus.UNVERIFIED }
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
        // Only allow delete for unknown/invalid slips (safety)
        val isDeletable = slip.amount == null || slip.verificationStatus == VerificationStatus.UNVERIFIED
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
                        val payload = OcrProcessor(context).processReceipt(path).rawText
                        handlePayload(payload)
                    }
                },
                onFileSelected = { /* unused – picker handled inside PhotoCaptureScreen */ },
                onImageSelected = { path ->
                    scope.launch {
                        val payload = OcrProcessor(context).processReceipt(path, scanCenterOnly = false).rawText
                        handlePayload(payload)
                    }
                },
                onQrDetected = { payload -> handlePayload(payload) },
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
                onAutoSync = { syncTrackedFolderInBackground() }
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
    currentCurrency = "THB",
    rules = emptyList(),
    googleDriveSync = false,
    easySlipEnabled = easySlipEnabled,
    apiKey = apiKey,
    checkDuplicates = checkDuplicates,
    knownNames = knownNames,
    unverifiedCount = savedSlips.count { it.verificationStatus == VerificationStatus.UNVERIFIED },
    onCurrencyChange = {},
    onToggleDriveSync = {},
    onToggleEasySlip = { enabled ->
        easySlipEnabled = enabled
        prefs.edit().putBoolean("easy_slip_enabled", enabled).apply()
        if (enabled && apiKey.isNotBlank()) resyncUnverifiedSlips()
    },
    onUpdateApiKey = { key ->
        apiKey = key
        prefs.edit().putString("api_key", key).apply()
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
                    notificationIncomeEnabled = notificationIncomeEnabled,
                    notificationExpenseEnabled = notificationExpenseEnabled,
                    notificationWhitelist = notificationWhitelist,
                    notificationExpenseWhitelist = notificationExpenseWhitelist,
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
                        notificationWhitelist = notificationWhitelist.filterNot { it.packageName == pkg && it.prefix == pref }
                        saveNotificationWhitelist(prefs, notificationWhitelist)
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
                        notificationExpenseWhitelist = notificationExpenseWhitelist.filterNot { it.packageName == pkg && it.prefix == pref }
                        saveNotificationWhitelistExpense(prefs, notificationExpenseWhitelist)
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
                    onNavigateToBackup = {},
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

private const val PREFS_SLIPS = "saved_slips"
private const val PREFS_SEEN = "seen_payloads"
private const val PREFS_FOLDERS = "tracked_folders"
private const val PREFS_KNOWN_NAMES = "known_names"

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
            savedAt = obj.optLong("savedAt", System.currentTimeMillis())
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