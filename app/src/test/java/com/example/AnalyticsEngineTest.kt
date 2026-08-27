package com.example

import com.example.data.analytics.AnalyticsEngine
import com.example.data.model.Expense
import com.example.data.model.SourceType
import com.example.data.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEngineTest {

    @Test
    fun testEmptyExpensesSummary() {
        val summary = AnalyticsEngine.generateAnalytics(emptyList(), "$")
        assertEquals(0.0, summary.totalSpent, 0.001)
        assertEquals("$0.00", summary.formattedTotal)
        assertTrue(summary.insights.isNotEmpty())
    }

    @Test
    fun testExpenseAggregationAndInsights() {
        val testExpenses = listOf(
            Expense(
                id = 1,
                merchant = "Amazon",
                amount = 150.00,
                date = "2023-10-24",
                category = "Retail",
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                sourceType = SourceType.CAMERA
            ),
            Expense(
                id = 2,
                merchant = "Amazon",
                amount = 50.00,
                date = "2023-10-24",
                category = "Retail",
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                sourceType = SourceType.GALLERY
            ),
            Expense(
                id = 3,
                merchant = "Starbucks",
                amount = 25.00,
                date = "2023-10-23",
                category = "Food & Dining",
                isVerified = true,
                verificationStatus = VerificationStatus.VERIFIED,
                sourceType = SourceType.MANUAL
            )
        )

        val summary = AnalyticsEngine.generateAnalytics(testExpenses, "$")
        assertEquals(225.00, summary.totalSpent, 0.01)
        assertEquals("Amazon", summary.mostFrequentMerchant)
        assertEquals(2, summary.frequentMerchantCount)
        assertNotNull(summary.highestExpense)
        assertEquals("Amazon", summary.highestExpense?.merchant)
        assertEquals(150.00, summary.highestExpense?.amount ?: 0.0, 0.01)

        // Verify categories
        assertEquals(2, summary.categorySpends.size)
        val retailCat = summary.categorySpends.find { it.category == "Retail" }
        assertNotNull(retailCat)
        assertEquals(200.00, retailCat?.totalAmount ?: 0.0, 0.01)
    }
}
