package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.easyslip.VerifySlipResponse
import com.example.data.model.VerificationStatus
import com.example.ui.theme.FireCashBackground
import com.example.ui.theme.FireCashOnSurface
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashSurfaceContainerLow
import java.util.Locale

@Composable
fun QrPayloadScreen(
    payload: String,
    slipData: VerifySlipResponse? = null,
    warning: String = "",
    onBack: () -> Unit = {},
    onSave: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(FireCashBackground)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "QR Payload",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Raw payload display
            Text(
                text = if (payload.isBlank()) "No payload" else payload,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Warning banner when verification can't run (e.g. no API key)
            if (warning.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFB74D).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D)
                    )
                    Text(
                        text = warning,
                        color = Color(0xFFFFB74D),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Verification status banner
            if (slipData != null) {
                StatusBanner(slipData = slipData)
                Spacer(modifier = Modifier.height(12.dp))

                // Slip details card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            FireCashSurfaceContainerLow,
                            RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow("Amount", slipData.amount?.let { "THB %.2f".format(Locale.US, it) } ?: "—")
                    DetailRow("Transaction Ref", slipData.transRef ?: "—")
                    DetailRow("Date", slipData.transDate ?: "—")
                    DetailRow("Time", slipData.transTime ?: "—")
                    DetailRow("Sender", slipData.senderName ?: "—")
                    DetailRow("Sender Bank", slipData.sendingBankName ?: slipData.sendingBank ?: "—")
                    DetailRow("Receiver", slipData.receiverName ?: "—")
                    DetailRow("Receiver Bank", slipData.receivingBankName ?: slipData.receivingBank ?: "—")
                    DetailRow("Amount Matched", if (slipData.isAmountMatched) "Yes" else "No")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save button - transaction type is auto-detected via My Names in Settings
            Button(
                onClick = { onSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FireCashPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Save to Account",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(slipData: VerifySlipResponse) {
    val (icon, text, color) = when (slipData.verificationStatus) {
        VerificationStatus.DUPLICATE_DETECTED -> Triple(
            Icons.Default.ErrorOutline,
            "Duplicate slip detected",
            Color(0xFFFFB74D)
        )
        VerificationStatus.VERIFIED -> Triple(
            Icons.Default.CheckCircle,
            "Slip verified successfully",
            Color(0xFF66BB6A)
        )
        else -> Triple(
            Icons.Default.ErrorOutline,
            slipData.errorMessage ?: "Verification failed",
            Color(0xFFEF5350)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Text(text = text, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = FireCashOnSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = FireCashOnSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
