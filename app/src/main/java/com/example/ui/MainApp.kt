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
import com.example.ui.screens.AccountSettingsScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.components.CaptureBottomBar
import com.example.ui.theme.FireCashBackground

@Composable
fun MainApp(modifier: Modifier = Modifier) {
    var showCapture by remember { mutableStateOf(true) }
    var showPayload by remember { mutableStateOf(false) }
    var showSavedSlips by remember { mutableStateOf(false) }
    var showAccountSettings by remember { mutableStateOf(false) }
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
    val seenPayloads = remember {
        mutableSetOf<String>().apply { addAll(loadSeenPayloads(prefs)) }
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

    suspend fun addSlip(payload: String, isMoneyIn: Boolean = false) {
        val result = runCatching { verifyWithEasySlip(payload) }.getOrNull()
        slipData = result
        // Auto-resolve isMoneyIn based on known names:
        // - if both sender & receiver are known -> transfer (neutral, stored as false, UI shows Transfer)
        // - if receiver is known -> income
        // - if sender is known -> expense
        // - else fallback to requested isMoneyIn (manual toggle / default)
        val senderKnown = isKnownName(result?.senderName, knownNames)
        val receiverKnown = isKnownName(result?.receiverName, knownNames)
        val resolvedIsMoneyIn = when {
            senderKnown && receiverKnown -> false // transfer - will be excluded from balance
            receiverKnown -> true
            senderKnown -> false
            else -> isMoneyIn
        }
        val slip = SavedSlip(
            payload = payload,
            amount = result?.amount,
            transRef = result?.transRef,
            senderName = result?.senderName,
            receiverName = result?.receiverName,
            date = result?.transDate,
            time = result?.transTime,
            verificationStatus = result?.verificationStatus ?: VerificationStatus.UNVERIFIED,
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
            showCapture = true
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

    Scaffold(
        containerColor = FireCashBackground,
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!showPayload && !showAccountSettings) {
                CaptureBottomBar(
                    showSettings = !showCapture && !showSavedSlips,
                    showSavedSlips = showSavedSlips,
                    onSettingsClick = {
                        showCapture = false
                        showSavedSlips = false
                    },
                    onCaptureClick = {
                        showCapture = true
                        showSavedSlips = false
                    },
                    onSavedSlipsClick = {
                        showCapture = false
                        showSavedSlips = true
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {
        if (showPayload) {
            QrPayloadScreen(
                payload = qrPayload,
                slipData = slipData,
                warning = slipWarning,
                onBack = {
                    showPayload = false
                    showCapture = true
                },
                onSave = {
                    savePayload()
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
                },
                payloadText = qrPayload,
                modifier = Modifier.fillMaxSize()
            )
        } else if (showAccountSettings) {
            AccountSettingsScreen(
                isLoading = isLoading,
                trackedFolders = trackedFolderUris,
                onBack = {
                    showAccountSettings = false
                    showSavedSlips = true
                },
                onFolderSelected = { uri -> onFolderSelected(uri) },
                onRemoveFolder = { uriStr -> onRemoveFolder(uriStr) },
                onSyncNow = { syncTrackedFolder() },
                onImportSlips = { paths -> importSlips(paths) }
            )
        } else if (showSavedSlips) {
            AccountScreen(
                slips = savedSlips,
                knownNames = knownNames,
                isLoading = isLoading,
                isBackgroundSyncing = isBackgroundSyncing,
                onBack = {
                    showSavedSlips = false
                    showCapture = true
                },
                onSlipClick = { slip ->
                    qrPayload = slip.payload
                    slipData = slip.slipData
                    slipWarning = ""
                    showSavedSlips = false
                    showPayload = true
                },
                onOpenSettings = {
                    showSavedSlips = false
                    showAccountSettings = true
                },
                onOpenAnalytics = {
                    showSavedSlips = false
                    showAnalytics = true
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
    onCurrencyChange = {},
    onToggleDriveSync = {},
    onToggleEasySlip = { enabled ->
        easySlipEnabled = enabled
        prefs.edit().putBoolean("easy_slip_enabled", enabled).apply()
    },
    onUpdateApiKey = { key ->
        apiKey = key
        prefs.edit().putString("api_key", key).apply()
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
                    onAddRule = { _, _ -> },
                    onRemoveRule = {},
                    onNavigateToBackup = {},
                    onBack = {
                        showCapture = true
                        showSavedSlips = false
                    },
                    onNavigateToCapture = {
                        showCapture = true
                        showSavedSlips = false
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