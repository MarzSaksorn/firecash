package com.example.service

import android.content.SharedPreferences

/**
 * Pre-defined notification whitelist presets (app package + prefix).
 *
 * Seeded into preferences ONCE on first launch, and only when the user has
 * not already customized the whitelist (i.e. the pref key does not exist yet).
 *
 * To add defaults, edit [incomePresets] and [expensePresets] below.
 * Each entry is one (package, prefix) pair; an app can appear multiple times
 * with different prefixes.
 */
object NotificationPresets {

    /** Money-in notification apps: <packageName, prefix> — filled in by user later. */
    val incomePresets: List<WhitelistedApp> = listOf(
        // example: WhitelistedApp("com.kasikornbank.kplus", "โอนเงินเข้า")
    )

    /** Money-out notification apps: <packageName, prefix> — filled in by user later. */
    val expensePresets: List<WhitelistedApp> = listOf(
        // example: WhitelistedApp("com.kasikornbank.kplus", "โอนเงินสำเร็จ")
    )

    private const val PREFS_PRESETS_SEEDED = "notification_presets_seeded"

    /**
     * Seed preset lists into prefs. Safe to call on every launch — the flag
     * prevents re-seeding, and existing (user-customized) lists are never touched.
     */
    fun seedIfNeeded(prefs: SharedPreferences) {
        if (prefs.getBoolean(PREFS_PRESETS_SEEDED, false)) return
        prefs.edit().putBoolean(PREFS_PRESETS_SEEDED, true).apply()

        if (incomePresets.isNotEmpty() &&
            !prefs.contains(IncomeNotificationService.PREFS_NOTIFICATION_WHITELIST)
        ) {
            IncomeNotificationService.saveWhitelist(prefs, incomePresets)
        }
        if (expensePresets.isNotEmpty() &&
            !prefs.contains(IncomeNotificationService.PREFS_NOTIFICATION_WHITELIST_EXPENSE)
        ) {
            IncomeNotificationService.saveWhitelistExpense(prefs, expensePresets)
        }
    }
}
