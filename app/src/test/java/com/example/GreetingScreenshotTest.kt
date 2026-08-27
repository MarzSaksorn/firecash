package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.Expense
import com.example.data.model.SourceType
import com.example.data.model.VerificationStatus
import com.example.ui.screens.ExpenseItemCard
import com.example.ui.theme.FireCashTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleExpense = Expense(
        id = 1,
        merchant = "Starbucks Coffee",
        amount = 14.50,
        date = "2023-10-24",
        time = "08:42 AM",
        category = "Food & Dining",
        tags = "Breakfast",
        isVerified = true,
        verificationStatus = VerificationStatus.VERIFIED,
        crc = "A8F4",
        sourceType = SourceType.CAMERA
    )
    composeTestRule.setContent {
        FireCashTheme {
            ExpenseItemCard(
                expense = sampleExpense,
                currencySymbol = "$",
                onDelete = {}
            )
        }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/expense_card.png")
  }
}
