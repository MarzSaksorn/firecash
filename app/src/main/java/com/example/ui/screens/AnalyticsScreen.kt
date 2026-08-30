package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.analytics.AnalyticsEngine
import com.example.data.analytics.InsightType
import com.example.data.analytics.SpendingInsight
import com.example.data.model.Expense
import com.example.data.model.SavedSlip
import com.example.ui.theme.FireCashBackground
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashSurfaceContainerLow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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
    if (isSelfTransfer(slip, knownNames)) return null
    val receiverKnown = isKnownName(slip.receiverName, knownNames)
    val senderKnown = isKnownName(slip.senderName, knownNames)
    return when {
        receiverKnown -> true
        senderKnown -> false
        else -> slip.isMoneyIn
    }
}

@Composable
fun AnalyticsScreen(
    slips: List<SavedSlip>,
    knownNames: List<String> = emptyList(),
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val now = LocalDate.now()
    val expenses = remember(slips, knownNames, now) {
        val today = now.toString()
        slips.mapNotNull { slip ->
            if (isSelfTransfer(slip, knownNames)) return@mapNotNull null
            val amt = slip.amount ?: return@mapNotNull null
            val effective = effectiveIsMoneyIn(slip, knownNames) ?: return@mapNotNull null
            Expense(
                merchant = slip.senderName ?: slip.receiverName ?: "Unknown",
                amount = amt,
                date = normalizeDate(slip.date, today),
                time = slip.time ?: "",
                category = if (effective) "Income" else "Other"
            )
        }
    }
    val analytics = AnalyticsEngine.generateAnalytics(expenses)
    val totalSpent = analytics.totalSpent
    val changePct = analytics.changePercentage
    val avgPerDay = analytics.averagePerDay
    val insights = analytics.insights

    val monthKey = remember(now) { now.format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)) }
    val monthLabel = remember(now) {
        "${now.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${now.year}"
    }
    val availableMonths = remember(expenses) {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
        expenses.map { it.date.take(7) }
            .filter { it.length == 7 }
            .distinct()
            .sortedDescending()
            .take(3)
            .mapNotNull { key ->
                val m = runCatching { LocalDate.parse("$key-01", fmt) }.getOrNull() ?: return@mapNotNull null
                MonthTotals(
                    key = key,
                    label = "${m.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${m.year}",
                    income = expenses.filter { it.date.startsWith(key) && it.category == "Income" }.sumOf { e -> e.amount },
                    expense = expenses.filter { it.date.startsWith(key) && it.category != "Income" }.sumOf { e -> e.amount },
                    isCurrent = key == monthKey
                )
            }
    }
    var comparedKeys by remember(monthKey, availableMonths) {
        mutableStateOf(setOf(availableMonths.firstOrNull()?.key ?: monthKey))
    }
    var showCompareDialog by remember { mutableStateOf(false) }
    val pieMonths = remember(comparedKeys, availableMonths) {
        availableMonths.filter { it.key in comparedKeys }.sortedByDescending { it.key }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FireCashBackground)
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
                text = "Spending Summary",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Total Spent",
                value = "THB %.2f".format(Locale.US, totalSpent),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Avg/Day",
                value = "THB %.2f".format(Locale.US, avgPerDay),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "vs Last",
                value = "%+.1f%%".format(Locale.US, changePct),
                valueColor = if (changePct >= 0) Color(0xFFEF5350) else Color(0xFF66BB6A),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Income vs Spending",
                    color = FireCashOnSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = monthLabel,
                        color = FireCashPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { showCompareDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Compare", color = FireCashPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (pieMonths.any { it.income + it.expense > 0 }) {
                PieChart(
                    months = pieMonths,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                pieMonths.forEach { m ->
                    if (m.income + m.expense <= 0) return@forEach
                    LegendRow(
                        color = Color(0xFF66BB6A),
                        label = if (pieMonths.size == 1) "Money In" else "${m.label.substringBefore(' ')} · In",
                        amount = m.income,
                        total = m.income + m.expense
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LegendRow(
                        color = Color(0xFFFF6B00),
                        label = if (pieMonths.size == 1) "Money Out" else "${m.label.substringBefore(' ')} · Out",
                        amount = m.expense,
                        total = m.income + m.expense
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (availableMonths.isEmpty()) "No dated transactions yet" else "No transactions in this month",
                        color = FireCashOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (insights.isNotEmpty()) {
            Text(
                text = "AI Insights",
                color = FireCashOnSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(insights) { insight ->
                    InsightRow(insight = insight)
                }
            }
        } else {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = FireCashOnSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No data yet",
                        color = FireCashOnSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    if (showCompareDialog) {
        AlertDialog(
            onDismissRequest = { showCompareDialog = false },
            containerColor = FireCashSurfaceContainerLow,
            title = {
                Text(
                    text = "Compare Months",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                if (availableMonths.isEmpty()) {
                    Text(
                        text = "No dated transactions yet",
                        color = FireCashOnSurfaceVariant,
                        fontSize = 13.sp
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(max = 360.dp)
                    ) {
                        availableMonths.forEach { mt ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = mt.key in comparedKeys,
                                onCheckedChange = { checked ->
                                    if (mt.isCurrent) return@Checkbox
                                    comparedKeys = if (checked) comparedKeys + mt.key else comparedKeys - mt.key
                                },
                                enabled = !mt.isCurrent,
                                colors = CheckboxDefaults.colors(checkedColor = FireCashPrimary)
                            )
                            Text(
                                text = if (mt.isCurrent) "${mt.label} (current)" else mt.label,
                                color = if (mt.isCurrent) FireCashOnSurfaceVariant else Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Up to 3 months can be compared at once",
                        color = FireCashOnSurfaceVariant,
                        fontSize = 11.sp
                    )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCompareDialog = false }) {
                    Text("Done", color = FireCashPrimary)
                }
            }
        )
    }
}

private val SLIP_DATE_FORMATS = listOf(
    "yyyy-MM-dd",
    "dd/MM/yyyy",
    "dd-MM-yyyy",
    "yyyy/MM/dd",
    "MM/dd/yyyy"
)

private fun normalizeDate(raw: String?, today: String): String {
    val s = raw?.trim().orEmpty()
    if (s.isBlank()) return today
    for (f in SLIP_DATE_FORMATS) {
        val d = runCatching { LocalDate.parse(s, DateTimeFormatter.ofPattern(f, Locale.US)) }.getOrNull()
        if (d != null) return d.toString()
    }
    return today
}

private data class MonthTotals(
    val key: String,
    val label: String,
    val income: Double,
    val expense: Double,
    val isCurrent: Boolean
)

@Composable
private fun PieChart(
    months: List<MonthTotals>,
    modifier: Modifier = Modifier
) {
    if (months.isEmpty()) return
    val current = months.first()
    val net = current.income - current.expense
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val d = minOf(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            val ringWidth = 36.dp.toPx()
            val r = d / 2f - ringWidth / 2f - 2.dp.toPx()
            val topLeft = Offset(center.x - r, center.y - r)
            val arcSize = Size(r * 2f, r * 2f)
            if (months.size == 1) {
                val m = months.first()
                val total = m.income + m.expense
                if (total > 0) {
                    val inSweep = ((m.income / total).coerceIn(0.0, 1.0) * 360f).toFloat()
                    if (inSweep > 0f) {
                        drawArc(
                            color = Color(0xFF66BB6A),
                            startAngle = -90f,
                            sweepAngle = inSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = ringWidth)
                        )
                    }
                    if (inSweep < 360f) {
                        drawArc(
                            color = Color(0xFFFF6B00),
                            startAngle = -90f + inSweep,
                            sweepAngle = 360f - inSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = ringWidth)
                        )
                    }
                }
            } else {
                val n = months.size
                val sector = 360f / n
                val gap = 2f
                months.forEachIndexed { index, m ->
                    val total = m.income + m.expense
                    if (total <= 0) return@forEachIndexed
                    val start = -90f + index * sector + gap / 2f
                    val sweep = sector - gap
                    val inSweep = sweep * (m.income / total).coerceIn(0.0, 1.0).toFloat()
                    if (inSweep > 0f) {
                        drawArc(
                            color = Color(0xFF66BB6A),
                            startAngle = start,
                            sweepAngle = inSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = ringWidth)
                        )
                    }
                    if (inSweep < sweep) {
                        drawArc(
                            color = Color(0xFFFF6B00),
                            startAngle = start + inSweep,
                            sweepAngle = sweep - inSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = ringWidth)
                        )
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Net · ${current.label.substringBefore(' ')}",
                color = FireCashOnSurfaceVariant,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "THB %.2f".format(Locale.US, net),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LegendRow(
    color: Color,
    label: String,
    amount: Double,
    total: Double
) {
    val pct = if (total > 0) (amount / total * 100) else 0.0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = FireCashOnSurfaceVariant, fontSize = 13.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "THB %.2f".format(Locale.US, amount),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "%.1f%%".format(Locale.US, pct),
            color = FireCashOnSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Column(
        modifier = modifier
            .background(FireCashSurfaceContainerLow, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = FireCashOnSurfaceVariant,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InsightRow(insight: SpendingInsight) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FireCashSurfaceContainerLow, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (insight.type) {
                InsightType.TREND -> Icons.Default.TrendingUp
                InsightType.ANOMALY -> Icons.Default.Warning
                InsightType.RECURRING -> Icons.Default.Repeat
                InsightType.BUDGET -> Icons.Default.AccountBalanceWallet
            },
            contentDescription = null,
            tint = when (insight.type) {
                InsightType.TREND -> Color(0xFF6366F1)
                InsightType.ANOMALY -> Color(0xFFEF5350)
                InsightType.RECURRING -> Color(0xFF66BB6A)
                InsightType.BUDGET -> FireCashPrimary
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = insight.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = insight.description,
                color = FireCashOnSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 2
            )
        }
    }
}

