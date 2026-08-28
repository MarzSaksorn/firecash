package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
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
import java.time.temporal.WeekFields
import java.util.Locale

private data class TimeEntry(val label: String, val total: Double)

private enum class TimeBucket { DAY, WEEK, MONTH }

@Composable
fun AnalyticsScreen(
    slips: List<SavedSlip>,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val expenses = remember(slips) {
        slips.mapNotNull { slip ->
            val amt = slip.amount ?: return@mapNotNull null
            Expense(
                merchant = slip.senderName ?: slip.receiverName ?: "Unknown",
                amount = amt,
                date = slip.date ?: "",
                time = slip.time ?: "",
                category = if (slip.isMoneyIn) "Income" else "Other"
            )
        }
    }
    val analytics = AnalyticsEngine.generateAnalytics(expenses)
    val totalSpent = analytics.totalSpent
    val changePct = analytics.changePercentage
    val avgPerDay = analytics.averagePerDay
    val insights = analytics.insights

    var selectedBucket by remember { mutableStateOf(TimeBucket.DAY) }
    val timeEntries = remember(expenses, selectedBucket) {
        computeEntries(expenses.filter { it.date.isNotBlank() }, selectedBucket)
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
                    text = "Spending Over Time",
                    color = FireCashOnSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeFilterChip(label = "Day", selected = selectedBucket == TimeBucket.DAY) {
                        selectedBucket = TimeBucket.DAY
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TimeFilterChip(label = "Week", selected = selectedBucket == TimeBucket.WEEK) {
                        selectedBucket = TimeBucket.WEEK
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TimeFilterChip(label = "Month", selected = selectedBucket == TimeBucket.MONTH) {
                        selectedBucket = TimeBucket.MONTH
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (timeEntries.isNotEmpty()) {
                StickChart(
                    data = timeEntries,
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
                        text = "No dated transactions yet",
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
private fun StickChart(
    data: List<TimeEntry>,
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
                if (count == 0) return@Canvas
                val stickWidth = 6.dp.toPx()
                val columnWidth = size.width / count
                val chartBottom = size.height - 4.dp.toPx()
                val chartHeight = chartBottom - 18.dp.toPx()
                val baselineColor = Color(0xFF2C3036)
                val stickColor = Color(0xFFFF6B00)

                drawLine(
                    color = baselineColor,
                    start = Offset(0f, chartBottom),
                    end = Offset(size.width, chartBottom),
                    strokeWidth = 2.dp.toPx()
                )

                data.forEachIndexed { index, entry ->
                    val stickHeight = if (maxAmount > 0) {
                        (entry.total / maxAmount * chartHeight).toFloat()
                    } else 0f
                    val centerX = columnWidth * index + columnWidth / 2f
                    val topY = chartBottom - stickHeight

                    drawLine(
                        color = stickColor,
                        start = Offset(centerX, chartBottom),
                        end = Offset(centerX, topY),
                        strokeWidth = stickWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    drawCircle(
                        color = stickColor,
                        radius = stickWidth / 2f,
                        center = Offset(centerX, topY)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
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

private fun computeEntries(
    expenses: List<Expense>,
    bucket: TimeBucket
): List<TimeEntry> {
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
        val label = when (bucket) {
            TimeBucket.DAY -> key.takeLast(5)
            TimeBucket.WEEK -> "W${key.takeLast(2)}"
            TimeBucket.MONTH -> {
                val d = LocalDate.parse("${key}-01", df)
                d.month.getDisplayName(TextStyle.SHORT, Locale.US)
            }
        }
        TimeEntry(label = label, total = list.sumOf { e -> e.amount })
    }.sortedBy { it.label }
}
