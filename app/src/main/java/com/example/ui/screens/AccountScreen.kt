package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.model.VerificationStatus
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SavedSlip
import com.example.ui.theme.FireCashBackground
import com.example.ui.theme.FireCashOnSurface
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashSecondaryContainer
import com.example.ui.theme.FireCashSurfaceContainerLow
import java.util.Locale

private fun isSelfTransfer(slip: SavedSlip, knownNames: List<String> = emptyList()): Boolean {
    val s = slip.senderName?.trim()?.lowercase(Locale.ROOT)
    val r = slip.receiverName?.trim()?.lowercase(Locale.ROOT)
    if (!s.isNullOrEmpty() && s == r) return true
    if (knownNames.isEmpty()) return false
    val sKnown = !s.isNullOrEmpty() && knownNames.any { it.trim().lowercase(Locale.ROOT) == s }
    val rKnown = !r.isNullOrEmpty() && knownNames.any { it.trim().lowercase(Locale.ROOT) == r }
    return sKnown && rKnown
}

private fun isKnownName(name: String?, knownNames: List<String>): Boolean {
    if (name.isNullOrBlank() || knownNames.isEmpty()) return false
    val norm = name.trim().lowercase(Locale.ROOT)
    return knownNames.any { it.trim().lowercase(Locale.ROOT) == norm }
}

private fun effectiveIsMoneyIn(slip: SavedSlip, knownNames: List<String>): Boolean? {
    // Manual override takes precedence
    when (slip.manualCategory) {
        "income" -> return true
        "expense" -> return false
        "transfer" -> return null
    }
    if (isSelfTransfer(slip, knownNames)) return null // Transfer - excluded from balance
    val receiverKnown = isKnownName(slip.receiverName, knownNames)
    val senderKnown = isKnownName(slip.senderName, knownNames)
    return when {
        receiverKnown -> true
        senderKnown -> false
        else -> slip.isMoneyIn
    }
}

private fun isDeletable(slip: SavedSlip): Boolean {
    // Deletable when unverified or the slip has no transaction reference (no way to verify identity)
    return slip.verificationStatus == VerificationStatus.UNVERIFIED ||
        slip.transRef.isNullOrBlank() ||
        slip.amount == null
}

@Composable
fun AccountScreen(
    slips: List<SavedSlip>,
    knownNames: List<String> = emptyList(),
    isLoading: Boolean = false,
    isBackgroundSyncing: Boolean = false,
    isUserSyncing: Boolean = false,
    onBack: () -> Unit,
    onSlipClick: (SavedSlip) -> Unit,
    onDeleteSlip: (SavedSlip) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCamera: () -> Unit = {},
    onAddManual: (amount: Double, isMoneyIn: Boolean, note: String) -> Unit = { _, _, _ -> },
    onAutoSync: () -> Unit,
    onSyncNow: () -> Unit = {},
    onFullResync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Background sync when the page opens — non-blocking
    LaunchedEffect(Unit) {
        onAutoSync()
    }
    var selectedWallet by remember { mutableStateOf<String?>(null) } // null = Bank, "cash" = Cash
    val walletSlips = remember(slips, selectedWallet) {
        if (selectedWallet == null) slips else slips.filter { it.wallet == "cash" }
    }
    val moneyIn = walletSlips.filter { effectiveIsMoneyIn(it, knownNames) == true && it.amount != null }.sumOf { it.amount!! }
    val moneyOut = walletSlips.filter { effectiveIsMoneyIn(it, knownNames) == false && it.amount != null }.sumOf { it.amount!! }
    val balance = moneyIn - moneyOut
    val bankBalance = slips.filter { effectiveIsMoneyIn(it, knownNames) == true && it.amount != null && (it.wallet == null || it.wallet == "bank") }.sumOf { it.amount!! } -
        slips.filter { effectiveIsMoneyIn(it, knownNames) == false && it.amount != null && (it.wallet == null || it.wallet == "bank") }.sumOf { it.amount!! }
    val cashBalance = slips.filter { effectiveIsMoneyIn(it, knownNames) == true && it.amount != null && it.wallet == "cash" }.sumOf { it.amount!! } -
        slips.filter { effectiveIsMoneyIn(it, knownNames) == false && it.amount != null && it.wallet == "cash" }.sumOf { it.amount!! }
    var selectedKeys by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteMultiDialog by remember { mutableStateOf(false) }
    val isSelectionMode = selectedKeys.isNotEmpty()
    val selectedSlips = remember(slips, selectedKeys) { slips.filter { it.savedAt in selectedKeys } }
    val deletableSelected = remember(selectedSlips) { selectedSlips.filter { isDeletable(it) } }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var manualAmount by remember { mutableStateOf("") }
    var manualIsIn by remember { mutableStateOf(true) }
    var manualNote by remember { mutableStateOf("") }
    val filteredSlips = remember(walletSlips, searchQuery, knownNames) {
        if (searchQuery.isBlank()) walletSlips
        else {
            val q = searchQuery.trim()
            val qLower = q.lowercase(Locale.ROOT)
            walletSlips.filter { slip ->
                val dateMatch = slip.date?.lowercase(Locale.ROOT)?.contains(qLower) == true
                val title = when {
                    isSelfTransfer(slip, knownNames) -> "transfer"
                    effectiveIsMoneyIn(slip, knownNames) == true -> slip.senderName ?: ""
                    else -> slip.receiverName ?: ""
                }.lowercase(Locale.ROOT)
                val titleMatch = title.contains(qLower)
                val amountStr = slip.amount?.let { "%.2f".format(Locale.US, it) } ?: ""
                val amountMatch = amountStr.contains(q) || slip.amount?.toString()?.contains(q) == true
                val payloadExact = slip.payload == q
                val transRefExact = slip.transRef == q
                dateMatch || titleMatch || amountMatch || payloadExact || transRefExact
            }
        }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedKeys = emptySet()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FireCashBackground)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                IconButton(onClick = { selectedKeys = emptySet() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear selection",
                        tint = FireCashPrimary
                    )
                }
                Text(
                    text = "${selectedKeys.size} selected",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (deletableSelected.isNotEmpty()) {
                    IconButton(onClick = { showDeleteMultiDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete selected",
                            tint = Color(0xFFEF5350)
                        )
                    }
                }
                TextButton(onClick = { selectedKeys = walletSlips.map { it.savedAt }.toSet() }) {
                    Text("All", color = FireCashPrimary, fontSize = 13.sp)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.firecash_icon),
                        contentDescription = "FireCash logo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(FireCashSurfaceContainerLow)
                    )
                    Text(
                        text = "FireCash",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Account settings",
                        tint = FireCashPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Wallet tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val bankSelected = selectedWallet == null
            TextButton(
                onClick = { selectedWallet = null },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (bankSelected) FireCashSecondaryContainer else Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "Bank",
                    tint = FireCashOnSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bank",
                    color = FireCashOnSurface,
                    fontWeight = if (bankSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
            val cashSelected = selectedWallet == "cash"
            TextButton(
                onClick = { selectedWallet = "cash" },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (cashSelected) FireCashSecondaryContainer else Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Cash",
                    tint = FireCashOnSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cash",
                    color = FireCashOnSurface,
                    fontWeight = if (cashSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Balance card - camera button at top-right, persistent
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FireCashSecondaryContainer, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Current Balance",
                        color = FireCashOnSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "THB %.2f".format(Locale.US, balance),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = if (selectedWallet == "cash") { { showAddManualDialog = true } } else onOpenCamera,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (selectedWallet == "cash") Icons.Default.Add else Icons.Default.PhotoCamera,
                        contentDescription = if (selectedWallet == "cash") "Add cash entry" else "Open camera",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = Color(0xFF66BB6A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Money In",
                            color = FireCashOnSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "THB %.2f".format(Locale.US, moneyIn),
                        color = Color(0xFF66BB6A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Money Out",
                            color = FireCashOnSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "THB %.2f".format(Locale.US, moneyOut),
                        color = Color(0xFFEF5350),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Wallet breakdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Bank",
                            color = FireCashOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "THB %.2f".format(Locale.US, bankBalance),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Cash",
                            color = FireCashOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "THB %.2f".format(Locale.US, cashBalance),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Summary button
        Button(
            onClick = onOpenAnalytics,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FireCashPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "View Spending Summary",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Transactions",
                    color = FireCashOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isBackgroundSyncing || isUserSyncing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = FireCashOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUserSyncing) "Syncing…" else "Auto-sync…",
                        color = FireCashOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchActive) "Close search" else "Search slips",
                        tint = FireCashPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        showAddManualDialog = true
                        manualAmount = ""
                        manualNote = ""
                        manualIsIn = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add income/expense manually",
                        tint = FireCashPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                // Sync: tap = new photos only, hold 10s = full resync (re-detect all + re-verify on server)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val startMs = down.uptimeMillis
                                val up = withTimeoutOrNull(10_000L) { waitForUpOrCancellation() }
                                when {
                                    up != null -> onSyncNow()
                                    android.os.SystemClock.uptimeMillis() - startMs >= 10_000L -> onFullResync()
                                    else -> Unit // cancelled early (scroll) — ignore
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync (tap = new slips, hold 10s = full resync)",
                        tint = FireCashPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (isSearchActive) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search date, title, amount, or exact QR payload", fontSize = 13.sp, color = FireCashOnSurfaceVariant) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = com.example.ui.theme.FireCashOnSurface, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FireCashPrimary,
                    unfocusedBorderColor = FireCashOnSurfaceVariant.copy(alpha = 0.4f),
                    focusedContainerColor = FireCashSurfaceContainerLow,
                    unfocusedContainerColor = FireCashSurfaceContainerLow,
                    cursorColor = FireCashPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = FireCashOnSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (filteredSlips.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = FireCashOnSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (slips.isEmpty()) "No transactions yet" else "No matching slips",
                    color = FireCashOnSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (slips.isEmpty()) "Scan a slip to record money in/out" else "Try another date, title, amount or exact QR payload",
                    color = FireCashOnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val grouped = filteredSlips
                .groupBy { it.date ?: "Unknown" }
                .map { (date, list) -> date to list.sortedByDescending { it.savedAt } }
                .sortedByDescending { (date, _) -> date }

            LazyColumn(modifier = Modifier.fillMaxWidth(), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                grouped.forEach { (date, dateSlips) ->
                    val dayTotal = dateSlips.sumOf { slip ->
                        val amt = slip.amount ?: return@sumOf 0.0
                        when (effectiveIsMoneyIn(slip, knownNames)) {
                            true -> amt
                            false -> -amt
                            else -> 0.0
                        }
                    }
                    items(dateSlips, key = { it.savedAt }) { slip ->
                        val isSelected = slip.savedAt in selectedKeys
                        TransactionRow(
                            slip = slip,
                            knownNames = knownNames,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedKeys = if (isSelected) selectedKeys - slip.savedAt else selectedKeys + slip.savedAt
                                } else {
                                    onSlipClick(slip)
                                }
                            },
                            onLongClick = {
                                selectedKeys = if (isSelected) selectedKeys - slip.savedAt else selectedKeys + slip.savedAt
                            }
                        )
                    }
                    item(key = "header_$date") {
                        DateHeader(date = date, count = dateSlips.size, total = dayTotal)
                    }
                }
            }
        }
        }

        // Loading overlay while syncing
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = "Syncing slips...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Multi-delete confirmation (only deletable among selected)
        if (showDeleteMultiDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteMultiDialog = false },
                title = { Text("Delete ${deletableSelected.size} slip(s)?", color = Color.White) },
                text = {
                    Text(
            if (deletableSelected.size < selectedKeys.size)
                "${deletableSelected.size} of ${selectedKeys.size} selected are unverified or have no reference and will be deleted. Verified slips will be kept. Continue?"
            else
                "Delete ${deletableSelected.size} unverified slip(s)? This cannot be undone.",
                        color = FireCashOnSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            deletableSelected.forEach { onDeleteSlip(it) }
                            selectedKeys = emptySet()
                            showDeleteMultiDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                    ) { Text("Delete", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteMultiDialog = false }) { Text("Cancel") }
                },
                containerColor = FireCashSurfaceContainerLow
            )
        }

        // Add manual income/expense dialog
        if (showAddManualDialog) {
            AlertDialog(
                onDismissRequest = { showAddManualDialog = false },
                title = { Text("Add Transaction", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(if (manualIsIn) Color(0xFF66BB6A).copy(alpha = 0.2f) else FireCashSurfaceContainerLow, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, if (manualIsIn) Color(0xFF66BB6A) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { manualIsIn = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Money In", color = if (manualIsIn) Color(0xFF66BB6A) else FireCashOnSurfaceVariant, fontWeight = if (manualIsIn) FontWeight.Bold else FontWeight.Normal)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(if (!manualIsIn) Color(0xFFEF5350).copy(alpha = 0.2f) else FireCashSurfaceContainerLow, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, if (!manualIsIn) Color(0xFFEF5350) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { manualIsIn = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Money Out", color = if (!manualIsIn) Color(0xFFEF5350) else FireCashOnSurfaceVariant, fontWeight = if (!manualIsIn) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                        OutlinedTextField(
                            value = manualAmount,
                            onValueChange = { manualAmount = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Amount (THB)") },
                            placeholder = { Text("e.g. 1500.00") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = com.example.ui.theme.FireCashOnSurface, fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireCashPrimary,
                                unfocusedBorderColor = FireCashOnSurfaceVariant.copy(alpha = 0.4f),
                                focusedContainerColor = FireCashSurfaceContainerLow,
                                unfocusedContainerColor = FireCashSurfaceContainerLow,
                                cursorColor = FireCashPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = manualNote,
                            onValueChange = { manualNote = it },
                            label = { Text("Note (optional)") },
                            placeholder = { Text("e.g. Groceries, Salary, Food") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = com.example.ui.theme.FireCashOnSurface, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireCashPrimary,
                                unfocusedBorderColor = FireCashOnSurfaceVariant.copy(alpha = 0.4f),
                                focusedContainerColor = FireCashSurfaceContainerLow,
                                unfocusedContainerColor = FireCashSurfaceContainerLow,
                                cursorColor = FireCashPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = manualAmount.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                onAddManual(amt, manualIsIn, manualNote.trim())
                                showAddManualDialog = false
                            }
                        },
                        enabled = (manualAmount.toDoubleOrNull() ?: 0.0) > 0
                    ) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddManualDialog = false }) { Text("Cancel") }
                },
                containerColor = FireCashSurfaceContainerLow
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionRow(
    slip: SavedSlip,
    knownNames: List<String> = emptyList(),
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val effective = effectiveIsMoneyIn(slip, knownNames)
    val isSelf = effective == null
    val isIn = effective == true
    val arrow = when {
        isSelf -> Icons.Default.SwapHoriz
        isIn -> Icons.Default.ArrowDownward
        else -> Icons.Default.ArrowUpward
    }
    val color = when {
        isSelf -> Color(0xFF9E9E9E)
        isIn -> Color(0xFF66BB6A)
        else -> Color(0xFFEF5350)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) FireCashPrimary.copy(alpha = 0.12f) else FireCashSurfaceContainerLow,
                RoundedCornerShape(16.dp)
            )
            .border(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) FireCashPrimary else Color.Gray.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        if (isSelected) FireCashPrimary else Color.Transparent,
                        CircleShape
                    )
                    .border(1.5.dp, if (isSelected) FireCashPrimary else Color.Gray.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = arrow,
                contentDescription = if (isIn) "Money in" else "Money out",
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    isSelf -> "Transfer"
                    isIn -> slip.senderName ?: "Transfer"
                    else -> slip.receiverName ?: "Transfer"
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = slip.transRef ?: slip.payload.take(20),
                color = FireCashOnSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            if (!slip.time.isNullOrBlank()) {
                Text(
                    text = slip.time!!,
                    color = FireCashOnSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = if (isSelf) "THB %.2f".format(Locale.US, slip.amount ?: 0.0)
                else "${if (isIn) "+" else "-"}THB %.2f".format(Locale.US, slip.amount ?: 0.0),
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DateHeader(date: String, count: Int, total: Double) {
    val totalColor = when {
        total > 0.0 -> Color(0xFF66BB6A)
        total < 0.0 -> Color(0xFFEF5350)
        else -> FireCashOnSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (date == "Unknown") "Unknown Date" else date,
            color = FireCashPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "THB %.2f • %d".format(Locale.US, total, count),
            color = totalColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
