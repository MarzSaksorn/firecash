package com.example.data.backup

import android.content.Context
import com.example.data.model.Expense
import com.example.data.model.KeywordRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupSnapshot(
    val timestamp: String,
    val totalRecords: Int,
    val expenses: List<Expense>,
    val rules: List<KeywordRule>,
    val appVersion: String = "1.0.0"
)

class DriveBackupManager(private val context: Context? = null) {

    suspend fun createBackupJson(
        expenses: List<Expense>,
        rules: List<KeywordRule>
    ): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "FireCash")
        root.put("version", "1.0")
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        root.put("totalExpenses", expenses.size)

        val expensesArray = JSONArray()
        for (e in expenses) {
            val item = JSONObject().apply {
                put("id", e.id)
                put("merchant", e.merchant)
                put("amount", e.amount)
                put("date", e.date)
                put("time", e.time)
                put("category", e.category)
                put("tags", e.tags)
                put("imageUrl", e.imageUrl ?: "")
                put("isVerified", e.isVerified)
                put("crc", e.crc ?: "")
                put("sendingBank", e.sendingBank ?: "")
                put("transRef", e.transRef ?: "")
                put("currency", e.currency)
                put("verificationStatus", e.verificationStatus.name)
            }
            expensesArray.put(item)
        }
        root.put("expenses", expensesArray)

        val rulesArray = JSONArray()
        for (r in rules) {
            val item = JSONObject().apply {
                put("keyword", r.keyword)
                put("category", r.category)
            }
            rulesArray.put(item)
        }
        root.put("rules", rulesArray)

        root.toString(2)
    }

    suspend fun saveBackupToFile(jsonContent: String): File? = withContext(Dispatchers.IO) {
        context?.let { ctx ->
            try {
                val dir = File(ctx.filesDir, "backups")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "firecash_backup_${System.currentTimeMillis()}.json")
                FileOutputStream(file).use { out ->
                    out.write(jsonContent.toByteArray())
                }
                return@withContext file
            } catch (e: Exception) {
                null
            }
        }
        null
    }
}
