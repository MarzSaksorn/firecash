package com.example.data.export

import android.content.Context
import android.content.Intent
import com.example.data.model.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.StringWriter
import java.util.Locale

object ExportManager {

    suspend fun generateCsv(
        expenses: List<Expense>,
        fromDate: String,
        toDate: String
    ): String = withContext(Dispatchers.Default) {
        val writer = StringWriter()
        writer.append("ID,Date,Time,Merchant,Category,Amount,Currency,Tags,Status,CRC,Bank,TransactionRef\n")

        for (item in expenses) {
            val cleanMerchant = escapeCsv(item.merchant)
            val cleanCategory = escapeCsv(item.category)
            val cleanTags = escapeCsv(item.tags)
            val status = item.verificationStatus.name
            val crc = item.crc ?: ""
            val bank = item.sendingBank ?: ""
            val ref = item.transRef ?: ""

            writer.append("${item.id},${item.date},${item.time},\"$cleanMerchant\",\"$cleanCategory\",${String.format(Locale.US, "%.2f", item.amount)},${item.currency},\"$cleanTags\",$status,$crc,$bank,$ref\n")
        }

        writer.toString()
    }

    suspend fun generatePdfSummary(
        expenses: List<Expense>,
        fromDate: String,
        toDate: String,
        currencySymbol: String = "$"
    ): String = withContext(Dispatchers.Default) {
        val total = expenses.sumOf { it.amount }
        val categoryBreakdown = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        buildString {
            append("====================================================\n")
            append("           FIRECASH EXPENSE & SLIP REPORT           \n")
            append("====================================================\n")
            append("Export Date: 2026-08-26 | Scope: $fromDate to $toDate\n")
            append("Total Transactions: ${expenses.size}\n")
            append("Total Expenditure: $currencySymbol${String.format(Locale.US, "%,.2f", total)}\n")
            append("Verified Slips: ${expenses.count { it.isVerified }} / ${expenses.size}\n\n")

            append("--- CATEGORY BREAKDOWN ---\n")
            categoryBreakdown.forEach { (cat, sum) ->
                val pct = if (total > 0) (sum / total) * 100 else 0.0
                append("• ${cat.padEnd(20)}: $currencySymbol${String.format(Locale.US, "%8.2f", sum)} (${String.format(Locale.US, "%5.1f", pct)}%)\n")
            }
            append("\n")

            append("--- ITEM DETAILS ---\n")
            expenses.forEachIndexed { idx, item ->
                val statusTag = if (item.isVerified) "[VERIFIED]" else "[UNVERIFIED]"
                append("${(idx + 1).toString().padStart(2)}. ${item.date} | ${item.merchant.take(20).padEnd(20)} | $currencySymbol${String.format(Locale.US, "%8.2f", item.amount)} | ${item.category} $statusTag\n")
                if (!item.crc.isNullOrBlank()) {
                    append("    ↳ Bank Slip CRC: ${item.crc} | Ref: ${item.transRef ?: "N/A"}\n")
                }
            }
            append("====================================================\n")
            append("Generated offline via FireCash Local Storage Engine.\n")
        }
    }

    fun shareExport(context: Context, content: String, title: String, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
