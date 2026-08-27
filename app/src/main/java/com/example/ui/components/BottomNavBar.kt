package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FireCashOnSecondaryContainer
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashSecondaryContainer
import com.example.ui.theme.FireCashSurfaceContainer
import com.example.ui.viewmodel.Screen

enum class NavTab {
    CAPTURE,
    EXPENSES,
    ANALYTICS,
    BACKUP
}

@Composable
fun FireCashBottomBar(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit,
    showBackupTab: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(FireCashSurfaceContainer)
            .navigationBarsPadding()
            .height(68.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!showBackupTab) {
            // Capture Tab
            BottomNavItem(
                icon = Icons.Default.PhotoCamera,
                label = "Capture",
                isSelected = currentScreen is Screen.Capture,
                testTag = "nav_capture",
                onClick = { onTabSelected(Screen.Capture) }
            )

            // Expenses Tab
            BottomNavItem(
                icon = Icons.Default.ReceiptLong,
                label = "Expenses",
                isSelected = currentScreen is Screen.Expenses,
                testTag = "nav_expenses",
                onClick = { onTabSelected(Screen.Expenses) }
            )

            // Analytics Tab
            BottomNavItem(
                icon = Icons.Default.Leaderboard,
                label = "Analytics",
                isSelected = currentScreen is Screen.Analytics,
                testTag = "nav_analytics",
                onClick = { onTabSelected(Screen.Analytics) }
            )
        } else {
            // Home / Expenses Tab
            BottomNavItem(
                icon = Icons.Default.ReceiptLong,
                label = "Expenses",
                isSelected = currentScreen is Screen.Expenses,
                testTag = "nav_expenses",
                onClick = { onTabSelected(Screen.Expenses) }
            )

            // Backup Tab
            BottomNavItem(
                icon = Icons.Default.CloudSync,
                label = "Backup",
                isSelected = currentScreen is Screen.BackupRestore,
                testTag = "nav_backup",
                onClick = { onTabSelected(Screen.BackupRestore) }
            )

            // Analytics Tab
            BottomNavItem(
                icon = Icons.Default.Leaderboard,
                label = "Analytics",
                isSelected = currentScreen is Screen.Analytics,
                testTag = "nav_analytics",
                onClick = { onTabSelected(Screen.Analytics) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) FireCashSecondaryContainer else Color.Transparent,
        label = "nav_item_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) FireCashOnSecondaryContainer else FireCashOnSurfaceVariant,
        label = "nav_item_content"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
