package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashSurfaceContainerLow
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private data class MoneyEntry(
    val sortKey: String,
    val label: String,
    val inTotal: Double,
    val outTotal: Double
)

private enum class TimeBucket { DAY, WEEK, MONTH }

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

private data class ChartState(
    val entries: List<MoneyEntry>,
    val label: String,
    val atLatest: Boolean
)

private fun latestWindowStart(bucket: TimeBucket, maxDate: LocalDate): LocalDate = when (bucket) {
    TimeBucket.DAY -> maxDate
    TimeBucket.WEEK -> {
        val wf = WeekFields.of(Locale.US)
        val diff = maxDate.dayOfWeek.value - wf.firstDayOfWeek.value
        maxDate.minusDays(if (diff < 0) (diff + 7).toLong() else diff.toLong())
    }
    TimeBucket.MONTH -> maxDate.withDayOfMonth(1)
}

private fun windowLength(bucket: TimeBucket): Period = when (bucket) {
    TimeBucket.DAY -> Period.ofDays(7)
    TimeBucket.WEEK -> Period.ofDays(42)
    TimeBucket.MONTH -> Period.ofMonths(12)
}

private fun windowLabel(bucket: TimeBucket, start: LocalDate): String {
    val short = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    val end = start.plus(windowLength(bucket)).minusDays(1)
    return when (bucket) {
        TimeBucket.DAY, TimeBucket.WEEK -> "${start.format(short)} – ${end.format(short)}"
        TimeBucket.MONTH -> {
            val ym = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
            "${start.format(ym)} – ${end.format(ym)}"
        }
    }
}

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
    val expenses = remember(slips, knownNames) {
        slips.mapNotNull { slip ->
            if (isSelfTransfer(slip, knownNames)) return@mapNotNull null
            val amt = slip.amount ?: return@mapNotNull null
            val effective = effectiveIsMoneyIn(slip, knownNames) ?: return@mapNotNull null
            Expense(
                merchant = slip.senderName ?: slip.receiverName ?: "Unknown",
                amount = amt,
                date = slip.date ?: "",
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

    var selectedBucket by remember { mutableStateOf(TimeBucket.DAY) }
    var windowStart by remember { mutableStateOf<LocalDate?>(null) }
    val maxDate = remember(expenses) {
        expenses.mapNotNull { runCatching { LocalDate.parse(it.date, DATE_FORMATTER) }.getOrNull() }.maxOrNull()
    }
    val chartState = remember(expenses, selectedBucket, windowStart) {
        val dated = expenses.filter { it.date.isNotBlank() }
        val max = dated.mapNotNull { runCatching { LocalDate.parse(it.date, DATE_FORMATTER) }.getOrNull() }.maxOrNull()
        if (max == null) null
        else {
            val latest = latestWindowStart(selectedBucket, max)
            val start = windowStart ?: latest
            val endExclusive = start.plus(windowLength(selectedBucket))
            val inWindow = dated.filter {
                val d = runCatching { LocalDate.parse(it.date, DATE_FORMATTER) }.getOrNull() ?: return@filter false
                !d.isBefore(start) && d.isBefore(endExclusive)
            }
            ChartState(
                entries = computeMoneyEntries(inWindow, selectedBucket),
                label = windowLabel(selectedBucket, start),
                atLatest = start == latest
            )
        }
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
                    TimeFilterChip(label = "Day", selected = selectedBucket == TimeBucket.DAY) {
                        selectedBucket = TimeBucket.DAY
                        windowStart = null
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TimeFilterChip(label = "Week", selected = selectedBucket == TimeBucket.WEEK) {
                        selectedBucket = TimeBucket.WEEK
                        windowStart = null
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TimeFilterChip(label = "Month", selected = selectedBucket == TimeBucket.MONTH) {
                        selectedBucket = TimeBucket.MONTH
                        windowStart = null
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (chartState != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val m = maxDate ?: return@IconButton
                            val latest = latestWindowStart(selectedBucket, m)
                            val current = windowStart ?: latest
                            windowStart = when (selectedBucket) {
                                TimeBucket.DAY -> current.minusDays(7)
                                TimeBucket.WEEK -> current.minusDays(42)
                                TimeBucket.MONTH -> current.minusMonths(12)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Earlier period",
                            tint = FireCashPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = chartState.label,
                        color = FireCashOnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val m = maxDate ?: return@IconButton
                            val latest = latestWindowStart(selectedBucket, m)
                            val current = windowStart ?: latest
                            val next = when (selectedBucket) {
                                TimeBucket.DAY -> current.plusDays(7)
                                TimeBucket.WEEK -> current.plusDays(42)
                                TimeBucket.MONTH -> current.plusMonths(12)
                            }
                            windowStart = if (!next.isBefore(latest)) null else next
                        },
                        enabled = !chartState.atLatest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Later period",
                            tint = if (chartState.atLatest) FireCashOnSurfaceVariant.copy(alpha = 0.3f) else FireCashPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF66BB6A))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = "Money In", color = FireCashOnSurfaceVariant, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF6B00))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = "Money Out", color = FireCashOnSurfaceVariant, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (chartState != null && chartState.entries.isNotEmpty()) {
                DualStickChart(
                    data = chartState.entries,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chartState == null) "No dated transactions yet" else "No transactions in this period",
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
}

@Composable
private fun TimeFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = FireCashPrimary,
            containerColor = FireCashSurfaceContainerLow
        ),
        modifier = Modifier.height(32.dp)
    )
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
private fun DualStickChart(
    data: List<MoneyEntry>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val maxIn = data.maxOfOrNull { it.inTotal } ?: 0.0
            val maxOut = data.maxOfOrNull { it.outTotal } ?: 0.0
            val maxAmount = maxOf(maxIn, maxOut)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = data.size
                if (count == 0 || maxAmount <= 0) return@Canvas
                val axisPad = 30.dp.toPx()
                val plotWidth = size.width - axisPad
                val columnWidth = plotWidth / count
                val gapPx = 3.dp.toPx()
                val stickPx = ((columnWidth - 2 * gapPx) / 2f).coerceIn(4.dp.toPx(), 10.dp.toPx())
                val chartBottom = size.height - 4.dp.toPx()
                val chartTop = 22.dp.toPx()
                val chartHeight = chartBottom - chartTop
                val baselineColor = Color(0xFF2C3036)
                val inColor = Color(0xFF66BB6A)
                val outColor = Color(0xFFFF6B00)

                drawLine(
                    color = baselineColor,
                    start = Offset(axisPad, chartBottom),
                    end = Offset(size.width, chartBottom),
                    strokeWidth = 2.dp.toPx()
                )

                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(0x8B, 0x91, 0x99)
                    textSize = 9.dp.toPx()
                }
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val y = chartBottom - chartHeight * i / gridSteps
                    drawLine(
                        color = Color(0xFF2A2E35),
                        start = Offset(axisPad, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        compactAmount(maxAmount * i / gridSteps),
                        2.dp.toPx(),
                        y - 3.dp.toPx(),
                        gridPaint
                    )
                }

                data.forEachIndexed { index, entry ->
                    val centerX = axisPad + columnWidth * index + columnWidth / 2f
                    val inH = (entry.inTotal / maxAmount * chartHeight).toFloat()
                    val outH = (entry.outTotal / maxAmount * chartHeight).toFloat()
                    val inX = centerX - gapPx / 2f - stickPx
                    val outX = centerX + gapPx / 2f

                    if (inH > 0) {
                        drawLine(
                            color = inColor,
                            start = Offset(inX, chartBottom),
                            end = Offset(inX, chartBottom - inH),
                            strokeWidth = stickPx,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        drawCircle(color = inColor, radius = stickPx / 2f, center = Offset(inX, chartBottom - inH))
                    }
                    if (outH > 0) {
                        drawLine(
                            color = outColor,
                            start = Offset(outX, chartBottom),
                            end = Offset(outX, chartBottom - outH),
                            strokeWidth = stickPx,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        drawCircle(color = outColor, radius = stickPx / 2f, center = Offset(outX, chartBottom - outH))
                    }
                }

                val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 9.dp.toPx()
                    isFakeBoldText = true
                }
                fun drawValueLabel(text: String, barCenterX: Float, topY: Float, color: Int) {
                    valuePaint.color = color
                    val textW = valuePaint.measureText(text)
                    var x = barCenterX - textW / 2f
                    if (x < axisPad) x = axisPad
                    val maxX = size.width - textW - 2.dp.toPx()
                    if (x > maxX) x = maxX.coerceAtLeast(axisPad)
                    drawContext.canvas.nativeCanvas.drawText(text, x, topY, valuePaint)
                }
                val maxInEntry = data.maxByOrNull { it.inTotal }?.takeIf { it.inTotal > 0 }
                if (maxInEntry != null) {
                    val idx = data.indexOf(maxInEntry)
                    val centerX = axisPad + columnWidth * idx + columnWidth / 2f
                    val inX = centerX - gapPx / 2f - stickPx
                    val h = (maxInEntry.inTotal / maxAmount * chartHeight).toFloat()
                    drawValueLabel(
                        compactAmount(maxInEntry.inTotal),
                        inX + stickPx / 2f,
                        chartBottom - h - 3.dp.toPx(),
                        android.graphics.Color.rgb(0x66, 0xBB, 0x6A)
                    )
                }
                val maxOutEntry = data.maxByOrNull { it.outTotal }?.takeIf { it.outTotal > 0 }
                if (maxOutEntry != null) {
                    val idx = data.indexOf(maxOutEntry)
                    val centerX = axisPad + columnWidth * idx + columnWidth / 2f
                    val outX = centerX + gapPx / 2f
                    val h = (maxOutEntry.outTotal / maxAmount * chartHeight).toFloat()
                    drawValueLabel(
                        compactAmount(maxOutEntry.outTotal),
                        outX + stickPx / 2f,
                        chartBottom - h - 3.dp.toPx(),
                        android.graphics.Color.rgb(0xFF, 0x6B, 0x00)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(30.dp))
            data.forEach { entry ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                    Text(
                        text = entry.label,
                        color = FireCashOnSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun compactAmount(v: Double): String = when {
    v >= 1_000_000 -> "%.1fM".format(Locale.US, v / 1_000_000)
    v >= 1_000 -> "%.1fk".format(Locale.US, v / 1_000)
    else -> "%.0f".format(Locale.US, v)
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

private fun computeMoneyEntries(
    expenses: List<Expense>,
    bucket: TimeBucket
): List<MoneyEntry> {
    if (expenses.isEmpty()) return emptyList()
    val df = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    val grouped = when (bucket) {
        TimeBucket.DAY -> expenses.groupBy { it.date }
        TimeBucket.WEEK -> expenses.groupBy {
            val d = LocalDate.parse(it.date, df)
            val week = d.get(WeekFields.of(Locale.US).weekOfYear())
            "${d.year}-W${week.toString().padStart(2, '0')}"
        }
        TimeBucket.MONTH -> expenses.groupBy {
            val d = LocalDate.parse(it.date, df)
            "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
        }
    }
    return grouped.map { (key, list) ->
        val (sortKey, label) = when (bucket) {
            TimeBucket.DAY -> key to key.takeLast(5)
            TimeBucket.WEEK -> key to "W${key.takeLast(2)}"
            TimeBucket.MONTH -> {
                val d = LocalDate.parse("$key-01", df)
                key to d.month.getDisplayName(TextStyle.SHORT, Locale.US)
            }
        }
        MoneyEntry(
            sortKey = sortKey,
            label = label,
            inTotal = list.filter { it.category == "Income" }.sumOf { e -> e.amount },
            outTotal = list.filter { it.category != "Income" }.sumOf { e -> e.amount }
        )
    }.sortedBy { it.sortKey }
}
