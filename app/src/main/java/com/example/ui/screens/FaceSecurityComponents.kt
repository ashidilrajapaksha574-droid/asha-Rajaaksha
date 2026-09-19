package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.FaceEngine
import com.example.data.FaceRegistrationResponse
import com.example.ui.theme.PolishBorder
import android.util.Size
import java.util.concurrent.Executors
import com.example.ui.theme.PolishTextSecondary
import com.example.viewmodel.VideoViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FaceSecurityConsoleCard(
    viewModel: VideoViewModel,
    faceSecurityEnabled: Boolean,
    registeredFaceRatio: Float?,
    registeredFaceImage: String?,
    isFaceVerified: Boolean,
    faceVerificationConfidence: Float,
    faceDetectionStatus: String,
    lastRegistrationResponse: FaceRegistrationResponse?,
    isFaceRegisteringMode: Boolean,
    onRegisteringModeChanged: (Boolean) -> Unit,
    isFaceConsoleExpanded: Boolean,
    onConsoleExpandedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var latestCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var latestRotationDegrees by remember { mutableStateOf(0) }
    var isCapturingInProgress by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.testTag("face_security_console_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, PolishBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row (clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConsoleExpandedChanged(!isFaceConsoleExpanded) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (faceSecurityEnabled && isFaceVerified) {
                                    Color(0xFF2E7D32).copy(alpha = 0.25f)
                                } else if (faceSecurityEnabled) {
                                    Color(0xFFC62828).copy(alpha = 0.2f)
                                } else {
                                    PolishBorder.copy(alpha = 0.3f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (faceSecurityEnabled && isFaceVerified) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = "Face Security Status",
                            tint = if (faceSecurityEnabled && isFaceVerified) {
                                Color(0xFF4CAF50)
                            } else if (faceSecurityEnabled) {
                                Color(0xFFE57373)
                            } else {
                                PolishTextSecondary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Face Auto-Shield Biometrics",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (faceSecurityEnabled) {
                                if (isFaceVerified) "Verified (${(faceVerificationConfidence * 100).toInt()}%) • Video Unlocked"
                                else "Shield Active • Paused"
                            } else {
                                "Biometric Shield Disabled"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (faceSecurityEnabled && isFaceVerified) {
                                Color(0xFF4CAF50)
                            } else if (faceSecurityEnabled) {
                                Color(0xFFE57373)
                            } else {
                                PolishTextSecondary
                            }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (faceSecurityEnabled && isFaceVerified) {
                            Color(0xFF2E7D32).copy(alpha = 0.2f)
                        } else if (faceSecurityEnabled) {
                            Color(0xFFC62828).copy(alpha = 0.2f)
                        } else {
                            Color.DarkGray
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (faceSecurityEnabled && isFaceVerified) "VERIFIED" else if (faceSecurityEnabled) "SHIELD ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (faceSecurityEnabled && isFaceVerified) Color(0xFF81C784) else if (faceSecurityEnabled) Color(0xFFFF8A80) else Color.LightGray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = if (isFaceConsoleExpanded) "▲" else "▼",
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishTextSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isFaceConsoleExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = PolishBorder, modifier = Modifier.padding(bottom = 12.dp))

                    if (!cameraPermissionState.status.isGranted) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Camera permission is required for face registration and automatic playback verification.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PolishTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Grant Camera Permission", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Camera preview block with live scanning laser
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.5.dp,
                                        color = if (faceSecurityEnabled && isFaceVerified) Color(0xFF4CAF50)
                                        else if (faceSecurityEnabled) Color(0xFFE57373)
                                        else MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CameraPreviewWithFaceDetection(
                                    viewModel = viewModel,
                                    isRegistering = isFaceRegisteringMode,
                                    onFrameCaptured = { bitmap, rotation ->
                                        latestCapturedBitmap = bitmap
                                        latestRotationDegrees = rotation
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Scanning laser effect
                                val infiniteTransition = rememberInfiniteTransition(label = "laser")
                                val laserY by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "laser_y"
                                )
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val canvasHeight = this.size.height
                                    val canvasWidth = this.size.width
                                    val y = canvasHeight * laserY
                                    drawLine(
                                        color = if (faceSecurityEnabled && isFaceVerified) Color(0xFF81C784) else Color(0xFF00E6FF),
                                        start = androidx.compose.ui.geometry.Offset(0f, y),
                                        end = androidx.compose.ui.geometry.Offset(canvasWidth, y),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }

                            // Info and switch block
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Detection Status:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PolishTextSecondary
                                )
                                Text(
                                    text = faceDetectionStatus,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        faceDetectionStatus.contains("Verified", ignoreCase = true) ||
                                        faceDetectionStatus.contains("Saved", ignoreCase = true) ||
                                        faceDetectionStatus.contains("Match", ignoreCase = true) -> Color(0xFF81C784)
                                        faceDetectionStatus.contains("Shield", ignoreCase = true) ||
                                        faceDetectionStatus.contains("Unauthorized", ignoreCase = true) ||
                                        faceDetectionStatus.contains("Denied", ignoreCase = true) -> Color(0xFFE57373)
                                        else -> MaterialTheme.colorScheme.onBackground
                                    }
                                )

                                if (registeredFaceRatio != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Auto-Shield Protection",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Switch(
                                            checked = faceSecurityEnabled,
                                            onCheckedChange = { viewModel.setFaceSecurityEnabled(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF4CAF50),
                                                checkedTrackColor = Color(0xFF2E7D32).copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Register your face to enable automatic face recognition play & shield.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PolishTextSecondary
                                    )
                                }
                            }
                        }

                        // Registered Face Profile Preview
                        if (registeredFaceImage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, PolishBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, Color(0xFF4CAF50), CircleShape)
                                    ) {
                                        AsyncImage(
                                            model = registeredFaceImage,
                                            contentDescription = "Registered Face Profile",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Registered Biometric Baseline",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Base64 Face Decoded & Saved • Ratio: ${String.format("%.2f", registeredFaceRatio ?: 1f)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PolishTextSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Profile Active",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Last Status message from registration or verification
                        if (lastRegistrationResponse != null && !lastRegistrationResponse.success) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = lastRegistrationResponse.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isFaceRegisteringMode) {
                                Button(
                                    onClick = {
                                        val bitmap = latestCapturedBitmap
                                        if (bitmap != null && !isCapturingInProgress) {
                                            isCapturingInProgress = true
                                            FaceEngine.registerFaceFromBitmap(
                                                bitmap = bitmap,
                                                rotationDegrees = latestRotationDegrees
                                            ) { response ->
                                                isCapturingInProgress = false
                                                viewModel.saveFaceRegistration(response)
                                                if (response.success) {
                                                    onRegisteringModeChanged(false)
                                                }
                                            }
                                        }
                                    },
                                    enabled = latestCapturedBitmap != null && !isCapturingInProgress,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isCapturingInProgress) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("Capture & Save Face", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onRegisteringModeChanged(false) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                            } else {
                                Button(
                                    onClick = { onRegisteringModeChanged(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (registeredFaceRatio == null) "Register Face" else "Recalibrate Face",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (registeredFaceRatio != null) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearRegisteredFace() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                                        border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Clear")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewWithFaceDetection(
    viewModel: VideoViewModel,
    isRegistering: Boolean,
    onFrameCaptured: (Bitmap, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        FaceDetection.getClient(options)
    }

    val isDisposed = remember { booleanArrayOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            isDisposed[0] = true
            try {
                if (cameraProviderFuture.isDone) {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                faceDetector.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                cameraExecutor.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            cameraProviderFuture.addListener({
                if (isDisposed[0]) return@addListener
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(320, 240))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isDisposed[0]) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                    // ONLY extract full frame bitmap when user is on the Registration step
                    // Never extract Bitmap during holding/watching video or web to avoid main thread freeze
                    if (isRegistering) {
                        val frameBitmap = try {
                            imageProxy.toBitmap()
                        } catch (e: Exception) {
                            null
                        }

                        if (frameBitmap != null) {
                            onFrameCaptured(frameBitmap, rotationDegrees)
                        }
                    }

                    @SuppressLint("UnsafeOptInUsageError")
                    val mediaImage = imageProxy.image
                    val isHolding = viewModel.videoPlaying.value
                    if (mediaImage != null && (isRegistering || isHolding)) {
                        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                        faceDetector.process(image)
                            .addOnSuccessListener { faces ->
                                if (isDisposed[0]) return@addOnSuccessListener
                                val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

                                if (isRegistering) {
                                    if (primaryFace != null) {
                                        viewModel.setFaceDetectionStatus("Face Detected (Tap 'Capture & Save')")
                                    } else {
                                        viewModel.setFaceDetectionStatus("Looking for Face...")
                                    }
                                } else if (viewModel.videoPlaying.value) {
                                    // Live verification loop actively verifying face while holding button
                                    viewModel.updateFaceVerification(primaryFace)
                                }
                            }
                            .addOnFailureListener {
                                if (isDisposed[0]) return@addOnFailureListener
                                viewModel.setFaceDetectionStatus("Detection Error")
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    if (!isDisposed[0]) {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}
