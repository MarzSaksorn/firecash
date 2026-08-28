package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.model.SavedSlip
import com.example.data.model.VerificationStatus
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IncomeNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        // Ignore our own app notifications
        if (sbn.packageName == packageName) return
        val prefs = getSharedPreferences("firecash_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PREFS_NOTIFICATION_INCOME, false)) return
        // Whitelist: if non-empty, only listed packages are processed
        val whitelist = loadWhitelist(prefs)
        if (whitelist.isNotEmpty() && sbn.packageName !in whitelist) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val combined = listOf(title, text, bigText).filter { it.isNotBlank() }.joinToString(" ").trim()
        if (combined.isBlank()) return

        val amount = extractFirstNumber(combined) ?: return
        saveIncomeFromNotification(this, amount, title, combined, sbn.packageName)
    }

    companion object {
        const val PREFS_NOTIFICATION_INCOME = "notification_income_enabled"
        const val PREFS_NOTIFICATION_WHITELIST = "notification_whitelist"
        private const val PREFS_SLIPS = "saved_slips"
        private const val PREFS_SEEN = "seen_payloads"

        // Regex: first number with optional commas/decimals, handles 1,234.56, ฿ 1,200 etc.
        private val AMOUNT_REGEX = Regex("""[-+]?\d{1,3}(?:,\d{3})*(?:\.\d+)?|\d+(?:\.\d+)?""")

        fun extractFirstNumber(text: String): Double? {
            val match = AMOUNT_REGEX.find(text) ?: return null
            val raw = match.value.replace(",", "")
            return raw.toDoubleOrNull()
        }

        fun isEnabled(context: Context): Boolean {
            return context.getSharedPreferences("firecash_settings", Context.MODE_PRIVATE)
                .getBoolean(PREFS_NOTIFICATION_INCOME, false)
        }

        fun hasPermission(context: Context): Boolean {
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(context.packageName)
        }

        fun saveIncomeFromNotification(
            context: Context,
            amount: Double,
            title: String,
            fullText: String,
            packageName: String
        ) {
            try {
                val prefs = context.getSharedPreferences("firecash_settings", Context.MODE_PRIVATE)
                // Build a semi-unique payload for dedupe
                val now = System.currentTimeMillis()
                val payload = "notif:$packageName:$amount:${fullText.hashCode()}:$now"
                val seen = loadSeenPayloads(prefs)
                if (payload in seen) return

                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                val dateStr = sdfDate.format(Date(now))
                val timeStr = sdfTime.format(Date(now))

                val slip = SavedSlip(
                    payload = payload,
                    amount = amount,
                    transRef = "NOTIF-${now}",
                    senderName = title.takeIf { it.isNotBlank() } ?: packageName,
                    receiverName = null, // will be resolved as income via knownNames or fallback to "Me"
                    date = dateStr,
                    time = timeStr,
                    verificationStatus = VerificationStatus.UNVERIFIED,
                    slipData = null,
                    isMoneyIn = true,
                    savedAt = now
                )

                val slips = loadSlips(prefs).toMutableList()
                slips.add(slip)
                saveSlips(prefs, slips)

                seen.add(payload)
                saveSeenPayloads(prefs, seen)
                Log.i("IncomeNotification", "Saved income $amount from $packageName: $title")
            } catch (e: Exception) {
                Log.w("IncomeNotification", "Failed to save notification income: ${e.message}")
            }
        }

        private fun loadSlips(prefs: SharedPreferences): List<SavedSlip> {
            val raw = prefs.getString(PREFS_SLIPS, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).mapNotNull { i -> slipFromJson(arr.getJSONObject(i)) }
            }.getOrDefault(emptyList())
        }

        private fun saveSlips(prefs: SharedPreferences, slips: List<SavedSlip>) {
            val arr = JSONArray()
            slips.forEach { arr.put(slipToJson(it)) }
            prefs.edit().putString(PREFS_SLIPS, arr.toString()).apply()
        }

        private fun loadSeenPayloads(prefs: SharedPreferences): MutableSet<String> {
            val raw = prefs.getString(PREFS_SEEN, null) ?: return mutableSetOf()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).mapTo(mutableSetOf()) { arr.getString(it) }
            }.getOrDefault(mutableSetOf())
        }

        private fun saveSeenPayloads(prefs: SharedPreferences, seen: Set<String>) {
            val arr = JSONArray(seen.toList())
            prefs.edit().putString(PREFS_SEEN, arr.toString()).apply()
        }

        private fun slipToJson(slip: SavedSlip): JSONObject {
            val obj = JSONObject()
            obj.put("payload", slip.payload)
            slip.amount?.let { obj.put("amount", it) }
            slip.transRef?.let { obj.put("transRef", it) }
            slip.senderName?.let { obj.put("senderName", it) }
            slip.receiverName?.let { obj.put("receiverName", it) }
            slip.date?.let { obj.put("date", it) }
            slip.time?.let { obj.put("time", it) }
            obj.put("verificationStatus", slip.verificationStatus.name)
            obj.put("isMoneyIn", slip.isMoneyIn)
            obj.put("savedAt", slip.savedAt)
            return obj
        }

        fun loadWhitelist(prefs: SharedPreferences): List<String> {
            val raw = prefs.getString(PREFS_NOTIFICATION_WHITELIST, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.getString(it) }
            }.getOrDefault(emptyList())
        }

        fun saveWhitelist(prefs: SharedPreferences, list: List<String>) {
            prefs.edit().putString(PREFS_NOTIFICATION_WHITELIST, JSONArray(list).toString()).apply()
        }

        private fun slipFromJson(obj: JSONObject): SavedSlip? {
            return runCatching {
                SavedSlip(
                    payload = obj.optString("payload"),
                    amount = if (obj.has("amount")) obj.optDouble("amount") else null,
                    transRef = obj.optString("transRef").ifEmpty { null },
                    senderName = obj.optString("senderName").ifEmpty { null },
                    receiverName = obj.optString("receiverName").ifEmpty { null },
                    date = obj.optString("date").ifEmpty { null },
                    time = obj.optString("time").ifEmpty { null },
                    verificationStatus = runCatching { VerificationStatus.valueOf(obj.optString("verificationStatus")) }.getOrDefault(VerificationStatus.UNVERIFIED),
                    slipData = null,
                    isMoneyIn = obj.optBoolean("isMoneyIn", true),
                    savedAt = obj.optLong("savedAt", System.currentTimeMillis())
                )
            }.getOrNull()
        }
    }
}
