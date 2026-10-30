package com.haze.app.ui.registration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.haze.app.ui.components.HazeBrandHeader
import com.haze.app.ui.theme.HazeGreenPrimary
import com.haze.app.ui.theme.HazeOrangeAccent

@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel,
    onRegistrationSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isRegistrationSuccess) {
        if (uiState.isRegistrationSuccess) {
            onRegistrationSuccess()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.currentStep == RegistrationStep.INPUT) {
                    RegistrationInputStep(
                        uiState = uiState,
                        viewModel = viewModel,
                        focusManager = focusManager
                    )
                } else {
                    RegistrationOtpStep(
                        uiState = uiState,
                        viewModel = viewModel,
                        focusManager = focusManager
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }


        }
    }
}

/**
 * Step 1: Input Page (Haze Logo, Name + Phone OR Phone Only if already an account)
 */
@Composable
private fun RegistrationInputStep(
    uiState: RegistrationUiState,
    viewModel: RegistrationViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    val context = LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // App Logo & Brand Header at top
    HazeBrandHeader()

    Spacer(modifier = Modifier.height(24.dp))

    // Heading Texts
    if (uiState.isExistingUserMode) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter your registered phone number to restore your account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter your details to share empty seats and travel affordably.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Full Name Input (ONLY shown in New User mode)
    AnimatedVisibility(
        visible = !uiState.isExistingUserMode,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Column {
            OutlinedTextField(
                value = uiState.fullName,
                onValueChange = { viewModel.onFullNameChange(it) },
                label = { Text("Full Name") },
                placeholder = { Text("e.g. Ramesh Kumar") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = HazeGreenPrimary)
                },
                isError = uiState.fullNameError != null,
                supportingText = {
                    uiState.fullNameError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    // Mobile Number Input
    OutlinedTextField(
        value = uiState.phoneNumber,
        onValueChange = { viewModel.onPhoneNumberChange(it) },
        label = { Text("Mobile Number") },
        placeholder = { Text("10-digit number") },
        prefix = {
            Text(
                text = "+91 ",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = HazeGreenPrimary)
        },
        isError = uiState.phoneError != null,
        supportingText = {
            uiState.phoneError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                (context as? Activity)?.let { viewModel.proceedToOtp(it) }
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )

    // Account Not Found alert banner in existing user mode
    if (uiState.isExistingUserMode && uiState.phoneError != null && uiState.phoneError.contains("register", ignoreCase = true)) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No Account Found",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                    Text(
                        text = "Mobile number not registered. Please create a new account.",
                        fontSize = 12.sp,
                        color = Color(0xFFB91C1C)
                    )
                }
                TextButton(
                    onClick = { viewModel.toggleExistingUserMode(false) }
                ) {
                    Text(
                        text = "Register",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Continue / Get OTP Button
    Button(
        onClick = {
            focusManager.clearFocus()
            (context as? Activity)?.let { viewModel.proceedToOtp(it) }
        },
        enabled = !uiState.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HazeGreenPrimary
        )
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Bottom Toggle Link
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (uiState.isExistingUserMode) "Need a new account " else "Already have an account ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (uiState.isExistingUserMode) "Register" else "Log In",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = HazeOrangeAccent,
            modifier = Modifier.clickable {
                viewModel.toggleExistingUserMode(!uiState.isExistingUserMode)
            }
        )
    }
}

/**
 * Step 2: Dedicated OTP Verification Page with 6 distinct boxes
 */
@Composable
private fun RegistrationOtpStep(
    uiState: RegistrationUiState,
    viewModel: RegistrationViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    val context = LocalContext.current
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // Top Navigation Bar with Back Arrow
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { viewModel.goBackToPhoneInput() },
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "OTP Verification",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Verification Info & Phone Number with Edit icon
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Verification code sent to",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+91 ${uiState.phoneNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = { viewModel.goBackToPhoneInput() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Number",
                    tint = HazeGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
        text = "Enter 6-Digit OTP",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 6-BOX OTP Input Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 6) {
            val digit = uiState.otpDigits.getOrElse(i) { "" }

            OtpSingleDigitBox(
                digit = digit,
                isError = uiState.otpError != null,
                focusRequester = focusRequesters[i],
                onValueChange = { newVal ->
                    viewModel.updateOtpDigit(i, newVal)
                    if (newVal.isNotEmpty()) {
                        if (newVal.length > 1) {
                            // User pasted full OTP
                            focusManager.clearFocus()
                        } else if (i < 5) {
                            focusRequesters[i + 1].requestFocus()
                        } else {
                            focusManager.clearFocus()
                        }
                    }
                },
                onBackspaceOnEmpty = {
                    if (i > 0) {
                        focusRequesters[i - 1].requestFocus()
                    }
                }
            )
        }
    }

    // OTP Error Message
    uiState.otpError?.let { err ->
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = err,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Verify & Continue Button
    Button(
        onClick = {
            focusManager.clearFocus()
            viewModel.verifyOtpAndContinue()
        },
        enabled = !uiState.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HazeGreenPrimary
        )
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = "Verify & Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Resend OTP Section
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Didn't receive code ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (uiState.resendCountdownSeconds > 0) {
            Text(
                text = "Resend in ${uiState.resendCountdownSeconds}s",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
        } else {
            Text(
                text = "Resend OTP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = HazeOrangeAccent,
                modifier = Modifier.clickable {
                    (context as? Activity)?.let { viewModel.resendOtp(it) }
                }
            )
        }
    }


}

/**
 * Individual Single Digit OTP Box
 */
@Composable
private fun OtpSingleDigitBox(
    digit: String,
    isError: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBackspaceOnEmpty: () -> Unit
) {
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        digit.isNotEmpty() -> HazeGreenPrimary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    val backgroundColor = if (digit.isNotEmpty()) {
        HazeGreenPrimary.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = if (digit.isNotEmpty() || isError) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = digit,
            onValueChange = { input ->
                onValueChange(input)
            },
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Backspace && digit.isEmpty()) {
                        onBackspaceOnEmpty()
                        true
                    } else {
                        false
                    }
                }
                .wrapContentSize(Alignment.Center),
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )
    }
}


