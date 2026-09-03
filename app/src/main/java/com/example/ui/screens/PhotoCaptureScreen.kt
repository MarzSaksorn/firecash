package com.example.ui.screens

import android.Manifest
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Simple photo capture UI.
 *
 * - Shows a live CameraX preview.
 * - Bottom row contains a file‑selection button on the left and a capture (shutter) button in the centre.
 * - Calls `onPhotoCaptured` with the absolute path of the saved image.
 * - Calls `onFileSelected` when the side button is tapped.
 */
@Composable
fun PhotoCaptureScreen(
    onPhotoCaptured: (String) -> Unit,
    onFileSelected: () -> Unit,
    onImageSelected: (String) -> Unit,
    isLoading: Boolean = false,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    payloadText: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Keep the photo in persistent app storage so it survives and links on the details page
            val photoDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            val photoFile = File(photoDir, "picked_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(photoFile).use { output -> input.copyTo(output) }
            }
            onImageSelected(photoFile.absolutePath)
        }
    }

    // Request camera permission on first composition
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera preview view
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    // Live slip detection: rotated-frame quad (in display pixels) when a flat slip is in view
    var slipQuad by remember { mutableStateOf<List<Pair<Float, Float>>?>(null) }
    var slipRotW by remember { mutableStateOf(1) }
    var slipRotH by remember { mutableStateOf(1) }
    var frameTick by remember { mutableStateOf(0) }
    // Preview view size in px so the quad can be mapped onto the screen
    var viewPx by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    // Initialise CameraX when permission is granted
    if (permissionGranted) {
        LaunchedEffect(previewView) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                imageCapture = ImageCapture.Builder().build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .build()
                imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                    // Live slip (flat surface) detection — cheap Y-plane sampling into OpenCV.
                    // The quad is rotated into the display orientation and reported in frame px.
                    try {
                        val tick = frameTick
                        frameTick = tick + 1
                        if (tick % 4 == 0) {
                            val plane = imageProxy.planes[0]
                            val found = com.example.data.ocr.SlipDocumentDetector
                                .detectYPlane(imageProxy.width, imageProxy.height, plane.rowStride, plane.buffer)
                            if (found != null) {
                                val w = imageProxy.width.toFloat()
                                val h = imageProxy.height.toFloat()
                                val rot = imageProxy.imageInfo.rotationDegrees
                                val pts = found.points.map { p ->
                                    when (rot) {
                                        90 -> (h - 1 - p.y).toFloat() to p.x.toFloat()      // portrait CW
                                        270 -> p.y.toFloat() to (w - 1 - p.x).toFloat()      // portrait CCW
                                        180 -> (w - 1 - p.x).toFloat() to (h - 1 - p.y).toFloat()
                                        else -> p.x.toFloat() to p.y.toFloat()
                                    }
                                }
                                // display dims after rotation
                                val rotW = if (rot == 90 || rot == 270) h.toInt() else w.toInt()
                                val rotH = if (rot == 90 || rot == 270) w.toInt() else h.toInt()
                                slipQuad = pts
                                slipRotW = rotW
                                slipRotH = rotH
                            } else {
                                slipQuad = null
                            }
                        }
                    } catch (_: Exception) {
                        slipQuad = null
                    } finally {
                        imageProxy.close()
                    }
                })

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            }, ContextCompat.getMainExecutor(context))
        }
    }

    Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        // Camera preview
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // QR payload display
        if (!payloadText.isNullOrBlank()) {
            Text(
                text = payloadText,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(12.dp)
                    .fillMaxWidth(),
                color = Color.White
            )
        }

        // Top bar: Account (left) and Settings (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onNavigateToAccount,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Account",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        // Loading overlay while scanning / verifying
        if (isLoading) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    text = "Scanning...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Slip frame overlay: draws the live detected quad right around the slip on the
        // preview (mapped from camera-frame px through the same center-crop the preview uses).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewPx = it }
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val quad = slipQuad ?: return@Canvas
                if (viewPx.width == 0 || viewPx.height == 0) return@Canvas
                val rotW = slipRotW.toFloat()
                val rotH = slipRotH.toFloat()
                if (rotW <= 0f || rotH <= 0f) return@Canvas
                val scale = kotlin.math.max(viewPx.width / rotW, viewPx.height / rotH)
                val dispW = rotW * scale
                val dispH = rotH * scale
                val ox = (viewPx.width - dispW) / 2f
                val oy = (viewPx.height - dispH) / 2f
                fun toOffset(p: Pair<Float, Float>) = androidx.compose.ui.geometry.Offset(
                    ox + p.first * scale,
                    oy + p.second * scale
                )
                val path = androidx.compose.ui.graphics.Path().apply {
                    quad.forEachIndexed { i, p ->
                        val o = toOffset(p)
                        if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                    }
                    close()
                }
                drawPath(path, color = Color(0xFF66BB6A), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()))
            }

            // Gentle white guide box while no slip is detected yet
            if (slipQuad == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                    )
                }
            }

            Text(
                text = if (slipQuad != null) "Slip detected — tap shutter" else "Align the slip within the view",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (slipQuad != null) Color(0xFF66BB6A) else Color.White
            )
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 32.dp, end = 32.dp, bottom = 116.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File / media button
            IconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Select file",
                    tint = Color.White
                )
            }

            // Capture (shutter) button
            IconButton(
                onClick = {
                    // Save into persistent app pictures dir so the slip keeps a lasting photo link
                    val photoDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                        ?: context.filesDir
                    val photoFile = File(photoDir, "capture_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture?.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onPhotoCaptured(photoFile.absolutePath)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                // ignore errors for this minimal UI
                            }
                        }
                    )
                },
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Capture photo",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
