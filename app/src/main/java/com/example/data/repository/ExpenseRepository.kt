package com.example.data.repository

import com.example.data.local.ExpenseDao
import com.example.data.local.KeywordRuleDao
import com.example.data.model.Expense
import com.example.data.model.KeywordRule
import com.example.data.model.SourceType
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val keywordRuleDao: KeywordRuleDao
) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allRules: Flow<List<KeywordRule>> = keywordRuleDao.getAllRules()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = expenseDao.getAllExpenses().first()
            if (existing.isEmpty()) {
                seedInitialData()
            }
            val existingRules = keywordRuleDao.getAllRules().first()
            if (existingRules.isEmpty()) {
                seedInitialRules()
            }
        }
    }

    private suspend fun seedInitialData() {
        val sampleExpenses = listOf(
            Expense(
                merchant = "Artisan Roasters",
                amount = 14.50,
                date = "2023-10-24",
                time = "08:42 AM",
                category = "Food & Dining",
                tags = "Coffee, Morning",
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCM6lTYNqsa0-EHu-FFKmSk_kH8_bkt83PGtRN27Z_UwztDsWqdGicAJeTrsKOhhbsbdrlEK8zF6y2pJR2R5RhMhIfpq5HfgCQsiPHirPnjj-rF0A6DJWHj7uXiLEvjLeWdfbBSmYxZt2odp1X6JYUWHP5EFuge6h8Mxn6Oo6s66z3eWrfeNXJ0KtUvYpjf-TU4ife6e7i6bs1agtHI_rPrkS88RgtGbZ_d14M8GocYQ0CqgkT62zDH9w",
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                crc = "88F2",
                sendingBank = "004",
                transRef = "TXN-20231024-8841",
                sourceType = SourceType.CAMERA,
                dateGroup = "Today"
            ),
            Expense(
                merchant = "City Cab Co.",
                amount = 45.00,
                date = "2023-10-24",
                time = "07:15 AM",
                category = "Travel",
                tags = "Taxi, Commute",
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBRg5UMxh0fNnHsk40bUW8ffJWpm0hSUNdXsZWxXTHXwNEyJ8VisSPWBai7mLzUu6nDrQ62goyUAZNZn7L5W7D0tTRLv7Sg-L58CZwTBtqWVsLFQlH4wau347uh4vJeFUoKjkkJq7SWGZDfsMo1DaCzhcUo0oMhkBiQKCDn6T6JAR7DOGPUai_KH64f_fJO9f9ZjdIttcpRFxQZPfB6TJvylLnVlS-VXOlNzQ006OTGaRcTKRDsDUwLow",
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                crc = "A8F4",
                sendingBank = "014",
                transRef = "TXN-20231024-7712",
                sourceType = SourceType.GALLERY,
                dateGroup = "Today"
            ),
            Expense(
                merchant = "Tech Gear Inc",
                amount = 299.99,
                date = "2023-10-23",
                time = "04:30 PM",
                category = "Office Supplies",
                tags = "Hardware, Laptop",
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAstveoykS_j7n5Zio6dxX4QQOWsw3i46sTCyBptuEKbSFMGByGgj02VEEgMOnP42dWochUKjsLiRBetIxN72xWOjLa4sLtM6fZPcPAatOFTh0TTHmmxW66nCUp2glMbAt_O3-qf2qxcEBumU6p2Wp_V6HmvMBAfWZHdq5ZT2Ng_FR9t3vv6utxXlhEjMkmFGfTng34vgoyDETu10DUJ6-tP58zH54PMkQ-B1KjEW2dAyEHnWu_nB6p-g",
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                crc = "4D1B",
                sendingBank = "002",
                sourceType = SourceType.PDF_UPLOAD,
                dateGroup = "Yesterday"
            ),
            Expense(
                merchant = "Monthly Cloud Subs",
                amount = 12.00,
                date = "2023-10-23",
                time = "09:00 AM",
                category = "Software",
                tags = "SaaS, Server",
                imageUrl = null,
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                sourceType = SourceType.MANUAL,
                dateGroup = "Yesterday"
            ),
            Expense(
                merchant = "Delta Airlines",
                amount = 540.00,
                date = "2024-04-12",
                time = "02:15 PM",
                category = "Travel",
                tags = "Flight, Business Trip",
                imageUrl = null,
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                sourceType = SourceType.PDF_UPLOAD,
                dateGroup = "Previous"
            ),
            Expense(
                merchant = "Starbucks Coffee",
                amount = 84.50,
                date = "2024-04-18",
                time = "10:30 AM",
                category = "Food & Dining",
                tags = "Coffee, Team",
                imageUrl = null,
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                sourceType = SourceType.CAMERA,
                dateGroup = "Previous"
            )
        )
        expenseDao.insertAll(sampleExpenses)
    }

    private suspend fun seedInitialRules() {
        val sampleRules = listOf(
            KeywordRule(keyword = "Uber", category = "Travel"),
            KeywordRule(keyword = "Starbucks", category = "Food & Dining"),
            KeywordRule(keyword = "Delta", category = "Travel"),
            KeywordRule(keyword = "Amazon", category = "Retail"),
            KeywordRule(keyword = "Cloud", category = "Software"),
            KeywordRule(keyword = "PromptPay", category = "Food & Dining")
        )
        keywordRuleDao.insertAll(sampleRules)
    }

    suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun insertAll(expenses: List<Expense>) {
        expenseDao.insertAll(expenses)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteById(id: Long) {
        expenseDao.deleteById(id)
    }

    suspend fun clearAll() {
        expenseDao.clearAll()
    }

    suspend fun insertRule(rule: KeywordRule): Long {
        return keywordRuleDao.insertRule(rule)
    }

    suspend fun deleteRule(rule: KeywordRule) {
        keywordRuleDao.deleteRule(rule)
    }

    suspend fun deleteRuleById(id: Long) {
        keywordRuleDao.deleteById(id)
    }

    fun searchExpenses(query: String): Flow<List<Expense>> {
        return expenseDao.searchExpenses(query)
    }

    fun getExpensesByCategory(category: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByCategory(category)
    }

    suspend fun autoCategorize(merchant: String): String {
        val rules = keywordRuleDao.getAllRules().first()
        for (rule in rules) {
            if (merchant.contains(rule.keyword, ignoreCase = true)) {
                return rule.category
            }
        }
        return "Other"
    }
}
