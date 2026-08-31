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
        WhitelistedApp("com.kasikornbank.makebykbank", "โอนเงินให้คุณ ฿"),
        WhitelistedApp("com.kasikornbank.makebykbank", "คุณได้รับเงิน ฿"),
        WhitelistedApp("com.kasikorn.retail.mbanking.wap", "จำนวนเงิน "),
        WhitelistedApp("com.scb.phone", "คุณได้รับเงิน ")
    )

    /** Money-out notification apps: <packageName, prefix> — filled in by user later. */
    val expensePresets: List<WhitelistedApp> = listOf(
        // example: WhitelistedApp("com.kasikornbank.kplus", "โอนเงินสำเร็จ")
        WhitelistedApp("com.kasikornbank.makebykbank", "โอนเงินสำเร็จ ฿"),
        WhitelistedApp("com.kasikorn.retail.mbanking.wap", "จำนวนเงิน -")
    )

    private const val PREFS_PRESETS_SEEDED = "notification_presets_seeded"
    private const val PREFS_DISABLED_INCOME = "preset_disabled_income"
    private const val PREFS_DISABLED_EXPENSE = "preset_disabled_expense"

    /** Stable identity key for a preset entry. */
    fun presetKey(entry: WhitelistedApp): String = "${entry.packageName}|${entry.prefix}"

    /** True if the entry is part of the permanent income preset list (cannot be removed, only toggled). */
    fun isIncomePermanent(entry: WhitelistedApp): Boolean =
        incomePresets.any { presetKey(it) == presetKey(entry) }

    /** True if the entry is part of the permanent expense preset list (cannot be removed, only toggled). */
    fun isExpensePermanent(entry: WhitelistedApp): Boolean =
        expensePresets.any { presetKey(it) == presetKey(entry) }

    fun loadDisabledIncome(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(PREFS_DISABLED_INCOME, emptySet()) ?: emptySet()

    fun loadDisabledExpense(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(PREFS_DISABLED_EXPENSE, emptySet()) ?: emptySet()

    fun setDisabledIncome(prefs: SharedPreferences, entry: WhitelistedApp, disabled: Boolean) {
        val set = loadDisabledIncome(prefs).toMutableSet()
        if (disabled) set.add(presetKey(entry)) else set.remove(presetKey(entry))
        prefs.edit().putStringSet(PREFS_DISABLED_INCOME, set).apply()
    }

    fun setDisabledExpense(prefs: SharedPreferences, entry: WhitelistedApp, disabled: Boolean) {
        val set = loadDisabledExpense(prefs).toMutableSet()
        if (disabled) set.add(presetKey(entry)) else set.remove(presetKey(entry))
        prefs.edit().putStringSet(PREFS_DISABLED_EXPENSE, set).apply()
    }

    /** Effective income list = enabled presets + user entries (preset duplicates dropped). */
    fun mergeIncome(stored: List<WhitelistedApp>, disabled: Set<String> = emptySet()): List<WhitelistedApp> =
        (incomePresets.filter { presetKey(it) !in disabled } + stored.filterNot { isIncomePermanent(it) }).distinct()

    /** Effective expense list = enabled presets + user entries (preset duplicates dropped). */
    fun mergeExpense(stored: List<WhitelistedApp>, disabled: Set<String> = emptySet()): List<WhitelistedApp> =
        (expensePresets.filter { presetKey(it) !in disabled } + stored.filterNot { isExpensePermanent(it) }).distinct()

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
