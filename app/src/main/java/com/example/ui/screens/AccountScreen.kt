package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // Only allow delete for unknown/invalid slips (e.g. failed verification, no amount/data)
    return slip.amount == null || slip.verificationStatus == VerificationStatus.UNVERIFIED
}

@Composable
fun AccountScreen(
    slips: List<SavedSlip>,
    knownNames: List<String> = emptyList(),
    isLoading: Boolean = false,
    isBackgroundSyncing: Boolean = false,
    onBack: () -> Unit,
    onSlipClick: (SavedSlip) -> Unit,
    onDeleteSlip: (SavedSlip) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCamera: () -> Unit = {},
    onAutoSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Background sync when the page opens — non-blocking
    LaunchedEffect(Unit) {
        onAutoSync()
    }
    val moneyIn = slips.filter { effectiveIsMoneyIn(it, knownNames) == true && it.amount != null }.sumOf { it.amount!! }
    val moneyOut = slips.filter { effectiveIsMoneyIn(it, knownNames) == false && it.amount != null }.sumOf { it.amount!! }
    val balance = moneyIn - moneyOut
    var selectedKeys by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteMultiDialog by remember { mutableStateOf(false) }
    val isSelectionMode = selectedKeys.isNotEmpty()
    val selectedSlips = remember(slips, selectedKeys) { slips.filter { it.savedAt in selectedKeys } }
    val deletableSelected = remember(selectedSlips) { selectedSlips.filter { isDeletable(it) } }

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
                TextButton(onClick = { selectedKeys = slips.map { it.savedAt }.toSet() }) {
                    Text("All", color = FireCashPrimary, fontSize = 13.sp)
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FireCashPrimary
                    )
                }
                Text(
                    text = "My Account",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
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
                    onClick = onOpenCamera,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Open camera",
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions",
                color = FireCashOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (isBackgroundSyncing) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = FireCashOnSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Syncing…",
                    color = FireCashOnSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (slips.isEmpty()) {
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
                    text = "No transactions yet",
                    color = FireCashOnSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Scan a slip to record money in/out",
                    color = FireCashOnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val grouped = slips
                .groupBy { it.date ?: "Unknown" }
                .map { (date, list) -> date to list.sortedByDescending { it.savedAt } }
                .sortedByDescending { (date, _) -> date }

            LazyColumn(reverseLayout = true, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                grouped.forEach { (date, dateSlips) ->
                    item(key = "header_$date") {
                        DateHeader(date = date, count = dateSlips.size)
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
                            "${deletableSelected.size} of ${selectedKeys.size} selected are unknown/invalid and will be deleted. Verified slips will be kept. Continue?"
                        else
                            "Delete ${deletableSelected.size} unknown/invalid slip(s)? This cannot be undone.",
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
private fun DateHeader(date: String, count: Int) {
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
            text = "$count",
            color = FireCashOnSurfaceVariant,
            fontSize = 12.sp
        )
    }
}
