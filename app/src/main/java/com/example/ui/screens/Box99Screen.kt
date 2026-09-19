package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.viewmodel.VideoViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Box99Screen(
    viewModel: VideoViewModel,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeVideo by viewModel.activeVideo.collectAsState()
    val videoPlaying by viewModel.videoPlaying.collectAsState()
    val faceSecurityEnabled by viewModel.faceSecurityEnabled.collectAsState()
    val registeredFaceRatio by viewModel.registeredFaceRatio.collectAsState()
    val registeredFaceImage by viewModel.registeredFaceImage.collectAsState()
    val isFaceVerified by viewModel.isFaceVerified.collectAsState()
    val faceVerificationConfidence by viewModel.faceVerificationConfidence.collectAsState()
    val faceDetectionStatus by viewModel.faceDetectionStatus.collectAsState()
    val lastRegistrationResponse by viewModel.lastRegistrationResponse.collectAsState()

    var isFaceRegisteringMode by remember { mutableStateOf(false) }
    var isFaceConsoleExpanded by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }

    var viewerMode by remember { mutableStateOf("VIDEO") } // "VIDEO" or "WEB"
    var webUrlInput by remember { mutableStateOf("https://www.google.com") }
    var activeWebUrl by remember { mutableStateOf("https://www.google.com") }
    var browserWebView by remember { mutableStateOf<WebView?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Pulsing animation for the hold overlay placeholder
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Button scale animation when pressed
    val buttonScale by animateFloatAsState(
        targetValue = if (videoPlaying) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    val infiniteTransitionRotate = rememberInfiniteTransition(label = "rotate")
    val rotationAngle by infiniteTransitionRotate.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // DUAL-CONDITION VERIFICATION:
    // The video player ONLY stays visible and playing if BOTH conditions are true simultaneously:
    // 1. User is actively pressing and holding down the "Press & Hold to Watch" button (videoPlaying)
    // 2. Face is actively showing in camera and verified (isFaceVerified)
    // If either condition becomes false, freeze the video and show the black overlay instantly.
    val shouldPlay = videoPlaying && isFaceVerified

    // Trigger video.play() or video.pause() inside the player
    LaunchedEffect(shouldPlay) {
        val webView = webViewInstance ?: return@LaunchedEffect
        if (shouldPlay) {
            webView.evaluateJavascript("playVideo()", null)
        } else {
            webView.evaluateJavascript("pauseVideo()", null)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val buttonDiameter = 96.dp
        val buttonDiameterPx = with(density) { buttonDiameter.toPx() }

        var buttonOffsetX by remember { mutableFloatStateOf(0f) }
        var buttonOffsetY by remember { mutableFloatStateOf(0f) }
        var isPositionInitialized by remember { mutableStateOf(false) }

        val minX = with(density) { 12.dp.toPx() }
        val maxX = (screenWidthPx - buttonDiameterPx - with(density) { 12.dp.toPx() }).coerceAtLeast(minX)
        val minY = with(density) { 16.dp.toPx() }
        val maxY = (screenHeightPx - buttonDiameterPx - with(density) { 24.dp.toPx() }).coerceAtLeast(minY)

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            if (!isPositionInitialized && screenWidthPx > 0f && screenHeightPx > 0f) {
                buttonOffsetX = (screenWidthPx - buttonDiameterPx) / 2f
                buttonOffsetY = screenHeightPx - buttonDiameterPx - with(density) { 36.dp.toPx() }
                isPositionInitialized = true
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Shield",
                            tint = PolishDarkText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "99box Secure Player",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (faceSecurityEnabled) "FACE RECOGNITION ACTIVE" else "STEALTH GUARD ARMED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            ),
                            color = if (faceSecurityEnabled) Color(0xFF4CAF50) else PolishTextSecondary
                        )
                    }
                }
                Box {
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Install App on Phone") },
                            onClick = {
                                isMenuExpanded = false
                                showInstallDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Install Information"
                                )
                            }
                        )
                    }
                }
            }
            HorizontalDivider(
                color = PolishBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Face Security & Biometrics Console Card
        FaceSecurityConsoleCard(
            viewModel = viewModel,
            faceSecurityEnabled = faceSecurityEnabled,
            registeredFaceRatio = registeredFaceRatio,
            registeredFaceImage = registeredFaceImage,
            isFaceVerified = isFaceVerified,
            faceVerificationConfidence = faceVerificationConfidence,
            faceDetectionStatus = faceDetectionStatus,
            lastRegistrationResponse = lastRegistrationResponse,
            isFaceRegisteringMode = isFaceRegisteringMode,
            onRegisteringModeChanged = { isFaceRegisteringMode = it },
            isFaceConsoleExpanded = isFaceConsoleExpanded,
            onConsoleExpandedChanged = { isFaceConsoleExpanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        // Mode Selector: Video Vault vs Secure Web
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .border(1.dp, PolishBorder, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { viewerMode = "VIDEO" }
                    .testTag("tab_video_mode"),
                color = if (viewerMode == "VIDEO") MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video Mode",
                        tint = if (viewerMode == "VIDEO") Color.White else PolishTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Video Vault",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (viewerMode == "VIDEO") Color.White else PolishTextSecondary
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { viewerMode = "WEB" }
                    .testTag("tab_web_mode"),
                color = if (viewerMode == "WEB") MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Web Mode",
                        tint = if (viewerMode == "WEB") Color.White else PolishTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Web Browser",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (viewerMode == "WEB") Color.White else PolishTextSecondary
                    )
                }
            }
        }

        if (viewerMode == "WEB") {
            // SECURE WEB BROWSER MODE (Protected by Face ID + Button Hold)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Address Bar & Controls
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, PolishBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { browserWebView?.goBack() },
                                modifier = Modifier.size(36.dp),
                                enabled = browserWebView?.canGoBack() == true
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (browserWebView?.canGoBack() == true) MaterialTheme.colorScheme.onSurface else PolishTextSecondary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { browserWebView?.goForward() },
                                modifier = Modifier.size(36.dp),
                                enabled = browserWebView?.canGoForward() == true
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Forward",
                                    tint = if (browserWebView?.canGoForward() == true) MaterialTheme.colorScheme.onSurface else PolishTextSecondary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { browserWebView?.reload() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // URL Input Field
                            OutlinedTextField(
                                value = webUrlInput,
                                onValueChange = { webUrlInput = it },
                                singleLine = true,
                                placeholder = { Text("Search or type URL...", fontSize = 12.sp) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val query = webUrlInput.trim()
                                            val target = if (!query.startsWith("http://") && !query.startsWith("https://")) {
                                                if (query.contains(".") && !query.contains(" ")) {
                                                    "https://$query"
                                                } else {
                                                    "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                                                }
                                            } else {
                                                query
                                            }
                                            activeWebUrl = target
                                            browserWebView?.loadUrl(target)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Go",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        val query = webUrlInput.trim()
                                        val target = if (!query.startsWith("http://") && !query.startsWith("https://")) {
                                            if (query.contains(".") && !query.contains(" ")) {
                                                "https://$query"
                                            } else {
                                                "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                                            }
                                        } else {
                                            query
                                        }
                                        activeWebUrl = target
                                        browserWebView?.loadUrl(target)
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("web_url_input"),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = PolishBorder
                                )
                            )
                        }

                        // Bookmark Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Google" to "https://www.google.com",
                                "Wikipedia" to "https://www.wikipedia.org",
                                "DuckDuckGo" to "https://duckduckgo.com",
                                "GitHub" to "https://github.com"
                            ).forEach { (name, url) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(0.5.dp, PolishBorder),
                                    modifier = Modifier
                                        .clickable {
                                            webUrlInput = url
                                            activeWebUrl = url
                                            browserWebView?.loadUrl(url)
                                        }
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Secure Web View Container with Dark Security Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF141613))
                        .border(
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (shouldPlay) MaterialTheme.colorScheme.primary else PolishBorder
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                ) {
                    SecureWebBrowserView(
                        url = activeWebUrl,
                        onWebViewCreated = { browserWebView = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Dark Security Shield Overlay (Disappears ONLY when holding button AND face verified!)
                    val isOverlayVisible = !shouldPlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isOverlayVisible,
                        enter = fadeIn(animationSpec = tween(120)),
                        exit = fadeOut(animationSpec = tween(100))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xF0101210))
                                .testTag("secure_web_overlay"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            color = if (faceSecurityEnabled) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Web Locked",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .scale(if (isOverlayVisible) pulseAlpha else 1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (faceSecurityEnabled) {
                                        if (registeredFaceRatio != null) "WEB SHIELD ACTIVE • LOOK AT CAMERA" else "WEB SHIELD ACTIVE • NO FACE REGISTERED"
                                    } else {
                                        "WEB GUARD • HOLD TRIGGER"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = if (faceSecurityEnabled) Color(0xFFFF8A80) else Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeWebUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = if (videoPlaying) Color(0xFF2E7D32).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (videoPlaying) Color(0xFF81C784) else Color.White.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (videoPlaying) Icons.Default.Face else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (videoPlaying) "SCANNING FACE..." else "PRESS & HOLD BUTTON TO BROWSE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (videoPlaying && isFaceVerified) "Dual Security Verified • Web Unlocked"
                    else if (videoPlaying) "Holding Trigger • Verifying Face..."
                    else if (isFaceVerified) "Face Detected • Press & Hold Button"
                    else "Press & Hold to Browse",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (videoPlaying && isFaceVerified) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Dual-Lock Watchdog: Webpage is visible ONLY while holding the button AND your face is continuously detected and verified by AI. Releasing or looking away hides the webpage instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else if (activeVideo == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 20.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, PolishBorder), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "No video selected",
                        tint = PolishTextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Active Video Selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a video from your library (Firebase Storage or YouTube) to start secure playback.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PolishTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToLibrary,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Open Video Library", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val video = activeVideo!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Video Container with Secure Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.77f) // aspect-video
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF141613))
                        .border(
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (shouldPlay) MaterialTheme.colorScheme.primary else PolishBorder
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                ) {
                    // 1. Secure Video Player (HTML5 <video> for Firebase/Direct streams & YouTube for YouTube IDs)
                    SecureVideoPlayerView(
                        videoSource = video.videoId,
                        onWebViewCreated = { webViewInstance = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. Dark Security Cover Overlay (Disappears automatically when shouldPlay is true!)
                    val isOverlayVisible = !shouldPlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isOverlayVisible,
                        enter = fadeIn(animationSpec = tween(120)),
                        exit = fadeOut(animationSpec = tween(100))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xF0101210))
                                .testTag("secure_guard_overlay"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Backdrop scrim
                            if (!video.videoId.startsWith("http")) {
                                AsyncImage(
                                    model = "https://img.youtube.com/vi/${video.videoId}/mqdefault.jpg",
                                    contentDescription = "Video backdrop",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawBehind {
                                            drawRect(Color(0xEE141613))
                                        }
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = if (faceSecurityEnabled) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Video Locked",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .scale(if (isOverlayVisible) pulseAlpha else 1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (faceSecurityEnabled) {
                                        if (registeredFaceRatio != null) "SHIELD ACTIVE • LOOK AT CAMERA" else "SHIELD ACTIVE • NO FACE REGISTERED"
                                    } else {
                                        "LOCKED • HOLD TRIGGER"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = if (faceSecurityEnabled) Color(0xFFFF8A80) else Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = if (videoPlaying) Color(0xFF2E7D32).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (videoPlaying) Color(0xFF81C784) else Color.White.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (videoPlaying) Icons.Default.Face else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (videoPlaying) "SCANNING FACE..." else "PRESS & HOLD BUTTON TO WATCH",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Bottom Left video title overlay (Pill)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = 200.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controller details below player
                Text(
                    text = if (videoPlaying && isFaceVerified) "Dual Security Verified • Streaming"
                    else if (videoPlaying) "Holding Trigger • Verifying Face..."
                    else if (isFaceVerified) "Face Detected • Press & Hold Button"
                    else "Press & Hold to Watch",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (videoPlaying && isFaceVerified) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dual-Lock Watchdog: Stream plays ONLY while holding the button AND your face is continuously detected and verified by AI. Releasing or looking away freezes the video and shields the screen instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        // Bottom spacer reserving room for the movable floating button
        Spacer(modifier = Modifier.height(115.dp))
    }

    // FULL SCREEN BLACK PAGE (Triggers instantly when hold is released or face unverified)
    val showBlackoutPage = !shouldPlay && !isFaceRegisteringMode
    AnimatedVisibility(
        visible = showBlackoutPage,
        enter = fadeIn(animationSpec = tween(120)),
        exit = fadeOut(animationSpec = tween(100)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .testTag("fullscreen_blackout_page"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color(0xFF141414), CircleShape)
                        .border(1.5.dp, Color(0xFFD32F2F).copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Full Screen Blackout",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier
                            .size(36.dp)
                            .scale(pulseAlpha)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "FULL SCREEN BLACKOUT ACTIVE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isFaceVerified) {
                        "Face Verified • Press & Hold Trigger to Unlock"
                    } else {
                        "Look at Front Camera & Hold Trigger to Unlock"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isFaceVerified) Color(0xFF4CAF50) else Color(0xFFFF5252), CircleShape)
                        )
                        Text(
                            text = if (viewerMode == "WEB") "Target: Secure Web ($activeWebUrl)" else "Target: ${activeVideo?.title ?: "Video Vault"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Tip: Hold the red button with your finger and slide up or down to reposition it anywhere on screen.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // MOVABLE FLOATING HOLD BUTTON (Hold & drag up or down freely)
    val canInteract = (viewerMode == "WEB") || (activeVideo != null)
    Box(
        modifier = Modifier
            .offset { IntOffset(buttonOffsetX.roundToInt(), buttonOffsetY.roundToInt()) }
            .size(buttonDiameter)
            .pointerInput(canInteract) {
                if (!canInteract) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        viewModel.setVideoPlaying(true)
                        val pointerId = down.id
                        down.consume()

                        var isDown = true
                        while (isDown) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                isDown = false
                                viewModel.setVideoPlaying(false)
                            } else {
                                val delta = change.positionChange()
                                if (delta != androidx.compose.ui.geometry.Offset.Zero) {
                                    buttonOffsetX = (buttonOffsetX + delta.x).coerceIn(minX, maxX)
                                    buttonOffsetY = (buttonOffsetY + delta.y).coerceIn(minY, maxY)
                                    change.consume()
                                }
                            }
                        }
                        viewModel.setVideoPlaying(false)
                    }
                }
            }
            .testTag("movable_hold_trigger_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer Dashed Rotating Ring
        Box(
            modifier = Modifier
                .size(buttonDiameter)
                .rotate(rotationAngle)
                .drawBehind {
                    drawCircle(
                        color = if (shouldPlay) Color(0xFF4CAF50).copy(alpha = 0.6f) else PolishTertiary.copy(alpha = 0.45f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    )
                }
        )

        // Central Active Button
        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(buttonScale)
                .clip(CircleShape)
                .background(if (shouldPlay) Color(0xFF2E7D32) else PolishTertiary)
                .border(
                    border = BorderStroke(5.dp, if (shouldPlay) Color(0xFF81C784) else PolishRedContainer),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(2.5.dp, Color.White, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (videoPlaying) "MOVE" else "HOLD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
            }
        }

        // Attached Status Pill
        Surface(
            color = Color(0xDD000000),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (shouldPlay) Color(0xFF4CAF50) else PolishBorder),
            modifier = Modifier
                .align(if (buttonOffsetY > screenHeightPx / 2f) Alignment.TopCenter else Alignment.BottomCenter)
                .offset(y = if (buttonOffsetY > screenHeightPx / 2f) (-28).dp else 28.dp)
        ) {
            Text(
                text = if (videoPlaying && isFaceVerified) {
                    if (viewerMode == "WEB") "UNLOCKED • DRAGGABLE" else "STREAMING • DRAGGABLE"
                } else if (videoPlaying) {
                    "SCANNING FACE..."
                } else {
                    "HOLD & MOVE"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = if (shouldPlay) Color(0xFF81C784) else Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Install 99box on Your Phone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "To run this application on your real physical phone with front camera face recognition:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "1. Export APK from AI Studio",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Click 'Download APK' in the top right menu of AI Studio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(color = PolishBorder.copy(alpha = 0.5f))
                            Text(
                                text = "2. Install on Device",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Open the downloaded APK on your Android device to install and enjoy secure face-protected video playback.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInstallDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Got It", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}

/**
 * Universal video player supporting both:
 * 1. HTML5 <video> for Firebase Storage URLs, Google Cloud Storage, direct MP4/WebM URLs
 * 2. YouTube IFrame API for YouTube videos
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SecureVideoPlayerView(
    videoSource: String,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDirectVideo = remember(videoSource) {
        videoSource.startsWith("http://") || videoSource.startsWith("https://") &&
        !videoSource.contains("youtu")
    }

    val htmlContent = remember(videoSource, isDirectVideo) {
        if (isDirectVideo) {
            getHtml5VideoPlayerHtml(videoSource)
        } else {
            getYoutubeHtml(videoSource)
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        return true
                    }
                }
                webChromeClient = WebChromeClient()
                settings.apply {
                    javaScriptEnabled = true
                    // CRITICAL: allows programmatic video.play() and audio playback without user tap
                    mediaPlaybackRequiresUserGesture = false
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    allowFileAccess = true
                    allowContentAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                }
                tag = videoSource
                onWebViewCreated(this)
                val baseUrl = if (isDirectVideo) "https://firebasestorage.googleapis.com" else "https://www.youtube-nocookie.com"
                loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            if (webView.tag != videoSource) {
                webView.tag = videoSource
                val baseUrl = if (isDirectVideo) "https://firebasestorage.googleapis.com" else "https://www.youtube-nocookie.com"
                webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
    )
}

/**
 * Generates responsive HTML5 <video> player page for Firebase Storage and cloud video URLs.
 */
private fun getHtml5VideoPlayerHtml(videoUrl: String): String {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            html, body {
                width: 100%;
                height: 100%;
                background-color: #000000;
                overflow: hidden;
            }
            #videoContainer {
                width: 100%;
                height: 100%;
                display: flex;
                align-items: center;
                justify-content: center;
            }
            video {
                width: 100%;
                height: 100%;
                object-fit: contain;
                background-color: #000000;
            }
        </style>
    </head>
    <body>
        <div id="videoContainer">
            <video id="player" src="$videoUrl" playsinline preload="auto" controls></video>
            <div id="overlay" style="position:fixed; top:0; left:0; width:100%; height:100%; background-color:#000000; z-index:999999; display:block; pointer-events:none;"></div>
        </div>
        <script>
            var video = document.getElementById('player');
            var overlay = document.getElementById('overlay');
            function playVideo() {
                try {
                    if (overlay) {
                        overlay.style.display = 'none';
                    }
                    if (video) {
                        var promise = video.play();
                        if (promise !== undefined) {
                            promise.catch(function(error) {
                                console.log("video.play error: " + error);
                            });
                        }
                    }
                } catch(e) {
                    console.log("playVideo error: " + e);
                }
            }
            function pauseVideo() {
                try {
                    if (overlay) {
                        overlay.style.display = 'block';
                    }
                    if (video) {
                        video.pause();
                    }
                } catch(e) {
                    console.log("pauseVideo error: " + e);
                }
            }
        </script>
    </body>
    </html>
    """.trimIndent()
}

/**
 * Generates YouTube player page with postMessage and JS play/pause control.
 */
private fun getYoutubeHtml(videoId: String): String {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            html, body {
                width: 100%;
                height: 100%;
                background-color: #000000;
                overflow: hidden;
            }
            #player-container {
                width: 100%;
                height: 100%;
                display: flex;
                align-items: center;
                justify-content: center;
            }
            iframe {
                width: 100%;
                height: 100%;
                border: 0;
            }
        </style>
    </head>
    <body>
        <div id="player-container">
            <iframe id="player"
                type="text/html"
                src="https://www.youtube-nocookie.com/embed/$videoId?enablejsapi=1&autoplay=1&playsinline=1&controls=1&rel=0&modestbranding=1&fs=1&origin=https://www.youtube-nocookie.com"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen>
            </iframe>
            <div id="overlay" style="position:fixed; top:0; left:0; width:100%; height:100%; background-color:#000000; z-index:999999; display:block; pointer-events:none;"></div>
        </div>
        <script>
            var iframe = document.getElementById('player');
            var overlay = document.getElementById('overlay');
            function playVideo() {
                try {
                    if (overlay) {
                        overlay.style.display = 'none';
                    }
                    if (iframe && iframe.contentWindow) {
                        iframe.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                    }
                } catch(e) {}
            }
            function pauseVideo() {
                try {
                    if (overlay) {
                        overlay.style.display = 'block';
                    }
                    if (iframe && iframe.contentWindow) {
                        iframe.contentWindow.postMessage('{"event":"command","func":"pauseVideo","args":""}', '*');
                    }
                } catch(e) {}
            }
        </script>
    </body>
    </html>
    """.trimIndent()
}

/**
 * Secure interactive Web Browser with hardware acceleration and full JavaScript/DOM support.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SecureWebBrowserView(
    url: String,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        return true
                    }
                }
                webChromeClient = WebChromeClient()
                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            if (webView.url != url && url.isNotBlank()) {
                webView.loadUrl(url)
            }
        },
        modifier = modifier
    )
}

