package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.example.ui.theme.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.LockState
import com.example.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    val lockState by viewModel.lockState.collectAsState()
    val passcode by viewModel.passcode.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val recoveryQuestion by viewModel.recoveryQuestion.collectAsState()
    val recoveryAnswerInput by viewModel.recoveryAnswerInput.collectAsState()

    var selectedQuestion by remember { mutableStateOf("What was the name of your first pet?") }
    var answerText by remember { mutableStateOf("") }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBg)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when (lockState) {
                        LockState.SETUP_PASSWORD -> "Create Secure Password"
                        LockState.SETUP_RECOVERY -> "Recovery Setup"
                        LockState.ENTER_PASSWORD -> "Enter Passcode"
                        LockState.RECOVERY_RESET -> "Verify Identity"
                        else -> "99box App Lock"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (lockState) {
                        LockState.SETUP_PASSWORD -> "Set a passcode to protect your videos."
                        LockState.SETUP_RECOVERY -> "Choose a recovery question to reset password later."
                        LockState.ENTER_PASSWORD -> "Input your code to unlock the private video vaults."
                        LockState.RECOVERY_RESET -> "Answer your recovery question to reset."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = PolishTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (authError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = authError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Central Interactive Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (lockState) {
                    LockState.SETUP_PASSWORD, LockState.ENTER_PASSWORD -> {
                        // Passcode dots indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val maxDigits = if (lockState == LockState.SETUP_PASSWORD) 6 else 6
                            repeat(maxDigits) { index ->
                                val active = index < passcode.length
                                val scale by animateFloatAsState(
                                    targetValue = if (active) 1.2f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary
                                            else PolishBorder
                                        )
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                    LockState.SETUP_RECOVERY -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            val questions = listOf(
                                "What was the name of your first pet?",
                                "What city were you born in?",
                                "What is your favorite book/movie?",
                                "What was the name of your first school?",
                                "What is your dream holiday destination?"
                            )

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = selectedQuestion,
                                    onValueChange = {},
                                    label = { Text("Recovery Question") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        unfocusedBorderColor = PolishBorder,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    questions.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            text = { Text(text = selectionOption, color = MaterialTheme.colorScheme.onBackground) },
                                            onClick = {
                                                selectedQuestion = selectionOption
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = answerText,
                                onValueChange = { answerText = it },
                                label = { Text("Security Answer") },
                                placeholder = { Text("Type recovery answer...") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = PolishBorder
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("security_answer_input")
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    viewModel.submitSecuritySetup(selectedQuestion, answerText)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("submit_security_setup_button")
                            ) {
                                Text("Complete Setup", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    LockState.RECOVERY_RESET -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Security Question:",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = recoveryQuestion,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = recoveryAnswerInput,
                                onValueChange = { viewModel.onAnswerInputChanged(it) },
                                label = { Text("Your Answer") },
                                placeholder = { Text("Type recovery answer...") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = PolishBorder
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recovery_verify_input")
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { viewModel.submitRecoveryAnswer() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("submit_recovery_answer_button")
                            ) {
                                Text("Verify & Reset Password", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(onClick = { viewModel.navigateToEnterPassword() }) {
                                Text("Back to Password Entry", color = PolishTextSecondary)
                            }
                        }
                    }
                    else -> {}
                }
            }

            // Keypad Screen / Footer Section
            if (lockState == LockState.SETUP_PASSWORD || lockState == LockState.ENTER_PASSWORD) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("Clear", "0", "Delete")
                    )

                    keyRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                KeypadButton(
                                    text = key,
                                    onClick = {
                                        when (key) {
                                            "Clear" -> viewModel.handleClearPress()
                                            "Delete" -> viewModel.handleDeletePress()
                                            else -> viewModel.handleKeyPress(key)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (lockState == LockState.ENTER_PASSWORD) {
                            TextButton(
                                onClick = { viewModel.navigateToRecovery() },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text("Forgot Password?", color = MaterialTheme.colorScheme.secondary)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Button(
                            onClick = {
                                if (lockState == LockState.SETUP_PASSWORD) {
                                    viewModel.submitSetupPassword()
                                } else {
                                    viewModel.submitPasscode()
                                }
                            },
                            enabled = passcode.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lockState == LockState.SETUP_PASSWORD)
                                    MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.primary,
                                disabledContainerColor = PolishBorder.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .testTag("enter_submit_button")
                        ) {
                            Text(
                                text = if (lockState == LockState.SETUP_PASSWORD) "Setup" else "Unlock",
                                fontWeight = FontWeight.Bold,
                                color = if (passcode.isNotEmpty()) {
                                    if (lockState == LockState.SETUP_PASSWORD) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                                } else PolishTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    val isAction = text == "Clear" || text == "Delete"
    val containerColor = if (isAction) {
        MaterialTheme.colorScheme.surface
    } else {
        PolishSecondary.copy(alpha = 0.3f)
    }
    val textColor = if (isAction) PolishTextSecondary else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .testTag("keypad_btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        if (text == "Delete") {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Backspace",
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = text,
                style = if (isAction) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        else MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
