package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.data.model.VerificationStatus
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SavedSlip
import com.example.ui.theme.FireCashBackground
import com.example.ui.theme.FireCashError
import com.example.ui.theme.FireCashOnPrimaryContainer
import com.example.ui.theme.FireCashOnSecondaryContainer
import com.example.ui.theme.FireCashOnSurface
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashPrimaryContainer
import com.example.ui.theme.FireCashSecondary
import com.example.ui.theme.FireCashSecondaryContainer
import com.example.ui.theme.FireCashSurfaceContainerHigh
import com.example.ui.theme.FireCashSurfaceContainerHighest
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
private fun BalanceCardBody(
    balance: Double,
    selectedWallet: String?,
    onOpenCamera: () -> Unit,
    onOpenAnalytics: (String?) -> Unit,
    onAddManualEntry: () -> Unit
) {
    Column {
        // Total Balance section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Balance",
                    color = Color(0xFFcbd5e1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "THB %.2f".format(Locale.US, balance),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp
                )
            }
            // Eye toggle placeholder
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👁",
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Trend indicator
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF17D982).copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "▲",
                color = Color(0xFF40f1a4),
                fontSize = 10.sp
            )
            Text(
                text = "+3.4% this month",
                color = Color(0xFF40f1a4),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        // Card action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Scan Slip button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable {
                        if (selectedWallet == "cash") onAddManualEntry()
                        else onOpenCamera()
            }
            .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Scan Slip",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
        }
            }
            // Analytics button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onOpenAnalytics(selectedWallet) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Analytics",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
        }
            }
        }
    }
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
    onOpenAnalytics: (String?) -> Unit,
    onOpenCamera: () -> Unit = {},
    onAddManual: (amount: Double, isMoneyIn: Boolean, note: String, wallet: String?) -> Unit = { _, _, _, _ -> },
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
    val walletSlips by remember { derivedStateOf {
            when (selectedWallet) {
                "cash" -> slips.filter { it.wallet == "cash" }
                else -> slips.filter { it.wallet != "cash" }
            }
        } }
    val moneyIn by remember { derivedStateOf { walletSlips.filter { effectiveIsMoneyIn(it, knownNames) == true && it.amount != null }.sumOf { it.amount!! } } }
    val moneyOut by remember { derivedStateOf { walletSlips.filter { effectiveIsMoneyIn(it, knownNames) == false && it.amount != null }.sumOf { it.amount!! } } }
    val balance by remember { derivedStateOf { moneyIn - moneyOut } }
    val bankBalance by remember { derivedStateOf {
        slips.filter { effectiveIsMoneyIn(it, knownNames) == true && it.amount != null && (it.wallet == null || it.wallet == "bank") }.sumOf { it.amount!! } -
        slips.filter { effectiveIsMoneyIn(it, knownNames) == false && it.amount != null && (it.wallet == null || it.wallet == "bank") }.sumOf { it.amount!! }
    } }
    val cashBalance by remember { derivedStateOf {
        slips.filter { effectiveIsMoneyIn(it, knownNames) == true && it.amount != null && it.wallet == "cash" }.sumOf { it.amount!! } -
        slips.filter { effectiveIsMoneyIn(it, knownNames) == false && it.amount != null && it.wallet == "cash" }.sumOf { it.amount!! }
    } }
    var selectedKeys by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteMultiDialog by remember { mutableStateOf(false) }
    val isSelectionMode = selectedKeys.isNotEmpty()
    val selectedSlips by remember { derivedStateOf { slips.filter { it.savedAt in selectedKeys } } }
    val deletableSelected by remember { derivedStateOf { selectedSlips.filter { isDeletable(it) } } }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var manualAmount by remember { mutableStateOf("") }
    var manualIsIn by remember { mutableStateOf(true) }
    var manualNote by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf<String?>(null) } // null=All, "income", "expense", "transfer"
    val filteredSlips by remember { derivedStateOf {
        val categoryFiltered = if (filterCategory != null) {
            walletSlips.filter { slip ->
                when (filterCategory) {
                    "income" -> effectiveIsMoneyIn(slip, knownNames) == true
                    "expense" -> effectiveIsMoneyIn(slip, knownNames) == false
                    "transfer" -> effectiveIsMoneyIn(slip, knownNames) == null
                    else -> true
        }
            }
        } else walletSlips
        if (searchQuery.isBlank()) categoryFiltered
        else {
            val q = searchQuery.trim()
            val qLower = q.lowercase(Locale.ROOT)
            categoryFiltered.filter { slip ->
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
                            tint = FireCashError
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
                // Bank/Cash toggle pill — Figma style
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FireCashSurfaceContainerLow)
                        .clickable {
                            selectedWallet = if (selectedWallet == "cash") null else "cash"
            }
                .padding(horizontal = 2.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Bank option
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selectedWallet != "cash") FireCashSurfaceContainerHighest
                                    else Color.Transparent
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Bank",
                                color = if (selectedWallet != "cash") Color.White else FireCashOnSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = if (selectedWallet != "cash") FontWeight.SemiBold else FontWeight.Normal
                            )
            }
                // Cash option
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selectedWallet == "cash") FireCashSurfaceContainerHighest
                                    else Color.Transparent
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cash",
                                color = if (selectedWallet == "cash") Color.White else FireCashOnSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = if (selectedWallet == "cash") FontWeight.SemiBold else FontWeight.Normal
                            )
            }
            }
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

        // Balance card — seamless continuous swipe (Bank ↔ Cash)
        val scope = rememberCoroutineScope()
        var cardWidthPx by remember { mutableStateOf(0) }
        // 0f = Bank centered, 1f = Cash centered
        val cardProgress = remember { Animatable(if (selectedWallet == "cash") 1f else 0f) }

        // Sync progress when pill toggle changes selection externally
        LaunchedEffect(selectedWallet) {
            val target = if (selectedWallet == "cash") 1f else 0f
            if (kotlin.math.abs(cardProgress.value - target) > 0.01f) {
                cardProgress.animateTo(
                    target,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { cardWidthPx = it.width }
                .pointerInput(cardWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val w = cardWidthPx.toFloat().coerceAtLeast(1f)
                                val target = if (cardProgress.value > 0.5f) 1f else 0f
                                cardProgress.animateTo(
                                    target,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                )
                                selectedWallet = if (target > 0.5f) "cash" else null
            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val w = cardWidthPx.toFloat().coerceAtLeast(1f)
                                cardProgress.snapTo(
                                    (cardProgress.value - dragAmount / w).coerceIn(0f, 1f)
                                )
            }
            }
            )
        }
        ) {
            val w = cardWidthPx.toFloat().coerceAtLeast(1f)
            val p = cardProgress.value

            // Bank card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = -p * w }
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A2744),
                                Color(0xFF1E3058),
                                Color(0xFF1A3D5C),
                                Color(0xFF184A5E)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset.Infinite
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                BalanceCardBody(
                    balance = bankBalance,
                    selectedWallet = null,
                    onOpenCamera = onOpenCamera,
                    onOpenAnalytics = onOpenAnalytics,
                    onAddManualEntry = { showAddManualDialog = true }
                )
            }

            // Cash card (declared on top Z-wise; at rest it sits off right edge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = (1f - p) * w }
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF065F46),
                                Color(0xFF047857),
                                Color(0xFF059669),
                                Color(0xFF10B981)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset.Infinite
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                BalanceCardBody(
                    balance = cashBalance,
                    selectedWallet = "cash",
                    onOpenCamera = onOpenCamera,
                    onOpenAnalytics = onOpenAnalytics,
                    onAddManualEntry = { showAddManualDialog = true }
                )
            }
        }
        // Page indicator dots — Bank / Cash
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isBank = selectedWallet == null
            val isCash = selectedWallet == "cash"
            // Dot 1 — Bank
            Box(
                modifier = Modifier
                    .size(if (isBank) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isBank) Color.White.copy(alpha = 0.9f)
                        else Color.White.copy(alpha = 0.3f)
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            // Dot 2 — Cash
            Box(
                modifier = Modifier
                    .size(if (isCash) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCash) Color.White.copy(alpha = 0.9f)
                        else Color.White.copy(alpha = 0.3f)
                    )
            )
        }
        // Income/Spent row — separate card below
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FireCashSurfaceContainerLow, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = FireCashSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Income",
                        color = FireCashOnSurfaceVariant,
                        fontSize = 12.sp
                    )
        }
                Text(
                    text = "THB %.2f".format(Locale.US, moneyIn),
                    color = FireCashSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Spent",
                        color = FireCashOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = FireCashError,
                        modifier = Modifier.size(16.dp)
                    )
        }
                Text(
                    text = "THB %.2f".format(Locale.US, moneyOut),
                    color = FireCashError,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }



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
        // Filter chips
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chips = listOf(
                null to "All",
                "income" to "Income",
                "expense" to "Expense",
                "transfer" to "Transfer"
            )
            chips.forEach { (key, label) ->
                val selected = filterCategory == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (selected) FireCashSurfaceContainerHighest else FireCashSurfaceContainerLow)
                        .clickable { filterCategory = key }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) FireCashPrimary else FireCashOnSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
        }
            }
        }
        if (isSearchActive) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireCashSurfaceContainerLow.copy(alpha = 0.9f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = FireCashOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search transactions, merchant...", fontSize = 14.sp, color = FireCashOnSurfaceVariant.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = FireCashOnSurface, fontSize = 14.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = FireCashPrimary
                        )
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = FireCashOnSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            }
        }
            }
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
                        colors = ButtonDefaults.buttonColors(containerColor = FireCashError)
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
                                    .background(if (manualIsIn) FireCashSecondary.copy(alpha = 0.2f) else FireCashSurfaceContainerLow, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, if (manualIsIn) FireCashSecondary else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { manualIsIn = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Money In", color = if (manualIsIn) FireCashSecondary else FireCashOnSurfaceVariant, fontWeight = if (manualIsIn) FontWeight.Bold else FontWeight.Normal)
            }
                    Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(if (!manualIsIn) FireCashError.copy(alpha = 0.2f) else FireCashSurfaceContainerLow, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, if (!manualIsIn) FireCashError else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { manualIsIn = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Money Out", color = if (!manualIsIn) FireCashError else FireCashOnSurfaceVariant, fontWeight = if (!manualIsIn) FontWeight.Bold else FontWeight.Normal)
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
                                onAddManual(amt, manualIsIn, manualNote.trim(), selectedWallet)
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
        isIn -> FireCashSecondary
        else -> FireCashError
    }
    val title = when {
        isSelf -> "Transfer"
        isIn -> slip.senderName ?: slip.receiverName ?: "Income"
        else -> slip.receiverName ?: slip.senderName ?: "Expense"
    }
    val category = when {
        isSelf -> "Transfer"
        isIn -> "Income"
        else -> "Expense"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) FireCashPrimary.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp),
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
        // Category icon — Figma style: solid brand color circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = arrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Merchant + category
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = category,
                color = FireCashOnSurfaceVariant,
                fontSize = 11.sp
            )
        }
        // Amount + time column (right-aligned)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (isSelf) "THB %.2f".format(Locale.US, slip.amount ?: 0.0)
                else "${if (isIn) "+" else "-"}THB %.2f".format(Locale.US, slip.amount ?: 0.0),
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (!slip.time.isNullOrBlank()) {
                Text(
                    text = slip.time,
                    color = FireCashOnSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun DateHeader(date: String, count: Int, total: Double) {
    val totalColor = when {
        total > 0.0 -> FireCashSecondary
        total < 0.0 -> FireCashError
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

