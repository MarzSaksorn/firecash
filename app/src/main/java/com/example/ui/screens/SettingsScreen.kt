package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.KeywordRule
import java.io.File
import java.io.FileOutputStream
import com.example.ui.StringKeys
import com.example.ui.Translations
import com.example.ui.components.FireCashTopBar
import com.example.ui.theme.FireCashBackground
import com.example.ui.theme.FireCashError
import com.example.ui.theme.FireCashOnBackground
import com.example.ui.theme.FireCashOnPrimary
import com.example.ui.theme.FireCashOnSurface
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashOutline
import com.example.ui.theme.FireCashOutlineVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashPrimaryContainer
import com.example.ui.theme.FireCashSecondary
import com.example.ui.theme.FireCashSurfaceContainer
import com.example.ui.theme.FireCashSurfaceContainerHigh
import com.example.ui.theme.FireCashSurfaceContainerHighest
import com.example.ui.theme.FireCashSurfaceContainerLow
import com.example.ui.theme.FireCashSurfaceDim
import com.example.ui.theme.FireCashSurfaceVariant

@Composable
fun SettingsScreen(
    rules: List<KeywordRule>,
    easySlipEnabled: Boolean,
    apiKey: String,
        verificationProvider: com.example.data.verification.VerificationProvider = com.example.data.verification.VerificationProvider.EASYSLIP,
    checkDuplicates: Boolean,
    knownNames: List<String> = emptyList(),
    unverifiedCount: Int = 0,
    onLanguageChange: (String) -> Unit = {},
        currentLang: String = "en",
        onThemeChange: (Boolean) -> Unit = {},
        isDarkTheme: Boolean = true,
        notificationIncomeEnabled: Boolean = false,
    notificationExpenseEnabled: Boolean = false,
    notificationWhitelist: List<com.example.service.WhitelistedApp> = emptyList(),
    notificationExpenseWhitelist: List<com.example.service.WhitelistedApp> = emptyList(),
    permanentIncomeApps: List<com.example.service.WhitelistedApp> = emptyList(),
    permanentExpenseApps: List<com.example.service.WhitelistedApp> = emptyList(),
    disabledIncomePresets: Set<String> = emptySet(),
    disabledExpensePresets: Set<String> = emptySet(),
    onToggleIncomePreset: (com.example.service.WhitelistedApp, Boolean) -> Unit = { _, _ -> },
    onToggleExpensePreset: (com.example.service.WhitelistedApp, Boolean) -> Unit = { _, _ -> },
    notificationAccessGranted: Boolean = false,
    batteryOptIgnored: Boolean = false,
    onRequestDisableBatteryOptimization: () -> Unit = {},
    backgroundListening: Boolean = false,
    onToggleBackgroundListening: (Boolean) -> Unit = {},
    isLoading: Boolean = false,
    trackedFolders: List<String> = emptyList(),
    onToggleEasySlip: (Boolean) -> Unit,
    onProviderChange: (com.example.data.verification.VerificationProvider) -> Unit = {},
    onUpdateApiKey: (String) -> Unit,
    onToggleCheckDuplicates: (Boolean) -> Unit,
    onAddKnownName: (String) -> Unit = {},
    onRemoveKnownName: (String) -> Unit = {},
    onSyncUnverified: () -> Unit = {},
    onToggleNotificationIncome: (Boolean) -> Unit = {},
    onToggleNotificationExpense: (Boolean) -> Unit = {},
    onAddWhitelistedApp: (String, String) -> Unit = { _, _ -> },
    onRemoveWhitelistedApp: (String) -> Unit = {},
    onAddExpenseWhitelistedApp: (String, String) -> Unit = { _, _ -> },
    onRemoveExpenseWhitelistedApp: (String) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onFolderSelected: (Uri) -> Unit = {},
    onRemoveFolder: (String) -> Unit = {},
    onSyncNow: () -> Unit = {},
    onForceSyncAll: () -> Unit = {},
    onImportSlips: (List<String>) -> Unit = {},
    onExportData: () -> Unit = {},
    onImportData: (String) -> Unit = {},
    onAddRule: (keyword: String, category: String) -> Unit,
    onRemoveRule: (KeywordRule) -> Unit,
    onBack: () -> Unit, onNavigateToCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dangerousExpanded by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var newKeyword by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Travel") }

    var apiKeyText by remember { mutableStateOf(apiKey) }
    androidx.compose.runtime.LaunchedEffect(apiKey) { apiKeyText = apiKey }

    // Display lists: ALL presets (enabled or disabled, so toggles stay visible) + user entries
    val displayIncomeWhitelist =
        (notificationWhitelist + permanentIncomeApps).distinctBy { it.packageName to it.prefix }
    val displayExpenseWhitelist =
        (notificationExpenseWhitelist + permanentExpenseApps).distinctBy { it.packageName to it.prefix }
    var newKnownName by remember { mutableStateOf("") }
    var newWhitelistApp by remember { mutableStateOf("") }
    var newWhitelistPrefix by remember { mutableStateOf("โอนเงินให้คุณ ฿") }
    var newExpenseWhitelistApp by remember { mutableStateOf("") }
    var newExpenseWhitelistPrefix by remember { mutableStateOf("โอนเงินสำเร็จ ฿") }

    val context = LocalContext.current
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val tempFile = File(context.cacheDir, "firecash_import_${System.currentTimeMillis()}.json")
            val ok = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
            }.isSuccess
            if (ok) onImportData(tempFile.absolutePath)
        }
    }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) onFolderSelected(uri) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val photoDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            val paths = uris.mapIndexedNotNull { index, uri ->
                val tempFile = File(photoDir, "sync_${System.currentTimeMillis()}_$index.jpg")
                val ok = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                }.isSuccess
                if (ok) tempFile.absolutePath else null
            }
            if (paths.isNotEmpty()) onImportSlips(paths)
        }
    }


    Column(        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(FireCashBackground)
    ) {
        FireCashTopBar(
            title = Translations.t(StringKeys.APP_NAME),
            showBackButton = true,
            onBackClick = onBack,
            onProfileClick = {}
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                            text = Translations.t(StringKeys.SAFE_SETTINGS),
                            color = FireCashSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        // Card: Language
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(FireCashSurfaceContainerLow)
                                                        .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                                        .padding(16.dp)
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.Language,
                                                                contentDescription = null,
                                                                tint = Color(0xFFB3C5FF),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Text(
                                                                text = if (currentLang == "th") "ภาษา" else "Language",
                                                                color = Color.White,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Spacer(modifier = Modifier.weight(1f))
                                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                listOf("en" to "English", "th" to "ภาษาไทย").forEach { (code, label) ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (currentLang == code) FireCashPrimary else FireCashSurfaceContainerHigh)
                                                                            .clickable { onLanguageChange(code) }
                                                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = label,
                                                                            color = if (currentLang == code) Color.White else FireCashOnSurfaceVariant,
                                                                            fontSize = 13.sp
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                // Card: Theme
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(FireCashSurfaceContainerLow)
                                                        .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                                        .padding(16.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.DarkMode,
                                                            contentDescription = null,
                                                            tint = Color(0xFFB3C5FF),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            text = if (currentLang == "th") "ธีม" else "Theme",
                                                            color = Color.White,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            listOf(true to (if (currentLang == "th") "มืด" else "Dark"), false to (if (currentLang == "th") "สว่าง" else "Light")).forEach { (isDark, label) ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(if (isDarkTheme == isDark) FireCashPrimary else FireCashSurfaceContainerHigh)
                                                                        .clickable { onThemeChange(isDark) }
                                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = label,
                                                                        color = if (isDarkTheme == isDark) Color.White else FireCashOnSurfaceVariant,
                                                                        fontSize = 13.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                        // Card: My Names (auto income / transfer detection)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow)
                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(FireCashSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = FireCashPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Translations.t(StringKeys.MY_NAMES),
                                color = FireCashOnSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = Translations.t(StringKeys.MY_NAMES_DESC),
                                color = FireCashOnSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newKnownName,
                            onValueChange = { newKnownName = it },
                            placeholder = { Text(Translations.t(StringKeys.NAME_PLACEHOLDER), fontSize = 13.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = FireCashOnSurface, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FireCashSurfaceContainerHigh,
                                unfocusedContainerColor = FireCashSurfaceContainerHigh,
                                focusedBorderColor = FireCashPrimary,
                                unfocusedBorderColor = FireCashOutlineVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("known_name_input")
                        )
                        Button(
                            onClick = {
                                val t = newKnownName.trim()
                                if (t.isNotEmpty()) {
                                    onAddKnownName(t)
                                    newKnownName = ""
                                }
                            },
                            modifier = Modifier.testTag("add_known_name_button")
                        ) { Text(Translations.t(StringKeys.ADD)) }
                    }
                    if (knownNames.isEmpty()) {
                        Text(
                            text = Translations.t(StringKeys.NO_NAMES_YET),
                            color = FireCashOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            knownNames.forEach { name ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(FireCashSurfaceContainerHighest)
                                        .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        color = FireCashOnSurface,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onRemoveKnownName(name) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = Translations.t(StringKeys.REMOVE),
                                            tint = FireCashOutline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card: Tracked Folders (migrated from AccountSettingsScreen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow)
                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(FireCashSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = FireCashPrimary, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = Translations.t(StringKeys.TRACKED_FOLDERS), color = FireCashOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = Translations.t(StringKeys.AUTO_SCAN), color = FireCashOnSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    if (trackedFolders.isEmpty()) {
                        Text(text = Translations.t(StringKeys.NO_FOLDERS_YET), color = FireCashOnSurfaceVariant, fontSize = 13.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            trackedFolders.forEach { uriStr ->
                                val name = runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uriStr))?.name }.getOrNull()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(FireCashSurfaceContainerHighest, RoundedCornerShape(12.dp))
                                        .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = FireCashPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = name ?: uriStr, color = FireCashOnSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onRemoveFolder(uriStr) }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = Translations.t(StringKeys.REMOVE_FOLDER), tint = FireCashOnSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { folderPickerLauncher.launch(null) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = FireCashPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = Translations.t(StringKeys.ADD_TRACKED_FOLDER), color = FireCashPrimary)
                    }
                    OutlinedButton(onClick = onSyncNow, enabled = trackedFolders.isNotEmpty() && !isLoading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = FireCashPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = Translations.t(StringKeys.SYNC_TRACKED_FOLDERS_NOW), color = FireCashPrimary)
                    }
                    OutlinedButton(onClick = onForceSyncAll, enabled = trackedFolders.isNotEmpty() && !isLoading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = FireCashSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = Translations.t(StringKeys.FORCE_SYNC_ALL_DESC), color = FireCashSecondary)
                    }
                    OutlinedButton(onClick = { photoPickerLauncher.launch(arrayOf("image/*")) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = FireCashPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = Translations.t(StringKeys.IMPORT_SLIP_PHOTOS), color = FireCashPrimary)
                    }
                    Text(text = Translations.t(StringKeys.FOLDER_SCAN_DESC), color = FireCashOnSurfaceVariant, fontSize = 13.sp)
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FireCashPrimary)
                            Text(text = Translations.t(StringKeys.SYNCING_SLIPS), color = FireCashOnSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Card 2: Keyword Mapping Pro
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow)
                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(FireCashSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                tint = FireCashPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = Translations.t(StringKeys.KEYWORD_MAPPING),
                                    color = FireCashOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(FireCashPrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = Translations.t(StringKeys.SMART_TAG),
                                        color = FireCashPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = Translations.t(StringKeys.KEYWORD_MAPPING_DESC),
                                color = FireCashOnSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Rules List
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rules.forEach { rule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(FireCashSurfaceContainerHighest)
                                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(FireCashSurfaceDim)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = rule.keyword,
                                            color = FireCashOnSurface,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                                        contentDescription = null,
                                        tint = FireCashOutline,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Text(
                                        text = rule.category,
                                        color = FireCashSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                IconButton(
                                    onClick = { onRemoveRule(rule) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = Translations.t(StringKeys.REMOVE_RULE),
                                        tint = FireCashOutline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Add Rule Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = FireCashOutline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { showAddRuleDialog = true }
                                .padding(vertical = 10.dp)
                                .testTag("add_rule_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = FireCashPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = Translations.t(StringKeys.ADD_RULE),
                                    color = FireCashPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashError.copy(alpha = 0.10f))
                    .padding(12.dp)
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dangerousExpanded = !dangerousExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (dangerousExpanded) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = FireCashError
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Translations.t(StringKeys.DANGEROUS),
                    color = FireCashError,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (dangerousExpanded) Translations.t(StringKeys.TAP_COLLAPSE) else Translations.t(StringKeys.TAP_EXPAND),
                    color = FireCashOnSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            AnimatedVisibility(
                visible = dangerousExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Slip Verification Configuration
            Text(
                text = Translations.t(StringKeys.BANK_SLIP_VERIFICATION),
                color = FireCashOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow)
                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(FireCashSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = FireCashSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = Translations.t(StringKeys.SLIP_VERIFICATION_TITLE),
                                    color = FireCashOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = Translations.t(StringKeys.VERIFY_PROMPTPAY),
                                    color = FireCashOnSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Switch(
                            checked = easySlipEnabled,
                            onCheckedChange = onToggleEasySlip,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FireCashOnPrimary,
                                checkedTrackColor = FireCashSecondary,
                                uncheckedThumbColor = FireCashOutline,
                                uncheckedTrackColor = FireCashSurfaceVariant
                            )
                        )
                    }

                    if (easySlipEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Provider selector
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                com.example.data.verification.VerificationProvider.entries.forEach { p ->
                                    FilterChip(
                                        selected = verificationProvider == p,
                                        onClick = { onProviderChange(p) },
                                        label = { Text(p.label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = FireCashPrimary,
                                            containerColor = FireCashSurfaceContainerHigh
                                        ),
                                        modifier = Modifier.height(32.dp)
                                    )
                                }
                            }

                            // API Key Field
                            OutlinedTextField(
                                value = apiKeyText,
                                onValueChange = {
                                    apiKeyText = it
                                    onUpdateApiKey(it)
                                },
                                label = { Text(Translations.fmt(StringKeys.API_KEY_LABEL, verificationProvider.label)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = FireCashOnSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                textStyle = TextStyle(
                                    color = FireCashOnSurface,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = FireCashSurfaceContainerHigh,
                                    unfocusedContainerColor = FireCashSurfaceContainerHigh,
                                    focusedBorderColor = FireCashPrimary,
                                    unfocusedBorderColor = FireCashOutlineVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Duplicate Check Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Translations.t(StringKeys.PREVENT_DUPLICATE),
                                    color = FireCashOnSurface,
                                    fontSize = 14.sp
                                )
                                Switch(
                                    checked = checkDuplicates,
                                    onCheckedChange = onToggleCheckDuplicates,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = FireCashOnPrimary,
                                        checkedTrackColor = FireCashPrimary
                                    )
                                )
                            }
                            if (unverifiedCount > 0) {
                                Button(
                                    onClick = onSyncUnverified,
                                    enabled = apiKey.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().testTag("sync_unverified_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = FireCashPrimary)
                                ) {
                                    Text(Translations.fmt(StringKeys.SYNC_UNVERIFIED_COUNT, unverifiedCount), color = FireCashOnPrimary)
                                }
                                if (apiKey.isBlank()) {
                                    Text(
                                        text = Translations.t(StringKeys.ADD_API_KEY_SYNC),
                                        color = FireCashOnSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card: Notification Income Detection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow)
                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(FireCashSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = FireCashSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Translations.t(StringKeys.NOTIFICATION_INCOME),
                                    color = FireCashOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = Translations.t(StringKeys.NOTIFICATION_INCOME_DESC),
                                    color = FireCashOnSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Switch(
                            checked = notificationIncomeEnabled,
                            onCheckedChange = onToggleNotificationIncome,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FireCashOnPrimary,
                                checkedTrackColor = FireCashSecondary,
                                uncheckedThumbColor = FireCashOutline,
                                uncheckedTrackColor = FireCashSurfaceVariant
                            ),
                            modifier = Modifier.testTag("notification_income_switch")
                        )
                    }
                    if (notificationIncomeEnabled) {
                        if (notificationAccessGranted) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = FireCashSecondary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = Translations.t(StringKeys.NOTIFICATION_ACCESS_GRANTED),
                                    color = FireCashSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Button(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = FireCashSurfaceContainerHigh)
                            ) {
                                Text(Translations.t(StringKeys.ENABLE_NOTIFICATION_ACCESS), color = FireCashOnSurface)
                            }
                        }
                        Text(
                            text = Translations.t(StringKeys.NOTIFICATION_INCOME_HELP),
                            color = FireCashOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                        // Whitelist with prefix detection
                        Text(
                            text = Translations.t(StringKeys.WHITELIST_TITLE),
                            color = FireCashOnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = Translations.t(StringKeys.WHITELIST_PREFIX_HELP),
                            color = FireCashOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = newWhitelistApp,
                            onValueChange = { newWhitelistApp = it },
                            placeholder = { Text(Translations.t(StringKeys.APP_PACKAGE_PLACEHOLDER), fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = FireCashOnSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FireCashSurfaceContainerHigh,
                                unfocusedContainerColor = FireCashSurfaceContainerHigh,
                                focusedBorderColor = FireCashPrimary,
                                unfocusedBorderColor = FireCashOutlineVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("whitelist_input")
                        )
                        OutlinedTextField(
                            value = newWhitelistPrefix,
                            onValueChange = { newWhitelistPrefix = it },
                            placeholder = { Text(Translations.t(StringKeys.PREFIX_PLACEHOLDER), fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = FireCashOnSurface, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FireCashSurfaceContainerHigh,
                                unfocusedContainerColor = FireCashSurfaceContainerHigh,
                                focusedBorderColor = FireCashPrimary,
                                unfocusedBorderColor = FireCashOutlineVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("whitelist_prefix_input")
                        )
                        Button(
                            onClick = {
                                val pkg = newWhitelistApp.trim()
                                if (pkg.isNotEmpty()) {
                                    onAddWhitelistedApp(pkg, newWhitelistPrefix.trim())
                                    newWhitelistApp = ""
                                    newWhitelistPrefix = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("add_whitelist_button")
                        ) { Text(Translations.t(StringKeys.ADD_TO_WHITELIST)) }
                        if (displayIncomeWhitelist.isEmpty()) {
                            Text(
                                text = Translations.t(StringKeys.NO_WHITELIST),
                                color = FireCashOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                displayIncomeWhitelist.forEach { entry ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(FireCashSurfaceContainerHighest)
                                            .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = entry.packageName,
                                                color = FireCashOnSurface,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.weight(1f)
                                            )
                                            val isPermanent = permanentIncomeApps.any { it.packageName == entry.packageName && it.prefix == entry.prefix }
                                            if (isPermanent) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = Translations.t(StringKeys.PERMANENT_PRESET),
                                                        tint = FireCashSecondary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Switch(
                                                        checked = com.example.service.NotificationPresets.presetKey(entry) !in disabledIncomePresets,
                                                        onCheckedChange = { onToggleIncomePreset(entry, it) },
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = FireCashOnPrimary,
                                                            checkedTrackColor = FireCashPrimary,
                                                            uncheckedThumbColor = FireCashOutline,
                                                            uncheckedTrackColor = FireCashSurfaceVariant
                                                        ),
                                                        modifier = Modifier.scale(0.7f)
                                                    )
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = { onRemoveWhitelistedApp(entry.packageName + "|" + entry.prefix) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = Translations.t(StringKeys.REMOVE),
                                                        tint = FireCashOutline,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (entry.prefix.isNotBlank()) {
                                            Text(
                                                text = Translations.fmt(StringKeys.PREFIX_LABEL, entry.prefix),
                                                color = FireCashOnSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        } else {
                                            Text(
                                                text = Translations.t(StringKeys.PREFIX_LABEL_ANY),
                                                color = FireCashOnSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card: Notification Expense (Money Out) — same logic as Income but isMoneyIn=false
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow)
                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(FireCashSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = FireCashError,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Translations.t(StringKeys.NOTIFICATION_EXPENSE),
                                    color = FireCashOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = Translations.t(StringKeys.NOTIFICATION_EXPENSE_DESC),
                                    color = FireCashOnSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Switch(
                            checked = notificationExpenseEnabled,
                            onCheckedChange = onToggleNotificationExpense,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FireCashOnPrimary,
                                checkedTrackColor = FireCashError,
                                uncheckedThumbColor = FireCashOutline,
                                uncheckedTrackColor = FireCashSurfaceVariant
                            ),
                            modifier = Modifier.testTag("notification_expense_switch")
                        )
                    }
                    if (notificationExpenseEnabled) {
                        if (notificationAccessGranted) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = FireCashSecondary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = Translations.t(StringKeys.NOTIFICATION_ACCESS_GRANTED),
                                    color = FireCashSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Button(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = FireCashSurfaceContainerHigh)
                            ) {
                                Text(Translations.t(StringKeys.ENABLE_NOTIFICATION_ACCESS), color = FireCashOnSurface)
                            }
                        }
                        Text(
                            text = Translations.t(StringKeys.NOTIFICATION_EXPENSE_HELP),
                            color = FireCashOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = Translations.t(StringKeys.WHITELIST_TITLE),
                            color = FireCashOnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = Translations.t(StringKeys.EXPENSE_PREFIX_HELP),
                            color = FireCashOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = newExpenseWhitelistApp,
                            onValueChange = { newExpenseWhitelistApp = it },
                            placeholder = { Text(Translations.t(StringKeys.APP_PACKAGE_PLACEHOLDER), fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = FireCashOnSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FireCashSurfaceContainerHigh,
                                unfocusedContainerColor = FireCashSurfaceContainerHigh,
                                focusedBorderColor = FireCashPrimary,
                                unfocusedBorderColor = FireCashOutlineVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("expense_whitelist_input")
                        )
                        OutlinedTextField(
                            value = newExpenseWhitelistPrefix,
                            onValueChange = { newExpenseWhitelistPrefix = it },
                            placeholder = { Text(Translations.t(StringKeys.EXPENSE_PREFIX_PLACEHOLDER), fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = FireCashOnSurface, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FireCashSurfaceContainerHigh,
                                unfocusedContainerColor = FireCashSurfaceContainerHigh,
                                focusedBorderColor = FireCashPrimary,
                                unfocusedBorderColor = FireCashOutlineVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("expense_whitelist_prefix_input")
                        )
                        Button(
                            onClick = {
                                val pkg = newExpenseWhitelistApp.trim()
                                if (pkg.isNotEmpty()) {
                                    onAddExpenseWhitelistedApp(pkg, newExpenseWhitelistPrefix.trim())
                                    newExpenseWhitelistApp = ""
                                    newExpenseWhitelistPrefix = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("add_expense_whitelist_button")
                        ) { Text(Translations.t(StringKeys.ADD_TO_WHITELIST)) }
                        if (displayExpenseWhitelist.isEmpty()) {
                            Text(
                                text = Translations.t(StringKeys.NO_WHITELIST),
                                color = FireCashOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                displayExpenseWhitelist.forEach { entry ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(FireCashSurfaceContainerHighest)
                                            .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = entry.packageName,
                                                color = FireCashOnSurface,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.weight(1f)
                                            )
                                            val isPermanent = permanentExpenseApps.any { it.packageName == entry.packageName && it.prefix == entry.prefix }
                                            if (isPermanent) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = Translations.t(StringKeys.PERMANENT_PRESET),
                                                        tint = FireCashSecondary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Switch(
                                                        checked = com.example.service.NotificationPresets.presetKey(entry) !in disabledExpensePresets,
                                                        onCheckedChange = { onToggleExpensePreset(entry, it) },
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = FireCashOnPrimary,
                                                            checkedTrackColor = FireCashPrimary,
                                                            uncheckedThumbColor = FireCashOutline,
                                                            uncheckedTrackColor = FireCashSurfaceVariant
                                                        ),
                                                        modifier = Modifier.scale(0.7f)
                                                    )
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = { onRemoveExpenseWhitelistedApp(entry.packageName + "|" + entry.prefix) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = Translations.t(StringKeys.REMOVE),
                                                        tint = FireCashOutline,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (entry.prefix.isNotBlank()) {
                                            Text(text = Translations.fmt(StringKeys.PREFIX_LABEL, entry.prefix), color = FireCashOnSurfaceVariant, fontSize = 11.sp)
                                        } else {
                                            Text(text = Translations.t(StringKeys.PREFIX_LABEL_ANY), color = FireCashOnSurfaceVariant, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


            // Card: Background & Battery (keeps NotificationListener alive)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow)
                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(FireCashSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.BatterySaver, contentDescription = null, tint = if (batteryOptIgnored) FireCashSecondary else FireCashPrimary, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = Translations.t(StringKeys.BACKGROUND_BATTERY), color = FireCashOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = Translations.t(StringKeys.BACKGROUND_BATTERY_DESC), color = FireCashOnSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    Text(
                        text = if (batteryOptIgnored)
                            Translations.t(StringKeys.BATTERY_DISABLED_MSG)
                        else
                            Translations.t(StringKeys.BATTERY_ENABLED_MSG),
                        color = if (batteryOptIgnored) FireCashSecondary else FireCashOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = Translations.t(StringKeys.KEEP_LISTENING), color = FireCashOnSurface, fontSize = 14.sp)
                            Text(text = Translations.t(StringKeys.KEEP_LISTENING_DESC), color = FireCashOnSurfaceVariant, fontSize = 11.sp, lineHeight = 14.sp)
                        }
                        Switch(
                            checked = backgroundListening,
                            onCheckedChange = onToggleBackgroundListening,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FireCashOnPrimary,
                                checkedTrackColor = FireCashSecondary,
                                uncheckedThumbColor = FireCashOutline,
                                uncheckedTrackColor = FireCashSurfaceVariant
                            ),
                            modifier = Modifier.testTag("background_listening_switch")
                        )
                    }
                    if (!batteryOptIgnored) {
                        Button(
                            onClick = onRequestDisableBatteryOptimization,
                            modifier = Modifier.fillMaxWidth().testTag("disable_battery_optimization_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = FireCashPrimary)
                        ) {
                            Text(Translations.t(StringKeys.DISABLE_BATTERY_OPT), color = FireCashOnPrimary)
                        }
                    }
                }
            }

            // Card: Data Transfer (export/import JSON between phones)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow)
                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(FireCashSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = FireCashError, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = Translations.t(StringKeys.DATA_TRANSFER), color = FireCashOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = Translations.t(StringKeys.DATA_TRANSFER_DESC), color = FireCashOnSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    Button(
                        onClick = onExportData,
                        modifier = Modifier.fillMaxWidth().testTag("export_data_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = FireCashPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = FireCashOnPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translations.t(StringKeys.EXPORT_JSON), color = FireCashOnPrimary)
                    }
                    OutlinedButton(
                        onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth().testTag("import_data_button")
                    ) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = FireCashPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translations.t(StringKeys.IMPORT_JSON), color = FireCashPrimary)
                    }
                    Text(text = Translations.t(StringKeys.IMPORT_REPLACE_DESC), color = FireCashOnSurfaceVariant, fontSize = 11.sp)
                }
            }
                }
            }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Add Rule Dialog
        if (showAddRuleDialog) {
            AlertDialog(
                onDismissRequest = { showAddRuleDialog = false },
                title = {
                    Text(
                        text = Translations.t(StringKeys.ADD_KEYWORD_MAPPING),
                        color = FireCashOnSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newKeyword,
                            onValueChange = { newKeyword = it },
                            label = { Text(Translations.t(StringKeys.MERCHANT_KEYWORD)) },
                            placeholder = { Text(Translations.t(StringKeys.MERCHANT_EXAMPLE)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            label = { Text(Translations.t(StringKeys.MAP_TO_CATEGORY)) },
                            placeholder = { Text(Translations.t(StringKeys.CATEGORY_EXAMPLE)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newKeyword.isNotBlank()) {
                                onAddRule(newKeyword, newCategory)
                                newKeyword = ""
                            }
                            showAddRuleDialog = false
                        }
                    ) {
                        Text(Translations.t(StringKeys.ADD_RULE))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddRuleDialog = false }) {
                        Text(Translations.t(StringKeys.CANCEL))
                    }
                },
                containerColor = FireCashSurfaceContainerHighest
            )
        }
    }
}