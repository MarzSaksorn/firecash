package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashSecondary
import com.example.ui.theme.FireCashSurfaceContainer

@Composable
fun CaptureBottomBar(
    showSettings: Boolean,
    showSavedSlips: Boolean,
    onSettingsClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onSavedSlipsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(FireCashSurfaceContainer)
            .navigationBarsPadding()
            .height(68.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings item
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onSettingsClick)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (showSettings) FireCashPrimary else FireCashOnSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Settings",
                    color = if (showSettings) FireCashPrimary else FireCashOnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (showSettings) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }

        // Capture button (middle, raised)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(FireCashSecondary)
                .clickable(onClick = onCaptureClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Capture",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        // Account item
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onSavedSlipsClick)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Account",
                    tint = if (showSavedSlips) FireCashPrimary else FireCashOnSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Account",
                    color = if (showSavedSlips) FireCashPrimary else FireCashOnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (showSavedSlips) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}
