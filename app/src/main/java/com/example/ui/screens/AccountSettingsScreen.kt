package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.example.ui.theme.FireCashBackground
import com.example.ui.theme.FireCashOnSurface
import com.example.ui.theme.FireCashOnSurfaceVariant
import com.example.ui.theme.FireCashPrimary
import com.example.ui.theme.FireCashSurfaceContainerLow
import java.io.File
import java.io.FileOutputStream

@Composable
fun AccountSettingsScreen(
    isLoading: Boolean = false,
    trackedFolders: List<String> = emptyList(),
    onBack: () -> Unit,
    onFolderSelected: (Uri) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onSyncNow: () -> Unit,
    onImportSlips: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onFolderSelected(uri)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val paths = uris.mapIndexedNotNull { index, uri ->
                val tempFile = File(context.cacheDir, "sync_${System.currentTimeMillis()}_$index.jpg")
                val ok = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                }.isSuccess
                if (ok) tempFile.absolutePath else null
            }
            if (paths.isNotEmpty()) {
                onImportSlips(paths)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FireCashBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FireCashPrimary
                    )
                }
                Text(
                    text = "Account Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tracked Folders",
                color = FireCashOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (trackedFolders.isEmpty()) {
                Text(
                    text = "No folders tracked yet. Add folders to auto-scan for new slips.",
                    color = FireCashOnSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    trackedFolders.forEach { uriStr ->
                        val name = runCatching {
                            DocumentFile.fromTreeUri(context, Uri.parse(uriStr))?.name
                        }.getOrNull()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FireCashSurfaceContainerLow, RoundedCornerShape(12.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = FireCashPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = name ?: uriStr,
                                color = FireCashOnSurface,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveFolder(uriStr) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove folder",
                                    tint = FireCashOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add folder
            OutlinedButton(
                onClick = { folderPickerLauncher.launch(null) },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = FireCashPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Tracked Folder",
                    color = FireCashPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sync tracked folders now
            OutlinedButton(
                onClick = onSyncNow,
                enabled = trackedFolders.isNotEmpty() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = FireCashPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sync Tracked Folders Now",
                    color = FireCashPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Import slip photos manually
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(arrayOf("image/*"))
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = FireCashPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Import Slip Photos from Device",
                    color = FireCashPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Slips in the tracked folder are scanned automatically for QR codes and added to your account.",
                color = FireCashOnSurfaceVariant,
                fontSize = 13.sp
            )
        }

        // Loading overlay while syncing
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = "Syncing slips...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
