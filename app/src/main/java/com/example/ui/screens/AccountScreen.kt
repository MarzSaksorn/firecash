package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun AccountScreen(
    slips: List<SavedSlip>,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onSlipClick: (SavedSlip) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onAutoSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-sync tracked folder when the page opens
    LaunchedEffect(Unit) {
        onAutoSync()
    }
    val moneyIn = slips.filter { it.isMoneyIn && it.amount != null }.sumOf { it.amount!! }
    val moneyOut = slips.filter { !it.isMoneyIn && it.amount != null }.sumOf { it.amount!! }
    val balance = moneyIn - moneyOut

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

        Spacer(modifier = Modifier.height(12.dp))

        // Balance card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FireCashSecondaryContainer, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
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
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
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

        Text(
            text = "Transactions",
            color = FireCashOnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
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

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                grouped.forEach { (date, dateSlips) ->
                    item(key = "header_$date") {
                        DateHeader(date = date, count = dateSlips.size)
                    }
                    items(dateSlips, key = { it.savedAt }) { slip ->
                        TransactionRow(slip = slip, onClick = { onSlipClick(slip) })
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
    }
}

@Composable
private fun TransactionRow(slip: SavedSlip, onClick: () -> Unit) {
    val isIn = slip.isMoneyIn
    val arrow = if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
    val color = if (isIn) Color(0xFF66BB6A) else Color(0xFFEF5350)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FireCashSurfaceContainerLow, RoundedCornerShape(16.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                text = if (isIn) (slip.senderName ?: "Transfer") else (slip.receiverName ?: "Transfer"),
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
                text = "${if (isIn) "+" else "-"}THB %.2f".format(Locale.US, slip.amount ?: 0.0),
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
