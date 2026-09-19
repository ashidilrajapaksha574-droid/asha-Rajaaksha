package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.VideoViewModel

@Composable
fun SettingsScreen(
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    val recoveryQuestion by viewModel.recoveryQuestion.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App settings banner
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
            border = BorderStroke(1.dp, PolishBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SECURITY CONSOLE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PolishDarkText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Configure and manage access security credentials for your video vault.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PolishDarkText.copy(alpha = 0.8f)
                )
            }
        }

        // Lock Settings Section
        SettingsGroupHeader(title = "App Protection")

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, PolishBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsItemRow(
                    icon = Icons.Default.Lock,
                    title = "Recovery Question",
                    subtitle = recoveryQuestion,
                    action = {}
                )

                HorizontalDivider(color = PolishBorder, modifier = Modifier.padding(vertical = 12.dp))

                SettingsItemRow(
                    icon = Icons.Default.Refresh,
                    title = "Reset Security Credentials",
                    subtitle = "Wipe passcode and recovery questions to start fresh.",
                    actionColor = MaterialTheme.colorScheme.error,
                    actionText = "Reset Lock",
                    action = { showResetDialog = true },
                    testTag = "reset_lock_row"
                )
            }
        }

        // About the Secure 99box technology
        SettingsGroupHeader(title = "Information & Guard Tech")

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, PolishBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "How the 99box Player works",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "This application guarantees safe watch sessions. When utilizing the 99box screen:\n\n" +
                            "• A pitch-black Cover Guard completely blocks and overlays the video canvas.\n" +
                            "• The YouTube video remains strictly paused until the large red circular button is actively held down.\n" +
                            "• Once your finger loses contact with the screen, the system immediately applies a script execution pause call and snaps the Cover Guard back over the canvas instantly.\n" +
                            "• Startup passcode ensures only authorized users can browse or add videos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PolishTextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App version info
        Text(
            text = "99box Secure Video Player v1.0.0\nSecure Vault Engine Active",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = PolishTextSecondary,
            textAlign = TextAlign.Center
        )
    }

    // Confirmation Dialog for Lock Reset
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Security Credentials?") },
            text = { Text("This will erase your secure login passcode and security recovery setups. The app will prompt for a new passcode setup on next launch.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetLock()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Reset Lock", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = PolishTextSecondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = PolishTextSecondary
        )
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        ),
        color = PolishTextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    actionColor: Color = MaterialTheme.colorScheme.primary,
    action: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = action)
            .padding(vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishTextSecondary
                )
            }
        }

        if (actionText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = actionColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
