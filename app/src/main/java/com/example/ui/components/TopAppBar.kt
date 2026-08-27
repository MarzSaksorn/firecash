package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashSurfaceContainer

@Composable
fun FireCashTopBar(
    title: String = "FireCash",
    showBackButton: Boolean = true,
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(FireCashSurfaceContainer)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .testTag("top_bar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = FireCashPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            IconButton(
                onClick = {},
                enabled = false
            ) {
                // Empty spacer matching back button size
            }
        }

        Text(
            text = title,
            color = FireCashPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.25).sp,
            modifier = Modifier.testTag("app_title")
        )

        IconButton(
            onClick = onProfileClick,
            modifier = Modifier
                .clip(CircleShape)
                .testTag("top_bar_profile_button")
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "User account",
                tint = FireCashPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
