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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

    val now = LocalDate.now()
    val monthKey = remember(now) { now.format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)) }
    val monthLabel = remember(now) {
        "${now.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${now.year}"
    }
    val monthIncome = remember(expenses, monthKey) {
        expenses.filter { it.date.startsWith(monthKey) && it.category == "Income" }.sumOf { e -> e.amount }
    }
    val monthExpense = remember(expenses, monthKey) {
        expenses.filter { it.date.startsWith(monthKey) && it.category != "Income" }.sumOf { e -> e.amount }
    }
    val monthlyBars = remember(expenses, now) {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
        (5L downTo 0L).map { i ->
            val m = now.minusMonths(i)
            val key = m.format(fmt)
            MonthlyBar(
                label = m.month.getDisplayName(TextStyle.SHORT, Locale.US),
                total = expenses.filter { it.date.startsWith(key) && it.category != "Income" }.sumOf { e -> e.amount }
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
                Text(
                    text = monthLabel,
                    color = FireCashPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (monthIncome + monthExpense > 0) {
                PieChart(
                    income = monthIncome,
                    expense = monthExpense,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LegendRow(
                    color = Color(0xFF66BB6A),
                    label = "Money In",
                    amount = monthIncome,
                    total = monthIncome + monthExpense
                )
                Spacer(modifier = Modifier.height(8.dp))
                LegendRow(
                    color = Color(0xFFFF6B00),
                    label = "Money Out",
                    amount = monthExpense,
                    total = monthIncome + monthExpense
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions this month yet",
                        color = FireCashOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Monthly Expenses",
                color = FireCashOnSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last 6 months",
                color = FireCashOnSurfaceVariant,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (monthlyBars.any { it.total > 0 }) {
                MonthlyStickChart(
                    data = monthlyBars,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expense data yet",
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
private fun PieChart(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val total = income + expense
    if (total <= 0) return
    val inFraction = (income / total).coerceIn(0.0, 1.0)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 36.dp.toPx()
            val inset = strokeWidth / 2f + 2.dp.toPx()
            val side = minOf(size.width, size.height) - inset * 2
            val arcSize = Size(side, side)
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            val startAngle = -90f
            val inSweep = (inFraction * 360f).toFloat()
            if (inSweep > 0f) {
                drawArc(
                    color = Color(0xFF66BB6A),
                    startAngle = startAngle,
                    sweepAngle = inSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            }
            if (inSweep < 360f) {
                drawArc(
                    color = Color(0xFFFF6B00),
                    startAngle = startAngle + inSweep,
                    sweepAngle = 360f - inSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Net",
                color = FireCashOnSurfaceVariant,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "THB %.2f".format(Locale.US, income - expense),
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

private data class MonthlyBar(val label: String, val total: Double)

@Composable
private fun MonthlyStickChart(
    data: List<MonthlyBar>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val maxAmount = data.maxOfOrNull { it.total } ?: 0.0
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = data.size
                if (count == 0 || maxAmount <= 0) return@Canvas
                val axisPad = 30.dp.toPx()
                val plotWidth = size.width - axisPad
                val columnWidth = plotWidth / count
                val gapPx = 3.dp.toPx()
                val stickPx = ((columnWidth - 2 * gapPx) / 2f).coerceIn(4.dp.toPx(), 10.dp.toPx())
                val chartBottom = size.height - 4.dp.toPx()
                val chartTop = 20.dp.toPx()
                val chartHeight = chartBottom - chartTop
                val baselineColor = Color(0xFF2C3036)
                val barColor = Color(0xFFFF6B00)
                val currentColor = Color(0xFFFF8A3D)

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

                data.forEachIndexed { index, bar ->
                    if (bar.total <= 0) return@forEachIndexed
                    val x = axisPad + columnWidth * index + columnWidth / 2f
                    val h = (bar.total / maxAmount * chartHeight).toFloat()
                    val color = if (index == data.lastIndex) currentColor else barColor
                    drawLine(
                        color = color,
                        start = Offset(x, chartBottom),
                        end = Offset(x, chartBottom - h),
                        strokeWidth = stickPx,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawCircle(color = color, radius = stickPx / 2f, center = Offset(x, chartBottom - h))
                }

                val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 9.dp.toPx()
                    isFakeBoldText = true
                }
                val maxBar = data.maxByOrNull { it.total }?.takeIf { it.total > 0 }
                if (maxBar != null) {
                    val idx = data.indexOf(maxBar)
                    val x = axisPad + columnWidth * idx + columnWidth / 2f
                    val h = (maxBar.total / maxAmount * chartHeight).toFloat()
                    val text = compactAmount(maxBar.total)
                    val textW = valuePaint.measureText(text)
                    var tx = x - textW / 2f
                    if (tx < axisPad) tx = axisPad
                    val maxX = size.width - textW - 2.dp.toPx()
                    if (tx > maxX) tx = maxX.coerceAtLeast(axisPad)
                    valuePaint.color = android.graphics.Color.rgb(0xFF, 0x8A, 0x3D)
                    drawContext.canvas.nativeCanvas.drawText(text, tx, chartBottom - h - 3.dp.toPx(), valuePaint)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(30.dp))
            data.forEach { bar ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                    Text(
                        text = bar.label,
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

