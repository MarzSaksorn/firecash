package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StringKeys
import com.example.ui.Translations
import com.example.ui.theme.FireCashOnPrimaryContainer
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashPrimaryContainer
import com.example.ui.theme.FireCashSurfaceContainer

/** The live app uses these slots as Home, Scan, Insights, and Settings. */
enum class NavTab {
    HOME,
    CARDS,
    SPENDING,
    PROFILE
}

@Composable
fun FireCashBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(
                    color = FireCashSurfaceContainer.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(24.dp)
                )
                .selectableGroup()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(Icons.Default.Home, Translations.t(StringKeys.HOME), currentTab == NavTab.HOME) {
                onTabSelected(NavTab.HOME)
            }
            BottomNavItem(Icons.Default.QrCodeScanner, Translations.t(StringKeys.SCAN_SLIP), currentTab == NavTab.CARDS) {
                onTabSelected(NavTab.CARDS)
            }
            BottomNavItem(Icons.Default.Leaderboard, Translations.t(StringKeys.ANALYTICS), currentTab == NavTab.SPENDING) {
                onTabSelected(NavTab.SPENDING)
            }
            BottomNavItem(Icons.Default.Settings, Translations.t(StringKeys.SETTINGS), currentTab == NavTab.PROFILE) {
                onTabSelected(NavTab.PROFILE)
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) FireCashPrimaryContainer else Color.Transparent,
        label = "nav_item_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) FireCashOnPrimaryContainer else FireCashOnSurfaceVariant,
        label = "nav_item_content"
    )

    Column(
        modifier = Modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = label
            }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
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
