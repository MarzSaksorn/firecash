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
    var slipMismatch by remember { mutableStateOf(false) }
    var slipDateMismatch by remember { mutableStateOf(false) }
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

    // Prefer a slip amount marked with a baht/THB/B symbol over the generic parser result,
    // which can grab exchange-rate fragments like "(¥1= THB 4.9793541)".
    fun extractSlipAmount(text: String): Double? {
        val patterns = listOf(
            Regex("""\bB\s*([\d,]+\.\d{2})\b"""),
            Regex("""฿\s*([\d,]+(?:\.\d{2})?)"""),
            Regex("""(?:THB|บาท)\s*([\d,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""([\d,]+(?:\.\d{2})?)\s*(?:THB|บาท)""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            p.find(text)?.let { m ->
                m.groupValues[1].replace(",", "").toDoubleOrNull()?.let { if (it > 0.0) return it }
            }
        }
        return null
    }

    suspend fun addSlip(
        payload: String,
        isMoneyIn: Boolean = false,
        photoPath: String? = null,
        ocrText: String? = null
    ) {
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
        // Fraud cross-check (shop mode): the amount printed on the slip photo must match the
        // QR payload amount and/or the bank-verified amount. A mismatch means a doctored slip.
        val textAmount = if (ocrText.isNullOrBlank()) null else extractSlipAmount(ocrText)
        val qrAmount = SlipDataParser.extractQrAmount(payload)
        val verifiedAmount = verified?.amount
        val candidates = listOfNotNull(textAmount, qrAmount, verifiedAmount)
        val mismatch = candidates.size >= 2 && candidates.max() - candidates.min() > 0.005
        slipMismatch = mismatch
        if (mismatch) {
            slipWarning = "Amount mismatch — photo text shows $textAmount but QR/bank shows ${qrAmount ?: verifiedAmount}. Possible tampered slip!"
        }
        // Date cross-check: the date printed on the slip photo must match the bank-verified date
        val textDate = if (ocrText.isNullOrBlank()) null else SlipDataParser.extractSlipDate(ocrText)
        val bankDate = verified?.transDate
        val dateMismatch = textDate != null && bankDate != null && textDate != bankDate
        slipDateMismatch = dateMismatch
        if (dateMismatch) {
            slipWarning = if (mismatch) "$slipWarning | Date mismatch — photo shows $textDate but bank shows $bankDate."
                else "Date mismatch — slip photo shows $textDate but bank shows $bankDate. Possible tampered slip!"
        }
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
            photoPath = photoPath,
            amountMismatch = mismatch,
            dateMismatch = dateMismatch
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

            // Restore the single dataset ("slips" / "seenPayloads" / "processedFiles")
            root.optJSONArray("slips")?.let { arr ->
                val list = mutableListOf<SavedSlip>()
                for (i in 0 until arr.length()) {
                    slipFromJson(arr.getJSONObject(i))?.let { list.add(it) }
                }
                savedSlips.clear()
                savedSlips.addAll(list)
                saveSlips(prefs, savedSlips)
            }
            val seen = root.optJSONArray("seenPayloads")
                ?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() } ?: emptySet()
            seenPayloads.clear(); seenPayloads.addAll(seen); saveSeenPayloads(prefs, seenPayloads)
            val processed = root.optJSONArray("processedFiles")
                ?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() } ?: emptySet()
            processedFiles.clear(); processedFiles.addAll(processed); saveProcessedFiles(prefs, processedFiles)

            fun readStrArr(name: String): List<String> =
                root.optJSONArray(name)?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()

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
                val processor = OcrProcessor(context)
                // The crop becomes the stored slip photo; the imported full-frame copy is
                // deleted once a crop was produced (the crop carries the slip content).
                val workPath = processor.flattenedCopy(path) ?: path
                val ocrText = processor.recognizeText(workPath, scanCenterOnly = false)
                val payload = processor.processReceipt(workPath, scanCenterOnly = false).rawText
                if (payload.isNotBlank()) {
                    addSlip(payload, photoPath = workPath, ocrText = ocrText)
                    if (workPath != path) {
                        runCatching { File(path).delete() }
                            .onFailure { android.util.Log.w("FireCashOCR", "could not delete imported frame $path: ${it.message}") }
                    }
                } else if (workPath != path) {
                    // Nothing worth logging — keep the full frame, drop the useless crop
                    runCatching { File(workPath).delete() }
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

            val processor = OcrProcessor(context)
            // Deterministic crop name from the tracked photo itself, so re-scanning the same
            // folder overwrites one crop file instead of piling up copies. The URI hash keeps
            // same-named photos in different folders from colliding on one crop file.
            val cropName = (file.name?.substringBeforeLast('.') ?: "slip")
                .replace(Regex("[^A-Za-z0-9._-]"), "_") +
                "_${Integer.toHexString(fileKey.hashCode()).take(8)}"
            val flatPath = processor.flattenedCopy(tempFile.absolutePath, outName = cropName)
            val workPath = flatPath ?: tempFile.absolutePath
            val ocrText = processor.recognizeText(workPath, scanCenterOnly = false)
            val payload = processor
                .processReceipt(workPath, scanCenterOnly = false)
                .rawText
            if (payload.isNotBlank() && payload !in seenPayloads) {
                seenPayloads.add(payload)
                // Prefer the persisted crop (it is what OCR/QR ran on); fall back to the
                // content:// uri of the original photo when no slip region was found.
                addSlip(payload, photoPath = flatPath ?: file.uri.toString(), ocrText = ocrText)
            } else if (payload.isBlank() && flatPath != null) {
                // Nothing worth logging — keep the user's photo in its folder, drop the crop
                runCatching { File(flatPath).delete() }
            } else if (flatPath != null) {
                // Payload already known — a re-scan upgraded the stored photo to the crop
                // (deterministic name ⇒ the same file each time, so nothing piles up).
                val idx = savedSlips.indexOfFirst { it.payload == payload }
                if (idx >= 0 && savedSlips[idx].photoPath != flatPath) {
                    savedSlips[idx] = savedSlips[idx].copy(photoPath = flatPath)
                    saveSlips(prefs, savedSlips)
                }
            }
            // Mark processed (even blank OCR) so future opens only handle genuinely new files
            processedFiles.add(fileKey)
            saveProcessedFiles(prefs, processedFiles)
            runCatching { tempFile.delete() }
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

    // Forced sync: clear the processed-file cache so every photo in the tracked folders
    // is re-read (text or QR) and re-added/updated, even ones already scanned before.
    fun forceSyncTrackedFolders() {
        if (trackedFolderUris.isEmpty()) return
        if (isLoading || isBackgroundSyncing) return
        processedFiles.clear()
        saveProcessedFiles(prefs, processedFiles)
        syncTrackedFolder()
    }

    // Verify a batch of slips. Guards against sandbox/test API keys that return the SAME
    // transRef (and amount) for every payload: if two or more different slips come back with
    // one shared transRef, the responses are canned and nothing is applied, so real amounts
    // extracted from slip photos are never overwritten with fake 178.00-style data.
    suspend fun verifyBatch(old: List<SavedSlip>): List<Pair<SavedSlip, VerifySlipResponse>> {
        val results = old.mapNotNull { s ->
            runCatching { verifyWithEasySlip(s.payload) }.getOrNull()?.let { s to it }
        }
        val ok = results.filter { it.second.success && it.second.transRef != null }
        val canned = ok.size >= 2 && ok.map { it.second.transRef }.distinct().size == 1
        if (canned) {
            android.util.Log.w("FireCashOCR", "verification returned one shared transRef for ${ok.size} slips — canned/test response, skipping")
            return emptyList()
        }
        return results
    }

    // Apply a verified response to a slip. The slip's own amount (from the photo / OCR or the
    // QR) is never overwritten — a sandbox/test API key returns arbitrary amounts (e.g. 178.00
    // or 0.00), so a verified amount only fills a missing one, and any disagreement with the
    // stored amount is surfaced as the amountMismatch fraud flag instead.
    fun applyVerifiedUpdate(old: SavedSlip, result: VerifySlipResponse): SavedSlip {
        val resolvedIsMoneyIn = when {
            isKnownName(result.senderName, knownNames) && isKnownName(result.receiverName, knownNames) -> false
            isKnownName(result.receiverName, knownNames) -> true
            isKnownName(result.senderName, knownNames) -> false
            else -> old.isMoneyIn
        }
        val verifiedAmount = result.amount
        val amount = old.amount ?: verifiedAmount
        val mismatch = old.amount != null && verifiedAmount != null && kotlin.math.abs(old.amount - verifiedAmount) > 0.005
        val dateMismatch = old.date != null && result.transDate != null && old.date != result.transDate
        return old.copy(
            amount = amount,
            transRef = result.transRef ?: old.transRef,
            senderName = result.senderName ?: old.senderName,
            receiverName = result.receiverName ?: old.receiverName,
            date = result.transDate ?: old.date,
            time = result.transTime ?: old.time,
            verificationStatus = result.verificationStatus,
            slipData = result.copy(amount = amount),
            isMoneyIn = resolvedIsMoneyIn,
            amountMismatch = old.amountMismatch || mismatch,
            dateMismatch = old.dateMismatch || dateMismatch
        )
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
                for ((old, result) in verifyBatch(verifyable.toList())) {
                    val idx = savedSlips.indexOfFirst { it.payload == old.payload }
                    if (idx >= 0) savedSlips[idx] = applyVerifiedUpdate(old, result)
                }
                saveSlips(prefs, savedSlips)
            } finally {
                isLoading = false
            }
        }
    }

    fun resyncUnverifiedSlips() {
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
                for ((old, result) in verifyBatch(unverified.toList())) {
                    val updated = applyVerifiedUpdate(old, result)
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
                amountMismatch = slipMismatch,
                dateMismatch = slipDateMismatch,
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
                        // Flatten the slip (document detection + perspective warp) so the
                        // QR + OCR read the flat document. The CROP becomes the stored slip
                        // photo; the full camera frame is dropped once the slip is saved.
                        isLoading = true
                        val processor = OcrProcessor(context)
                        val workPath = processor.flattenedCopy(path) ?: path
                        val ocrText = processor.recognizeText(workPath, scanCenterOnly = false)
                        val payload = processor.processReceipt(workPath, scanCenterOnly = false).rawText
                        if (payload.isNotBlank()) {
                            qrPayload = payload
                            qrPhotoPath = workPath
                            addSlip(payload, photoPath = workPath, ocrText = ocrText)
                            if (workPath != path) {
                                runCatching { File(path).delete() }
                                    .onFailure { android.util.Log.w("FireCashOCR", "could not delete full frame $path: ${it.message}") }
                            }
                            showCapture = false
                            showPayload = true
                        } else if (workPath != path) {
                            // Nothing worth logging — keep the full frame, drop the useless crop
                            runCatching { File(workPath).delete() }
                        }
                        isLoading = false
                    }
                },
                onFileSelected = { /* unused – picker handled inside PhotoCaptureScreen */ },
                onImageSelected = { path ->
                    scope.launch {
                        // Flatten the slip like a camera shot: the CROP becomes the stored
                        // photo; the picked full frame is dropped once the slip is saved.
                        isLoading = true
                        val processor = OcrProcessor(context)
                        val workPath = processor.flattenedCopy(path) ?: path
                        val ocrText = processor.recognizeText(workPath, scanCenterOnly = false)
                        val payload = processor.processReceipt(workPath, scanCenterOnly = false).rawText
                        if (payload.isNotBlank()) {
                            qrPayload = payload
                            qrPhotoPath = workPath
                            addSlip(payload, photoPath = workPath, ocrText = ocrText)
                            if (workPath != path) {
                                runCatching { File(path).delete() }
                                    .onFailure { android.util.Log.w("FireCashOCR", "could not delete picked frame $path: ${it.message}") }
                            }
                            showCapture = false
                            showPayload = true
                        } else if (workPath != path) {
                            // Nothing worth logging — keep the full frame, drop the useless crop
                            runCatching { File(workPath).delete() }
                        }
                        isLoading = false
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
                    slipMismatch = slip.amountMismatch
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
                    onForceSyncAll = { forceSyncTrackedFolders() },
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
    obj.put("amountMismatch", slip.amountMismatch)
    obj.put("dateMismatch", slip.dateMismatch)
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
            photoPath = obj.optString("photoPath").ifEmpty { null },
            amountMismatch = obj.optBoolean("amountMismatch", false),
            dateMismatch = obj.optBoolean("dateMismatch", false)
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