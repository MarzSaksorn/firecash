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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private enum class AnalyticsFilter { DAY, WEEK, MONTH, YEAR }

private data class ChartPoint(
    val label: String,
    val income: Double,
    val outcome: Double
)

private data class ChartData(
    val points: List<ChartPoint>,
    val maxValue: Double
)

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

// ── Data aggregation ──────────────────────────────────────────

private fun computeDayData(expenses: List<Expense>, now: LocalDate): ChartData {
    val dayFmt = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    val points = (0 until 15).map { offset ->
        val day = now.minusDays(14L - offset)
        val ds = day.toString()
        val dayExp = expenses.filter { it.date == ds }
        ChartPoint(
            label = day.format(dayFmt),
            income = dayExp.filter { it.category == "Income" }.sumOf { it.amount },
            outcome = dayExp.filter { it.category != "Income" }.sumOf { it.amount }
        )
    }
    val mx = points.maxOfOrNull { maxOf(it.income, it.outcome) } ?: 0.0
    return ChartData(points, (mx * 1.25).coerceAtLeast(1.0))
}

private fun computeWeekData(expenses: List<Expense>, now: LocalDate): ChartData {
    val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val points = (0..6).map { i ->
        val day = monday.plusDays(i.toLong())
        val ds = day.toString()
        val dayExp = expenses.filter { it.date == ds }
        ChartPoint(
            label = labels[i],
            income = dayExp.filter { it.category == "Income" }.sumOf { it.amount },
            outcome = dayExp.filter { it.category != "Income" }.sumOf { it.amount }
        )
    }
    val mx = points.maxOfOrNull { maxOf(it.income, it.outcome) } ?: 0.0
    return ChartData(points, (mx * 1.25).coerceAtLeast(1.0))
}

private fun computeMonthData(expenses: List<Expense>, now: LocalDate): ChartData {
    val ym = YearMonth.from(now)
    val lastDay = ym.lengthOfMonth()
    val points = (1..lastDay).map { day ->
        val ds = ym.atDay(day).toString()
        val dayExp = expenses.filter { it.date == ds }
        ChartPoint(
            label = day.toString(),
            income = dayExp.filter { it.category == "Income" }.sumOf { it.amount },
            outcome = dayExp.filter { it.category != "Income" }.sumOf { it.amount }
        )
    }
    val mx = points.maxOfOrNull { maxOf(it.income, it.outcome) } ?: 0.0
    return ChartData(points, (mx * 1.25).coerceAtLeast(1.0))
}

private fun computeYearData(expenses: List<Expense>, now: LocalDate): ChartData {
    val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val yearStr = now.year.toString()
    val points = (1..12).map { m ->
        val prefix = "$yearStr-${m.toString().padStart(2, '0')}"
        val mExp = expenses.filter { it.date.startsWith(prefix) }
        ChartPoint(
            label = labels[m - 1],
            income = mExp.filter { it.category == "Income" }.sumOf { it.amount },
            outcome = mExp.filter { it.category != "Income" }.sumOf { it.amount }
        )
    }
    val mx = points.maxOfOrNull { maxOf(it.income, it.outcome) } ?: 0.0
    return ChartData(points, (mx * 1.25).coerceAtLeast(1.0))
}

// ── Filter helpers ─────────────────────────────────────────────

private fun filterExpensesForPeriod(
    expenses: List<Expense>,
    filter: AnalyticsFilter,
    now: LocalDate
): List<Expense> {
    return when (filter) {
        AnalyticsFilter.DAY -> {
            val start = now.minusDays(14)
            expenses.filter { e ->
                runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { d -> d in start..now } ?: false
            }
        }
        AnalyticsFilter.WEEK -> {
            val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val sunday = monday.plusDays(6)
            expenses.filter { e ->
                runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { d -> d in monday..sunday } ?: false
            }
        }
        AnalyticsFilter.MONTH -> {
            val ym = YearMonth.from(now)
            expenses.filter { e ->
                runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { d -> YearMonth.from(d) == ym } ?: false
            }
        }
        AnalyticsFilter.YEAR -> {
            val y = now.year
            expenses.filter { e -> e.date.startsWith(y.toString()) }
        }
    }
}

// ── Main screen ────────────────────────────────────────────────

@Composable
fun AnalyticsScreen(
    slips: List<SavedSlip>,
    knownNames: List<String> = emptyList(),
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val now = LocalDate.now()
    val todayStr = now.toString()

    val expenses = remember(slips, knownNames, now) {
        slips.mapNotNull { slip ->
            if (isSelfTransfer(slip, knownNames)) return@mapNotNull null
            val amt = slip.amount ?: return@mapNotNull null
            val effective = effectiveIsMoneyIn(slip, knownNames) ?: return@mapNotNull null
            Expense(
                merchant = slip.senderName ?: slip.receiverName ?: "Unknown",
                amount = amt,
                date = normalizeDate(slip.date, todayStr),
                time = slip.time ?: "",
                category = if (effective) "Income" else "Other"
            )
        }
    }

    var selectedFilter by remember { mutableStateOf(AnalyticsFilter.MONTH) }

    val chartData = remember(expenses, selectedFilter, now) {
        when (selectedFilter) {
            AnalyticsFilter.DAY -> computeDayData(expenses, now)
            AnalyticsFilter.WEEK -> computeWeekData(expenses, now)
            AnalyticsFilter.MONTH -> computeMonthData(expenses, now)
            AnalyticsFilter.YEAR -> computeYearData(expenses, now)
        }
    }

    val filteredExpenses = remember(expenses, selectedFilter, now) {
        filterExpensesForPeriod(expenses, selectedFilter, now)
    }

    val analytics = AnalyticsEngine.generateAnalytics(filteredExpenses)
    val totalSpent = analytics.totalSpent
    val changePct = analytics.changePercentage
    val avgPerDay = analytics.averagePerDay
    val insights = analytics.insights

    val monthKey = remember(now) { now.format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)) }
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

    val activeMonth = remember(availableMonths) { availableMonths.firstOrNull() }
    val incomeTotal = activeMonth?.income ?: 0.0
    val expenseTotal = activeMonth?.expense ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FireCashBackground)
            .statusBarsPadding()
    ) {
        // ── Header ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ── Stat cards ─────────────────────────────────
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

            // ── Chart card ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FireCashSurfaceContainerLow, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    // Header row
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
                        if (selectedFilter == AnalyticsFilter.MONTH) {
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
                                    Text(
                                        "Compare",
                                        color = FireCashPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter tabs (Week | Month | Year)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FireCashSurfaceContainerHigh.copy(alpha = 0.5f))
                            .padding(2.dp)
                    ) {
                        listOf(
                            AnalyticsFilter.DAY to "Day",
                            AnalyticsFilter.WEEK to "Week",
                            AnalyticsFilter.MONTH to "Month",
                            AnalyticsFilter.YEAR to "Year"
                        ).forEach { (filter, label) ->
                            val isActive = filter == selectedFilter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isActive) FireCashSurfaceContainerHighest else Color.Transparent)
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isActive) Color.White else FireCashOnSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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

                    // Chart
                    val hasData = chartData.points.any { it.income > 0 || it.outcome > 0 }
                    if (hasData) {
                        when (selectedFilter) {
                            AnalyticsFilter.DAY, AnalyticsFilter.WEEK, AnalyticsFilter.MONTH -> {
                                LineChart(
                                    data = chartData,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                            AnalyticsFilter.YEAR -> {
                                YearBarChart(
                                    data = chartData,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    availableMonths.isEmpty() -> "No dated transactions yet"
                                    else -> "No transactions in this period"
                                },
                                color = FireCashOnSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend rows (monthly income/outcome summary)
                    if (hasData) {
                        val totalInc = chartData.points.sumOf { it.income }
                        val totalOut = chartData.points.sumOf { it.outcome }
                        val total = totalInc + totalOut
                        LegendRow(
                            color = Color(0xFF2B66FF),
                            label = "Income · ${selectedFilter.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            amount = totalInc,
                            total = total
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LegendRow(
                            color = Color(0xFF7DD3FC),
                            label = "Outcome · ${selectedFilter.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            amount = totalOut,
                            total = total
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── AI Insights ────────────────────────────────
            if (insights.isNotEmpty()) {
                Text(
                    text = "AI Insights",
                    color = FireCashOnSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    insights.forEach { insight ->
                        InsightRow(insight = insight)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
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
    }

    // ── Compare dialog ────────────────────────────────────────
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

// ── Chart composables ──────────────────────────────────────────

/**
 * Compute a visually pleasant round step for Y-axis grid labels.
 * e.g. maxVal=512 → step=100, so labels: 0, 100, 200, 300, 400, 500, 600
 */
private fun computeNiceStep(maxVal: Double): Double {
    val targetSteps = 5
    if (maxVal <= 0) return 1.0
    val rawStep = maxVal / targetSteps
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(rawStep)))
    val residual = rawStep / magnitude
    val niceStep = when {
        residual < 1.5 -> magnitude
        residual < 3.5 -> 2 * magnitude
        residual < 7.5 -> 5 * magnitude
        else -> 10 * magnitude
    }
    return niceStep
}


private val incomeColor = Color(0xFF2B66FF)
private val outcomeColor = Color(0xFF7DD3FC)
private val gridColor = Color(0xFF2A2D35)

/**
 * Line chart with mild cubic-bezier smoothing ("smoothen out but not much").
 * Control points are offset 35% along X from each point, keeping the Y horizontal,
 * so lines curve gently without overshooting.
 */
@Composable
private fun LineChart(
    data: ChartData,
    modifier: Modifier = Modifier
) {
    val points = data.points
    val maxVal = data.maxValue
    if (points.size < 2 || maxVal <= 0) return

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padL = 40.dp.toPx()
            val padR = 8.dp.toPx()
            val padT = 8.dp.toPx()
            val padB = 28.dp.toPx()
            val cw = size.width - padL - padR
            val ch = size.height - padT - padB
            if (cw <= 0 || ch <= 0) return@Canvas

            val stepX = cw / (points.size - 1).coerceAtLeast(1)

            // Compute nice round grid step
            val niceStep = computeNiceStep(maxVal)
            val niceMax = Math.ceil(maxVal / niceStep) * niceStep
            val gridCount = (niceMax / niceStep).toInt().coerceAtLeast(1)

            // Grid lines + Y-axis labels (THB scale)
            val nativeCanvas = drawContext.canvas.nativeCanvas
            val labelPaint = android.graphics.Paint().apply {
                color = 0xFF6B7280.toInt()
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            for (i in 0..gridCount) {
                val thb = i * niceStep
                val fraction = (thb / niceMax).toFloat()
                val y = padT + ch * (1f - fraction)
                drawLine(gridColor, Offset(padL, y), Offset(padL + cw, y), strokeWidth = 1f)
                val thbLabel = if (thb >= 1000) "THB %.0f".format(Locale.US, thb)
                               else "THB %.2f".format(Locale.US, thb)
                nativeCanvas.drawText(thbLabel, padL - 6.dp.toPx(), y + 3.dp.toPx(), labelPaint)
            }

            // Build line points
            val incPoints = points.mapIndexed { i, p ->
                Offset(
                    padL + i * stepX,
                    padT + ch * (1f - (p.income / maxVal).toFloat()).coerceIn(0f, 1f)
                )
            }
            val outPoints = points.mapIndexed { i, p ->
                Offset(
                    padL + i * stepX,
                    padT + ch * (1f - (p.outcome / maxVal).toFloat()).coerceIn(0f, 1f)
                )
            }

            // Draw mild-smooth lines
            drawSmoothLine(incPoints, incomeColor)
            drawSmoothLine(outPoints, outcomeColor)

            // X-axis labels — show subset to avoid crowding
            val maxLabels = 8f
            val labelStep = (points.size / maxLabels).toInt().coerceAtLeast(1)
            val xLabelPaint = android.graphics.Paint().apply {
                color = 0xFF6B7280.toInt()
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = false
            }
            points.forEachIndexed { i, p ->
                if (i % labelStep == 0 || i == points.size - 1) {
                    val x = padL + i * stepX
                    nativeCanvas.drawText(p.label, x, size.height - 4.dp.toPx(), xLabelPaint)
                }
            }
        }
    }
}

private fun DrawScope.drawSmoothLine(points: List<Offset>, color: Color) {
    if (points.size < 2) return
    val path = Path()
    path.moveTo(points[0].x, points[0].y)

    for (i in 0 until points.size - 1) {
        val cur = points[i]
        val nxt = points[i + 1]
        val dx = nxt.x - cur.x
        // Mild cubic bezier: leave horizontally toward next, arrive horizontally from prev
        // 0.35f gives gentle smoothing without overshoot
        path.cubicTo(
            cur.x + dx * 0.35f, cur.y,
            nxt.x - dx * 0.35f, nxt.y,
            nxt.x, nxt.y
        )
    }

    drawPath(
        path,
        color = color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // Dots at each data point
    points.forEach { pt ->
        drawCircle(color = color, radius = 3.dp.toPx(), center = pt)
    }
}

/**
 * Vertical grouped bar chart for Year view — each month has an income bar + outcome bar.
 */
@Composable
private fun YearBarChart(
    data: ChartData,
    modifier: Modifier = Modifier
) {
    val points = data.points
    val maxVal = data.maxValue
    if (points.isEmpty() || maxVal <= 0) return

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padL = 32.dp.toPx()
            val padR = 8.dp.toPx()
            val padT = 8.dp.toPx()
            val padB = 28.dp.toPx()
            val cw = size.width - padL - padR
            val ch = size.height - padT - padB
            if (cw <= 0 || ch <= 0) return@Canvas

            val barGroupWidth = cw / points.size
            val barWidth = (barGroupWidth * 0.35f).coerceAtMost(16.dp.toPx())
            val gap = (barGroupWidth - barWidth * 2) / 3f

            // Compute nice round grid step
            val niceStep = computeNiceStep(maxVal)
            val niceMax = Math.ceil(maxVal / niceStep) * niceStep
            val gridCount = (niceMax / niceStep).toInt().coerceAtLeast(1)

            // Grid lines + Y-axis labels (THB scale)
            val nativeCanvas = drawContext.canvas.nativeCanvas
            val labelPaint = android.graphics.Paint().apply {
                color = 0xFF6B7280.toInt()
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            for (i in 0..gridCount) {
                val thb = i * niceStep
                val fraction = (thb / niceMax).toFloat()
                val y = padT + ch * (1f - fraction)
                drawLine(gridColor, Offset(padL, y), Offset(padL + cw, y), strokeWidth = 1f)
                val thbLabel = if (thb >= 1000) "THB %.0f".format(Locale.US, thb)
                               else "THB %.2f".format(Locale.US, thb)
                nativeCanvas.drawText(thbLabel, padL - 6.dp.toPx(), y + 3.dp.toPx(), labelPaint)
            }

            // Bars + X labels
            val xLabelPaint = android.graphics.Paint().apply {
                color = 0xFF6B7280.toInt()
                textSize = 8.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            points.forEachIndexed { i, p ->
                val gx = padL + i * barGroupWidth
                val incH = (ch * (p.income / maxVal).toFloat()).coerceAtMost(ch)
                val outH = (ch * (p.outcome / maxVal).toFloat()).coerceAtMost(ch)

                // Income bar (left)
                drawRect(
                    color = incomeColor,
                    topLeft = Offset(gx + gap, padT + ch - incH),
                    size = androidx.compose.ui.geometry.Size(barWidth, incH)
                )
                // Outcome bar (right)
                drawRect(
                    color = outcomeColor,
                    topLeft = Offset(gx + gap * 2 + barWidth, padT + ch - outH),
                    size = androidx.compose.ui.geometry.Size(barWidth, outH)
                )
                // X label
                nativeCanvas.drawText(
                    p.label,
                    gx + barGroupWidth / 2f,
                    size.height - 4.dp.toPx(),
                    xLabelPaint
                )
            }
        }
    }
}

// ── Shared UI components ───────────────────────────────────────

private data class MonthTotals(
    val key: String,
    val label: String,
    val income: Double,
    val expense: Double,
    val isCurrent: Boolean
)

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