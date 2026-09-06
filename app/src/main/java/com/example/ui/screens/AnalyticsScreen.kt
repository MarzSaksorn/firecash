package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
import com.example.ui.theme.FireCashError
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashSecondary
import com.example.ui.theme.FireCashSurfaceContainerHigh
import com.example.ui.theme.FireCashSurfaceContainerHighest
import com.example.ui.theme.FireCashSurfaceContainerLow
import java.time.LocalDate
import java.time.YearMonth
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
    // Manual override takes precedence
    when (slip.manualCategory) {
        "income" -> return true
        "expense" -> return false
        "transfer" -> return null
    }
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
                val ym = runCatching { YearMonth.parse(key, fmt) }.getOrNull() ?: return@mapNotNull null
                MonthTotals(
                    key = key,
                    label = "${ym.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${ym.year}",
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

    val activeMonth = remember(availableMonths) {
        availableMonths.firstOrNull()
    }
    val incomeTotal = activeMonth?.income ?: 0.0
    val expenseTotal = activeMonth?.expense ?: 0.0
    val maxBarValue = remember(incomeTotal, expenseTotal) {
        (maxOf(incomeTotal, expenseTotal) * 1.3).coerceAtLeast(1.0)
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
                text = "Transactions",
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
                valueColor = if (changePct >= 0) FireCashError else FireCashSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Transaction Overview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FireCashSurfaceContainerLow, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Overview",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeMonth?.label?.substringBefore(' ') ?: "",
                            color = FireCashOnSurfaceVariant,
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

                // Legend tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FireCashSurfaceContainerHigh.copy(alpha = 0.5f))
                        .padding(2.dp)
                ) {
                    listOf("Week", "Month", "Year").forEach { tab ->
                        val isActive = tab == "Month"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isActive) FireCashSurfaceContainerHighest else Color.Transparent)
                                .clickable { /* Month is default */ }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tab,
                                color = if (isActive) Color.White else FireCashOnSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Legend indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2B66FF))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Income", color = FireCashOnSurfaceVariant, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7DD3FC))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Outcome", color = FireCashOnSurfaceVariant, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Bar chart
                if (incomeTotal + expenseTotal > 0) {
                    BarChart(
                        income = incomeTotal,
                        expense = expenseTotal,
                        maxValue = maxBarValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (availableMonths.isEmpty()) "No dated transactions yet" else "No transactions in this month",
                            color = FireCashOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Legend rows
                if (incomeTotal + expenseTotal > 0) {
                    LegendRow(
                        color = Color(0xFF2B66FF),
                        label = "Income · ${activeMonth?.label?.substringBefore(' ') ?: ""}",
                        amount = incomeTotal,
                        total = incomeTotal + expenseTotal
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LegendRow(
                        color = Color(0xFF7DD3FC),
                        label = "Outcome · ${activeMonth?.label?.substringBefore(' ') ?: ""}",
                        amount = expenseTotal,
                        total = incomeTotal + expenseTotal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
private fun BarChart(
    income: Double,
    expense: Double,
    maxValue: Double,
    modifier: Modifier = Modifier
) {
    val barCount = 6
    val labels = listOf("JAN", "MAR", "MAY", "JUL", "AUG", "DEC")
    val gridLines = listOf(0.0, 0.25, 0.5, 0.75, 1.0)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartLeft = 0f
            val chartRight = size.width
            val chartTop = 0f
            val chartBottom = size.height - 20.dp.toPx()
            val chartHeight = chartBottom - chartTop

            // Horizontal grid lines
            gridLines.forEach { fraction ->
                val y = chartBottom - (chartHeight * fraction).toFloat()
                drawLine(
                    color = Color(0xFF2A2D35),
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1f
                )
            }

            // Bar dimensions
            val barWidth = (chartRight - chartLeft) / barCount * 0.5f
            val gap = (chartRight - chartLeft) / barCount
            val barStartX = chartLeft + gap * 0.25f

            // X-axis labels + percentage labels via drawIntoCanvas
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                gridLines.forEach { fraction ->
                    val y = chartBottom - (chartHeight * fraction).toFloat()
                    val pctLabel = "${(fraction * 100).toInt()}%"
                    nativeCanvas.drawText(
                        pctLabel,
                        chartLeft + 4.dp.toPx(),
                        y - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = 0xFF6B7280.toInt()
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }

                // X-axis labels
                labels.forEachIndexed { index, label ->
                    val x = barStartX + index * gap + barWidth
                    nativeCanvas.drawText(
                        label,
                        x,
                        size.height - 2.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = if (label == "AUG") 0xFF2B66FF.toInt() else 0xFF6B7280.toInt()
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = label == "AUG"
                        }
                    )
                }
            }

            // Income bars (blue #2B66FF)
            val incomeHeight = (chartHeight * (income / maxValue)).toFloat().coerceAtMost(chartHeight)
            drawRect(
                color = Color(0xFF2B66FF),
                topLeft = Offset(barStartX, chartBottom - incomeHeight),
                size = Size(barWidth, incomeHeight),
                style = Fill
            )

            // Expense bars (light blue #7DD3FC)
            val expenseHeight = (chartHeight * (expense / maxValue)).toFloat().coerceAtMost(chartHeight)
            drawRect(
                color = Color(0xFF7DD3FC),
                topLeft = Offset(barStartX + gap, chartBottom - expenseHeight),
                size = Size(barWidth, expenseHeight),
                style = Fill
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
                InsightType.ANOMALY -> FireCashError
                InsightType.RECURRING -> FireCashSecondary
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

