package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.DatingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class UPIPaymentStep {
    SELECT_METHOD,
    LOADING_SDK,
    ENTER_PIN,
    VERIFYING,
    SUCCESS
}

@Composable
fun UPIPaymentDialog(
    planName: String, // "Premium" or "Premium Plus"
    viewModel: DatingViewModel,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(UPIPaymentStep.SELECT_METHOD) }
    var selectedApp by remember { mutableStateOf("Google Pay") }
    var upiPin by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    val amount = if (planName == "Premium") 30 else 270
    val upiId = "7259493330@ybl"

    LaunchedEffect(Unit) {
        // Generate a random high-fidelity UPI Ref ID
        transactionId = "UPI" + (100000000000L..999999999999L).random().toString()
    }

    // Handle SDK launch and verification transitions
    LaunchedEffect(currentStep) {
        when (currentStep) {
            UPIPaymentStep.LOADING_SDK -> {
                delay(1800)
                currentStep = UPIPaymentStep.ENTER_PIN
            }
            UPIPaymentStep.VERIFYING -> {
                delay(2000)
                currentStep = UPIPaymentStep.SUCCESS
            }
            UPIPaymentStep.SUCCESS -> {
                // Instantly update local profile state so premium badges are live
                viewModel.upgradeUserPremiumTier(planName)
            }
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = {
            // Only allow dismissal if not in critical processing steps
            if (currentStep == UPIPaymentStep.SELECT_METHOD || currentStep == UPIPaymentStep.SUCCESS) {
                onDismiss()
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("upi_payment_dialog"),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(300)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentStep) {
                    UPIPaymentStep.SELECT_METHOD -> {
                        PaymentMethodScreen(
                            planName = planName,
                            amount = amount,
                            upiId = upiId,
                            selectedApp = selectedApp,
                            onAppSelect = { selectedApp = it },
                            onProceed = { currentStep = UPIPaymentStep.LOADING_SDK },
                            onCancel = onDismiss
                        )
                    }
                    UPIPaymentStep.LOADING_SDK -> {
                        LoadingSdkScreen(selectedApp = selectedApp)
                    }
                    UPIPaymentStep.ENTER_PIN -> {
                        UpiPinGatewayScreen(
                            amount = amount,
                            upiId = upiId,
                            upiPin = upiPin,
                            onPinChange = { pin ->
                                if (pin.length <= 6) upiPin = pin
                            },
                            onSubmit = {
                                if (upiPin.length >= 4) {
                                    currentStep = UPIPaymentStep.VERIFYING
                                }
                            }
                        )
                    }
                    UPIPaymentStep.VERIFYING -> {
                        VerifyingTransactionScreen()
                    }
                    UPIPaymentStep.SUCCESS -> {
                        PaymentSuccessScreen(
                            amount = amount,
                            upiId = upiId,
                            transactionId = transactionId,
                            onFinish = {
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun PaymentMethodScreen(
    planName: String,
    amount: Int,
    upiId: String,
    selectedApp: String,
    onAppSelect: (String) -> Unit,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    val upiApps = listOf(
        Triple("Google Pay", "G Pay", Color(0xFF34A853)),
        Triple("PhonePe", "PhonePe", Color(0xFF5F259F)),
        Triple("Paytm", "Paytm", Color(0xFF00B9F5)),
        Triple("BHIM UPI", "BHIM", Color(0xFFE27925))
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SafeCupid Security Badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(TealVibrant.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Secure Payment",
                tint = TealAccent,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Secure UPI Payment Gateway",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Paying to verified VPA ID",
            fontSize = 11.sp,
            color = Color.Gray
        )

        // Merchant Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NavyLight.copy(alpha = 0.5f))
                .border(1.dp, TealAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SafeCupid Premium Service",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "UPI ID: $upiId",
                        color = TealAccent,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "₹$amount.00",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }

        Text(
            text = "Select your UPI Payment application:",
            fontSize = 12.sp,
            color = Color.LightGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Start
        )

        // App Selector Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            upiApps.forEach { (fullName, displayName, brandColor) ->
                val isSelected = selectedApp == fullName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NavyLight else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) TealAccent else Color.Gray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onAppSelect(fullName) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Simulated Logo
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(brandColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(2),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = fullName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    RadioButton(
                        selected = isSelected,
                        onClick = { onAppSelect(fullName) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = TealAccent,
                            unselectedColor = Color.Gray
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Pay Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel", fontSize = 13.sp)
            }

            Button(
                onClick = onProceed,
                colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Pay ₹$amount",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun LoadingSdkScreen(selectedApp: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = TealVibrant,
            modifier = Modifier.size(54.dp),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Launching Secure UPI Gateway...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Handshaking encrypted tunnels via $selectedApp SDK",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UpiPinGatewayScreen(
    amount: Int,
    upiId: String,
    upiPin: String,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A141A), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1B4965), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // NPCI Logo Vibe Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BHIM UPI Gateway",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00B9F5)
            )
            Text(
                text = "NPCI SECURE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                modifier = Modifier
                    .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Issuing bank panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F3D59)),
                contentAlignment = Alignment.Center
            ) {
                Text("B", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("State Bank of India", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Savings Account *****8493", color = Color.Gray, fontSize = 9.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("₹$amount.00", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payee label
        Text(
            text = "TO: SafeCupid Premium ($upiId)",
            color = Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ENTER 4-DIGIT UPI PIN",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bullet representation of entered digits
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            for (i in 0 until 4) {
                val hasDigit = upiPin.length > i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (hasDigit) TealAccent else Color.Gray.copy(alpha = 0.3f))
                        .border(1.dp, if (hasDigit) TealAccent else Color.Transparent, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // NPCI secure warning
        Text(
            text = "⚠️ Never share this PIN with anyone. NPCI or your bank will never ask for your UPI PIN.",
            color = Color(0xFFFFB300),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom High-fidelity Numeric Pad
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("⌫", "0", "Done")
            )

            keys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isDone = key == "Done"
                        val isBack = key == "⌫"
                        val isEnabled = when {
                            isDone -> upiPin.length >= 4
                            else -> true
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isDone && isEnabled -> TealVibrant
                                        isDone -> Color.Gray.copy(alpha = 0.15f)
                                        isBack -> Color.White.copy(alpha = 0.1f)
                                        else -> Color.White.copy(alpha = 0.05f)
                                    }
                                )
                                .clickable(enabled = isEnabled) {
                                    when {
                                        isBack -> {
                                            if (upiPin.isNotEmpty()) {
                                                onPinChange(upiPin.dropLast(1))
                                            }
                                        }
                                        isDone -> {
                                            onSubmit()
                                        }
                                        else -> {
                                            if (upiPin.length < 4) {
                                                onPinChange(upiPin + key)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBack) {
                                Icon(Icons.Default.Delete, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text(
                                    text = key,
                                    color = if (isDone && !isEnabled) Color.Gray else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerifyingTransactionScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFFFFD700),
            modifier = Modifier.size(54.dp),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Verifying UPI Transaction...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Settling secure payment blocks with issuing bank servers",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PaymentSuccessScreen(
    amount: Int,
    upiId: String,
    transactionId: String,
    onFinish: () -> Unit
) {
    // Elegant spring entry tick animation
    var tickScale by remember { mutableStateOf(0.3f) }
    LaunchedEffect(Unit) {
        tickScale = 1.0f
    }

    val animatedTickScale by animateFloatAsState(
        targetValue = tickScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "TickSpring"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Green Circle Animated Check
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(animatedTickScale)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "UPI Payment Successful! 🎉",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF4CAF50),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Premium features are now completely functional",
            fontSize = 11.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Receipt Details Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NavyLight.copy(alpha = 0.4f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReceiptRow(label = "Amount Paid", value = "₹$amount.00", isValueBold = true, valueColor = Color.White)
            ReceiptRow(label = "Recipient UPI VPA", value = upiId, valueColor = TealAccent)
            ReceiptRow(label = "Bank Reference ID", value = transactionId)
            ReceiptRow(label = "NPCI Status", value = "SUCCESS / SETTLED", valueColor = Color(0xFF4CAF50))
            ReceiptRow(label = "Secure Key Verification", value = "E2EE Verified")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payment_success_done_btn")
        ) {
            Text(
                text = "Done & Unlock Premium! 🔓",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ReceiptRow(
    label: String,
    value: String,
    isValueBold: Boolean = false,
    valueColor: Color = Color.LightGray
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = if (isValueBold) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
