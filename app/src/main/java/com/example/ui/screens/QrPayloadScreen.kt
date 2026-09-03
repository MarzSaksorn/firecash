package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.easyslip.VerifySlipResponse
import com.example.data.model.VerificationStatus
import com.example.ui.theme.FireCashBackground
import com.example.ui.theme.FireCashOnSurface
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashSurfaceContainerLow
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun QrPayloadScreen(
    payload: String,
    slipData: VerifySlipResponse? = null,
    warning: String = "",
    photoPath: String? = null,
    amountMismatch: Boolean = false,
    dateMismatch: Boolean = false,
    currentCategory: String? = null,
    onToggleCategory: ((String?) -> Unit)? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var payloadCopied by remember { mutableStateOf(false) }
    LaunchedEffect(payloadCopied) {
        if (payloadCopied) {
            delay(1200)
            payloadCopied = false
        }
    }

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
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = FireCashSurfaceContainerLow),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "QR Payload",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Raw payload display — tap to copy with green Copied feedback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (payloadCopied) Color(0xFF66BB6A) else Color.Gray, RoundedCornerShape(8.dp))
                    .clickable(enabled = payload.isNotBlank()) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("QR Payload", payload))
                        payloadCopied = true
                    }
                    .padding(12.dp)
            ) {
                Text(
                    text = when {
                        payload.isBlank() -> "No payload"
                        payloadCopied -> "Copied"
                        else -> payload
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (payloadCopied) Color(0xFF66BB6A) else Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fraud alert: photo text amount doesn't match the QR/bank amount
            if (amountMismatch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEF5350).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF5350)
                    )
                    Text(
                        text = "Amount mismatch — the amount on this slip photo differs from the QR/bank amount. Possible tampered slip!",
                        color = Color(0xFFEF5350),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Fraud alert: photo text date doesn't match the bank-verified date
            if (dateMismatch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEF5350).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF5350)
                    )
                    Text(
                        text = "Date mismatch — the date printed on this slip photo differs from the bank-verified date. Possible tampered slip!",
                        color = Color(0xFFEF5350),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

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

            // Verification status banner + details — always show card, fallback to payload-extracted amount if not verified
            if (slipData != null) {
                StatusBanner(slipData = slipData)
                Spacer(modifier = Modifier.height(12.dp))
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
                    DetailRow("Amount", slipData.amount?.let { "THB %.2f".format(Locale.US, it) } ?: extractAmount(payload)?.let { "THB %.2f".format(Locale.US, it) } ?: "—")
                    DetailRow("Transaction Ref", slipData.transRef ?: "—")
                    DetailRow("Date", slipData.transDate ?: "—")
                    DetailRow("Time", slipData.transTime ?: "—")
                    DetailRow("Sender", slipData.senderName ?: "—")
                    DetailRow("Sender Bank", slipData.sendingBankName ?: slipData.sendingBank ?: "—")
                    DetailRow("Receiver", slipData.receiverName ?: "—")
                    DetailRow("Receiver Bank", slipData.receivingBankName ?: slipData.receivingBank ?: "—")
                    DetailRow("Amount Matched", if (slipData.isAmountMatched) "Yes" else "No")
                }
            } else {
                // Fallback for old slips where slipData was null — still show a card from raw payload
                Spacer(modifier = Modifier.height(4.dp))
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
                    DetailRow("Amount", extractAmount(payload)?.let { "THB %.2f".format(Locale.US, it) } ?: "—")
                    DetailRow("Transaction Ref", "—")
                    DetailRow("Date", "—")
                    DetailRow("Time", "—")
                    DetailRow("Sender", "—")
                    DetailRow("Receiver", "—")
                    DetailRow("Status", "Not verified — enable EasySlip and Sync unverified")
                }
            }

            // Manual income/expense/transfer toggle
            Spacer(modifier = Modifier.height(16.dp))
            CategoryToggle(
                currentCategory = currentCategory,
                onToggle = onToggleCategory
            )

            // Photo of the slip on device — thumbnail + open link
            if (!photoPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                PhotoSection(photoPath = photoPath)
            }

        }
    }
}

@Composable
private fun PhotoSection(photoPath: String) {
    val context = LocalContext.current
    val uri = runCatching { Uri.parse(photoPath) }.getOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FireCashSurfaceContainerLow, RoundedCornerShape(16.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Slip Photo",
            color = FireCashOnSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
        AsyncImage(
            model = photoPath,
            contentDescription = "Slip photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.4f))
        )
        Button(
            onClick = {
                val targetUri = if (photoPath.startsWith("file://") || (uri?.scheme == null)) {
                    val f = File(uri?.path ?: photoPath)
                    FileProvider.getUriForFile(
                        context,
                        context.packageName + ".fileprovider",
                        f
                    )
                } else {
                    uri
                }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(targetUri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = FireCashPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open photo on device", color = Color.White, fontSize = 14.sp)
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
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1200)
            copied = false
        }
    }
    val isCopyable = value != "—" && value.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isCopyable) {
                if (!isCopyable) return@clickable
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(label, value))
                copied = true
            }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = FireCashOnSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (copied) "Copied" else value,
            color = if (copied) Color(0xFF66BB6A) else FireCashOnSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun extractAmount(text: String): Double? {
    val regex = Regex("""\d{1,3}(?:,\d{3})*(?:\.\d+)?|\d+(?:\.\d+)?""")
    return regex.find(text)?.value?.replace(",", "")?.toDoubleOrNull()
}

@Composable
private fun CategoryToggle(
    currentCategory: String?,
    onToggle: ((String?) -> Unit)?
) {
    val options = listOf(
        "income" to "Income" to Icons.Default.ArrowUpward,
        "expense" to "Expense" to Icons.Default.ArrowDownward,
        "transfer" to "Transfer" to Icons.Default.SwapHoriz
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FireCashSurfaceContainerLow, RoundedCornerShape(16.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Classification",
            color = FireCashOnSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (keyLabel, icon) ->
                val (key, label) = keyLabel
                val selected = when (currentCategory) {
                    null -> false
                    key -> true
                    else -> false
                }
                val bgColor = if (selected) when (key) {
                    "income" -> Color(0xFF10B981)
                    "expense" -> Color(0xFFEF4444)
                    else -> Color(0xFF6366F1)
                } else FireCashSurfaceContainerLow
                Button(
                    onClick = {
                        onToggle?.invoke(if (selected) null else key)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = bgColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
        if (currentCategory == null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Auto-detected from known names",
                color = FireCashOnSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
