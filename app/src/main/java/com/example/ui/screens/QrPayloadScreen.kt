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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.easyslip.VerifySlipResponse
import com.example.data.model.VerificationStatus
import com.example.ui.StringKeys
import com.example.ui.Translations
import com.example.ui.theme.FireCashBackground
import com.example.ui.theme.FireCashError
import com.example.ui.theme.FireCashOnSurface
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashOutlineVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashPrimaryContainer
import com.example.ui.theme.FireCashSecondary
import com.example.ui.theme.FireCashSecondaryContainer
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
    currentWallet: String? = null,
    onToggleWallet: ((String?) -> Unit)? = null,
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
                border = androidx.compose.foundation.BorderStroke(1.dp, FireCashOutlineVariant.copy(alpha = 0.35f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = Translations.t(StringKeys.BACK),
                    tint = FireCashOnSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Translations.t(StringKeys.BACK),
                    color = FireCashOnSurface,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = Translations.t(StringKeys.SLIP_DETAILS),
                style = MaterialTheme.typography.headlineSmall,
                color = FireCashOnSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Lead with the amount and current verification context. Raw QR data stays below for diagnostics.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FireCashSurfaceContainerLow, RoundedCornerShape(20.dp))
                    .border(1.dp, FireCashPrimary.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = Translations.t(StringKeys.AMOUNT),
                    color = FireCashOnSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = (slipData?.amount ?: extractAmount(payload))
                        ?.let { "${Translations.t(StringKeys.THB)} %.2f".format(Locale.US, it) }
                        ?: "—",
                    color = FireCashOnSurface,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (slipData?.verificationStatus) {
                        VerificationStatus.VERIFIED -> Translations.t(StringKeys.SLIP_VERIFIED)
                        VerificationStatus.DUPLICATE_DETECTED -> Translations.t(StringKeys.DUPLICATE_SLIP)
                        VerificationStatus.UNVERIFIED, null -> Translations.t(StringKeys.NOT_VERIFIED)
                        else -> Translations.t(StringKeys.VERIFICATION_FAILED)
                    },
                    color = when (slipData?.verificationStatus) {
                        VerificationStatus.VERIFIED -> FireCashSecondary
                        VerificationStatus.DUPLICATE_DETECTED -> Color(0xFFFFC46B)
                        else -> FireCashOnSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Raw payload display — tap to copy with green Copied feedback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(FireCashSurfaceContainerLow, RoundedCornerShape(14.dp))
                    .border(1.dp, if (payloadCopied) FireCashSecondary else FireCashOutlineVariant, RoundedCornerShape(14.dp))
                    .clickable(
                        enabled = payload.isNotBlank(),
                        role = Role.Button,
                        onClickLabel = Translations.fmt(StringKeys.COPY_FIELD, Translations.t(StringKeys.QR_PAYLOAD))
                    ) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("QR Payload", payload))
                        payloadCopied = true
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            payload.isBlank() -> Translations.t(StringKeys.NO_PAYLOAD)
                            else -> payload
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (payloadCopied) FireCashSecondary else FireCashOnSurface,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = if (payloadCopied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                        contentDescription = if (payloadCopied) Translations.t(StringKeys.COPIED) else Translations.t(StringKeys.QR_PAYLOAD),
                        tint = if (payloadCopied) FireCashSecondary else FireCashPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fraud alert: photo text amount doesn't match the QR/bank amount
            if (amountMismatch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FireCashError.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = FireCashError
                    )
                    Text(
                        text = Translations.t(StringKeys.AMOUNT_MISMATCH),
                        color = FireCashError,
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
                        .background(FireCashError.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = FireCashError
                    )
                    Text(
                        text = Translations.t(StringKeys.DATE_MISMATCH),
                        color = FireCashError,
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
                        .background(FireCashPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = FireCashPrimary
                    )
                    Text(
                        text = warning,
                        color = FireCashPrimary,
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
                        .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(Translations.t(StringKeys.AMOUNT), slipData.amount?.let { "${Translations.t(StringKeys.THB)} %.2f".format(Locale.US, it) } ?: extractAmount(payload)?.let { "${Translations.t(StringKeys.THB)} %.2f".format(Locale.US, it) } ?: "—")
                    DetailRow(Translations.t(StringKeys.TRANSACTION_REF), slipData.transRef ?: "—")
                    DetailRow(Translations.t(StringKeys.DATE), slipData.transDate ?: "—")
                    DetailRow(Translations.t(StringKeys.TIME), slipData.transTime ?: "—")
                    DetailRow(Translations.t(StringKeys.SENDER), slipData.senderName ?: "—")
                    DetailRow(Translations.t(StringKeys.SENDER_BANK), slipData.sendingBankName ?: slipData.sendingBank ?: "—")
                    DetailRow(Translations.t(StringKeys.RECEIVER), slipData.receiverName ?: "—")
                    DetailRow(Translations.t(StringKeys.RECEIVER_BANK), slipData.receivingBankName ?: slipData.receivingBank ?: "—")
                    DetailRow(Translations.t(StringKeys.AMOUNT_MATCHED), if (slipData.isAmountMatched) Translations.t(StringKeys.YES) else Translations.t(StringKeys.NO))
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
                        .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(Translations.t(StringKeys.AMOUNT), extractAmount(payload)?.let { "${Translations.t(StringKeys.THB)} %.2f".format(Locale.US, it) } ?: "—")
                    DetailRow(Translations.t(StringKeys.TRANSACTION_REF), "—")
                    DetailRow(Translations.t(StringKeys.DATE), "—")
                    DetailRow(Translations.t(StringKeys.TIME), "—")
                    DetailRow(Translations.t(StringKeys.SENDER), "—")
                    DetailRow(Translations.t(StringKeys.RECEIVER), "—")
                    DetailRow(Translations.t(StringKeys.STATUS), Translations.t(StringKeys.NOT_VERIFIED))
                }
            }

            // Manual income/expense/transfer toggle
            Spacer(modifier = Modifier.height(16.dp))
            CategoryToggle(
                currentCategory = currentCategory,
                onToggle = onToggleCategory
            )

            // Wallet toggle
            Spacer(modifier = Modifier.height(16.dp))
            WalletToggle(
                currentWallet = currentWallet,
                onToggle = onToggleWallet
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
            .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = Translations.t(StringKeys.SLIP_PHOTO),
            color = FireCashOnSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
        AsyncImage(
            model = photoPath,
            contentDescription = Translations.t(StringKeys.SLIP_PHOTO),
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
            Text(Translations.t(StringKeys.OPEN_PHOTO), color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
private fun StatusBanner(slipData: VerifySlipResponse) {
    val (icon, text, color) = when (slipData.verificationStatus) {
        VerificationStatus.DUPLICATE_DETECTED -> Triple(
            Icons.Default.ErrorOutline,
            Translations.t(StringKeys.DUPLICATE_SLIP),
            FireCashPrimary
        )
        VerificationStatus.VERIFIED -> Triple(
            Icons.Default.CheckCircle,
            Translations.t(StringKeys.SLIP_VERIFIED),
            FireCashSecondary
        )
        else -> Triple(
            Icons.Default.ErrorOutline,
            slipData.errorMessage ?: Translations.t(StringKeys.VERIFICATION_FAILED),
            FireCashError
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
            .heightIn(min = 48.dp)
            .clickable(
                enabled = isCopyable,
                role = Role.Button,
                onClickLabel = Translations.fmt(StringKeys.COPY_FIELD, label)
            ) {
                if (!isCopyable) return@clickable
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(label, value))
                copied = true
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = FireCashOnSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = value,
                color = FireCashOnSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            if (copied) {
                Text(
                    text = Translations.t(StringKeys.COPIED),
                    color = FireCashSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (copied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
            contentDescription = null,
            tint = if (copied) FireCashSecondary else FireCashOnSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun WalletToggle(
    currentWallet: String?,
    onToggle: ((String?) -> Unit)?
) {
    val options = listOf(
        null to Translations.t(StringKeys.BANK) to Icons.Default.AccountBalance,
        "cash" to Translations.t(StringKeys.CASH) to Icons.Default.AccountBalanceWallet
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FireCashSurfaceContainerLow, RoundedCornerShape(16.dp))
            .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = Translations.t(StringKeys.WALLET),
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
                val selected = currentWallet == key
                val bgColor = if (selected) {
                    if (key == null) FireCashPrimaryContainer else FireCashSecondaryContainer
                } else FireCashSurfaceContainerLow
                val contentColor = if (selected) FireCashOnSurface else FireCashOnSurfaceVariant
                Button(
                    onClick = { onToggle?.invoke(key) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bgColor,
                        contentColor = contentColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
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
        "income" to Translations.t(StringKeys.INCOME) to Icons.Default.ArrowUpward,
        "expense" to Translations.t(StringKeys.EXPENSE) to Icons.Default.ArrowDownward,
        "transfer" to Translations.t(StringKeys.TRANSFER) to Icons.Default.SwapHoriz
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FireCashSurfaceContainerLow, RoundedCornerShape(16.dp))
            .border(1.dp, FireCashOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = Translations.t(StringKeys.CLASSIFICATION),
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
                    "income" -> FireCashSecondaryContainer
                    "expense" -> FireCashError.copy(alpha = 0.22f)
                    else -> FireCashPrimaryContainer
                } else FireCashSurfaceContainerLow
                val contentColor = if (selected) FireCashOnSurface else FireCashOnSurfaceVariant
                Button(
                    onClick = {
                        onToggle?.invoke(if (selected) null else key)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bgColor,
                        contentColor = contentColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        if (currentCategory == null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Translations.t(StringKeys.AUTO_DETECTED),
                color = FireCashOnSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
