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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.ui.components.FireCashTopBar
import com.example.ui.theme.FireCashBackground
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
    currentCurrency: String,
    rules: List<KeywordRule>,
    googleDriveSync: Boolean,
    easySlipEnabled: Boolean,
    apiKey: String,
    checkDuplicates: Boolean,
    knownNames: List<String> = emptyList(),
    unverifiedCount: Int = 0,
    notificationIncomeEnabled: Boolean = false,
    notificationExpenseEnabled: Boolean = false,
    notificationWhitelist: List<com.example.service.WhitelistedApp> = emptyList(),
    notificationExpenseWhitelist: List<com.example.service.WhitelistedApp> = emptyList(),
    batteryOptIgnored: Boolean = false,
    onRequestDisableBatteryOptimization: () -> Unit = {},
    isLoading: Boolean = false,
    trackedFolders: List<String> = emptyList(),
    onCurrencyChange: (String) -> Unit,
    onToggleDriveSync: (Boolean) -> Unit,
    onToggleEasySlip: (Boolean) -> Unit,
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
    onImportSlips: (List<String>) -> Unit = {},
    onAddRule: (keyword: String, category: String) -> Unit,
    onRemoveRule: (KeywordRule) -> Unit,
    onNavigateToBackup: () -> Unit,
    onBack: () -> Unit, onNavigateToCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var newKeyword by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Travel") }

    var apiKeyText by remember { mutableStateOf(apiKey) }
    var newKnownName by remember { mutableStateOf("") }
    var newWhitelistApp by remember { mutableStateOf("") }
    var newWhitelistPrefix by remember { mutableStateOf("โอนเงินให้คุณ ฿") }
    var newExpenseWhitelistApp by remember { mutableStateOf("") }
    var newExpenseWhitelistPrefix by remember { mutableStateOf("โอนเงินสำเร็จ ฿") }

    val context = LocalContext.current
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

    val currencies = listOf("USD", "THB", "EUR", "GBP", "JPY")

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(FireCashBackground)
    ) {
        FireCashTopBar(
            title = "FireCash",
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
                text = "General Settings",
                color = FireCashOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Card 1: Base Currency
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow)
                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
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
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = FireCashPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Base Currency",
                                color = FireCashOnSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Set default currency for analytics",
                                color = FireCashOnSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Currency Dropdown
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FireCashSurfaceContainerHigh)
                                .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { currencyMenuExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("currency_selector_dropdown"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = when (currentCurrency) {
                                    "THB" -> "THB (฿)"
                                    "EUR" -> "EUR (€)"
                                    "GBP" -> "GBP (£)"
                                    "JPY" -> "JPY (¥)"
                                    else -> "USD ($)"
                                },
                                color = FireCashOnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = FireCashOnSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false },
                            modifier = Modifier.background(FireCashSurfaceContainerHighest)
                        ) {
                            currencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when (curr) {
                                                "THB" -> "THB (฿)"
                                                "EUR" -> "EUR (€)"
                                                "GBP" -> "GBP (£)"
                                                "JPY" -> "JPY (¥)"
                                                else -> "USD ($)"
                                            },
                                            color = FireCashOnSurface
                                        )
                                    },
                                    onClick = {
                                        onCurrencyChange(curr)
                                        currencyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // EasySlip Verification Configuration
            Text(
                text = "Bank Slip Verification (EasySlip Proxy)",
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
                                    text = "EasySlip Verification",
                                    color = FireCashOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Verify PromptPay / EMVCo Tag 91 CRC",
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
                            // API Key Field
                            OutlinedTextField(
                                value = apiKeyText,
                                onValueChange = {
                                    apiKeyText = it
                                    onUpdateApiKey(it)
                                },
                                label = { Text("EasySlip API Key") },
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
                                    text = "Prevent Duplicate Slips",
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
                                    Text("Sync $unverifiedCount unverified slip(s) now", color = FireCashOnPrimary)
                                }
                                if (apiKey.isBlank()) {
                                    Text(
                                        text = "Add API key to enable sync",
                                        color = FireCashOnSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
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
                                text = "My Names",
                                color = FireCashOnSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Receiver = your name → Income • Both = your names → Transfer (excluded from balance)",
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
                            placeholder = { Text("e.g. Somchai / สมชาย ใจดี", fontSize = 13.sp) },
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
                        ) { Text("Add") }
                    }
                    if (knownNames.isEmpty()) {
                        Text(
                            text = "No names yet — add your Thai / English variants.",
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
                                            contentDescription = "Remove",
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
                                    text = "Notification Income",
                                    color = FireCashOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Auto-capture income from bank notifications (first number → amount)",
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
                        Button(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FireCashSurfaceContainerHigh)
                        ) {
                            Text("Enable Notification Access", color = FireCashOnSurface)
                        }
                        Text(
                            text = "Scoops the first number from notifications and saves as Income. Ensure FireCash is enabled in system Notification Access.",
                            color = FireCashOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                        // Whitelist with prefix detection
                        Text(
                            text = "Whitelist (only these apps will be read)",
                            color = FireCashOnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Prefix: only notifications containing this text will be read, amount is first number after it. e.g. โอนเงินให้คุณ ฿",
                            color = FireCashOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = newWhitelistApp,
                            onValueChange = { newWhitelistApp = it },
                            placeholder = { Text("App package e.g. com.kasikornbank.kplus", fontSize = 12.sp) },
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
                            placeholder = { Text("Prefix e.g. โอนเงินให้คุณ ฿ (empty = any)", fontSize = 11.sp) },
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
                        ) { Text("Add to whitelist") }
                        if (notificationWhitelist.isEmpty()) {
                            Text(
                                text = "No whitelist — all apps will be read. Add package names to restrict.",
                                color = FireCashOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                notificationWhitelist.forEach { entry ->
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
                                            IconButton(
                                                onClick = { onRemoveWhitelistedApp(entry.packageName + "|" + entry.prefix) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = FireCashOutline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (entry.prefix.isNotBlank()) {
                                            Text(
                                                text = "Prefix: ${entry.prefix}",
                                                color = FireCashOnSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        } else {
                                            Text(
                                                text = "Prefix: (any)",
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
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notification Expense",
                                    color = FireCashOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Auto-capture expense from notifications (first number after prefix → amount)",
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
                                checkedTrackColor = Color(0xFFEF5350),
                                uncheckedThumbColor = FireCashOutline,
                                uncheckedTrackColor = FireCashSurfaceVariant
                            ),
                            modifier = Modifier.testTag("notification_expense_switch")
                        )
                    }
                    if (notificationExpenseEnabled) {
                        Button(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FireCashSurfaceContainerHigh)
                        ) {
                            Text("Enable Notification Access", color = FireCashOnSurface)
                        }
                        Text(
                            text = "Same service as Income — ensure FireCash is enabled in Notification Access.",
                            color = FireCashOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Whitelist (only these apps will be read)",
                            color = FireCashOnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Prefix: only notifications containing this text will be read, amount is first number after it. e.g. โอนเงินสำเร็จ ฿",
                            color = FireCashOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = newExpenseWhitelistApp,
                            onValueChange = { newExpenseWhitelistApp = it },
                            placeholder = { Text("App package e.g. com.kasikornbank.kplus", fontSize = 12.sp) },
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
                            placeholder = { Text("Prefix e.g. โอนเงินสำเร็จ ฿ (empty = any)", fontSize = 11.sp) },
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
                        ) { Text("Add to whitelist") }
                        if (notificationExpenseWhitelist.isEmpty()) {
                            Text(
                                text = "No whitelist — all apps will be read. Add package names to restrict.",
                                color = FireCashOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                notificationExpenseWhitelist.forEach { entry ->
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
                                            IconButton(
                                                onClick = { onRemoveExpenseWhitelistedApp(entry.packageName + "|" + entry.prefix) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = FireCashOutline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (entry.prefix.isNotBlank()) {
                                            Text(text = "Prefix: ${entry.prefix}", color = FireCashOnSurfaceVariant, fontSize = 11.sp)
                                        } else {
                                            Text(text = "Prefix: (any)", color = FireCashOnSurfaceVariant, fontSize = 11.sp)
                                        }
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
                            Text(text = "Tracked Folders", color = FireCashOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Auto-scan for new slips", color = FireCashOnSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    if (trackedFolders.isEmpty()) {
                        Text(text = "No folders tracked yet. Add folders to auto-scan for new slips.", color = FireCashOnSurfaceVariant, fontSize = 13.sp)
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
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove folder", tint = FireCashOnSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { folderPickerLauncher.launch(null) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = FireCashPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Add Tracked Folder", color = FireCashPrimary)
                    }
                    OutlinedButton(onClick = onSyncNow, enabled = trackedFolders.isNotEmpty() && !isLoading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = FireCashPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Sync Tracked Folders Now", color = FireCashPrimary)
                    }
                    OutlinedButton(onClick = { photoPickerLauncher.launch(arrayOf("image/*")) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = FireCashPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Import Slip Photos from Device", color = FireCashPrimary)
                    }
                    Text(text = "Slips in the tracked folder are scanned automatically for QR codes and added to your account.", color = FireCashOnSurfaceVariant, fontSize = 13.sp)
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FireCashPrimary)
                            Text(text = "Syncing slips...", color = FireCashOnSurfaceVariant, fontSize = 12.sp)
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
                            Text(text = "Background & Battery", color = FireCashOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Keep the app alive so notifications are caught", color = FireCashOnSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    Text(
                        text = if (batteryOptIgnored)
                            "Battery optimization is disabled — notification income/expense will run in background."
                        else
                            "Battery optimization is enabled. Disable it so the app can keep listening for notifications in the background.",
                        color = if (batteryOptIgnored) FireCashSecondary else FireCashOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                    if (!batteryOptIgnored) {
                        Button(
                            onClick = onRequestDisableBatteryOptimization,
                            modifier = Modifier.fillMaxWidth().testTag("disable_battery_optimization_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = FireCashPrimary)
                        ) {
                            Text("Disable Battery Optimization", color = FireCashOnPrimary)
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
                                    text = "Keyword Mapping",
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
                                        text = "Smart",
                                        color = FireCashPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Auto-assign categories based on extracted keywords.",
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
                                        contentDescription = "Remove rule",
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
                                    text = "Add Rule",
                                    color = FireCashPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Data & Storage",
                color = FireCashOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            // Card 3: Google Drive Sync & Backup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow)
                    .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { onNavigateToBackup() }
                    .padding(16.dp)
            ) {
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
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = FireCashPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Google Drive & Local Backup",
                                color = FireCashOnSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Manage snapshots and encrypted exports",
                                color = FireCashOnSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                        contentDescription = null,
                        tint = FireCashPrimary
                    )
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
                        text = "Add Keyword Mapping",
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
                            label = { Text("Merchant / Keyword") },
                            placeholder = { Text("e.g. Uber, Netflix, Starbucks, PromptPay") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            label = { Text("Map To Category") },
                            placeholder = { Text("e.g. Travel, Software, Food & Dining") },
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
                        Text("Add Rule")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddRuleDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = FireCashSurfaceContainerHighest
            )
        }
    }
}
