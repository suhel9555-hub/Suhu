@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.AppNotification
import com.example.viewmodel.NotificationType
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.Match
import com.example.data.Message
import com.example.data.Profile
import com.example.viewmodel.DatingViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// --- Styling Palette (Teal & Navy Blue from FIND CORRECT Logo) ---
val NavyDark = Color(0xFF0F3D59)
val SkyBlue = Color(0xFFE8F1F5)
val WarmSand = Color(0xFFFAFAFA)

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val accent: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val isLight: Boolean
)

val DarkColors = AppColors(
    background = Color(0xFF081C26),
    surface = Color(0xFF0E2A38),
    surfaceVariant = Color(0xFF1B4965),
    primary = Color(0xFF00A896),
    accent = Color(0xFF02C39A),
    text = Color.White,
    textSecondary = Color.Gray,
    border = Color(0xFF1B4965).copy(alpha = 0.3f),
    isLight = false
)

val LightLowColors = AppColors(
    background = Color(0xFFF2F6F5), // low light pastel mint/slate
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3EDEE), // soft pastel teal-gray
    primary = Color(0xFF00A896),
    accent = Color(0xFF02C39A),
    text = Color(0xFF0F2537),
    textSecondary = Color(0xFF5A6E7F),
    border = Color(0xFFDFE7E6),
    isLight = true
)

val LocalAppColors = staticCompositionLocalOf { DarkColors }

val DarkBackground: Color
    @Composable
    get() = LocalAppColors.current.background

val DarkSurface: Color
    @Composable
    get() = LocalAppColors.current.surface

val NavyLight: Color
    @Composable
    get() = LocalAppColors.current.surfaceVariant

val TealVibrant = Color(0xFF00A896)
val TealAccent = Color(0xFF02C39A)

// List of high-contrast beautiful gradients for avatar backgrounds
val AvatarGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFF00A896), Color(0xFF02C39A))),
    Brush.linearGradient(listOf(Color(0xFF3F51B5), Color(0xFF00BCD4))),
    Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFFFFC107))),
    Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63))),
    Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))),
    Brush.linearGradient(listOf(Color(0xFFE040FB), Color(0xFF00E5FF)))
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DatingApp(viewModel: DatingViewModel) {
    val isLightTheme by viewModel.isLightTheme.collectAsStateWithLifecycle()
    val colors = if (isLightTheme) LightLowColors else DarkColors

    CompositionLocalProvider(LocalAppColors provides colors) {
        val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
        val systemNotification by viewModel.systemNotification.collectAsStateWithLifecycle()
        val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
        val confettiCount by viewModel.confettiCount.collectAsStateWithLifecycle()

        var activeScreen by remember { mutableStateOf("welcome") }
        var showSupportBot by remember { mutableStateOf(false) }
        val activeMatchId by viewModel.activeMatchId.collectAsStateWithLifecycle()
        val activeGroupId by viewModel.activeGroupId.collectAsStateWithLifecycle()

        val context = LocalContext.current
        var showStartupPermissions by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val prefs = context.getSharedPreferences("dating_app_prefs", Context.MODE_PRIVATE)
            val hasAskedStartupPermissions = prefs.getBoolean("has_asked_startup_permissions", false)
            
            if (!hasAskedStartupPermissions) {
                val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                val hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                
                val hasPhotos = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }

                val hasNotifications = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

                if (!hasCamera || !hasLocation || !hasMic || !hasPhotos || !hasNotifications || !hasContacts) {
                    showStartupPermissions = true
                }
                
                // Mark as asked so we never prompt again at startup
                prefs.edit().putBoolean("has_asked_startup_permissions", true).apply()
            }
        }

        // Route controller based on user profile state
        LaunchedEffect(userProfile) {
            if (userProfile == null) {
                activeScreen = "welcome"
            } else {
                viewModel.checkAndApplyStartupPinLock()
                if (activeScreen == "welcome" || activeScreen == "onboarding") {
                    activeScreen = "deck"
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // Core navigation flow
            AnimatedContent(
                targetState = activeScreen,
                transitionSpec = {
                    slideInHorizontally(animationSpec = tween(150)) { width -> width } + fadeIn(animationSpec = tween(150)) togetherWith
                            slideOutHorizontally(animationSpec = tween(150)) { width -> -width } + fadeOut(animationSpec = tween(150))
                }
            ) { screen ->
                when (screen) {
                    "welcome" -> WelcomeScreen(
                        onNavigateToOnboarding = { activeScreen = "onboarding" }
                    )
                    "onboarding" -> OnboardingScreen(
                        viewModel = viewModel,
                        onSignUpSuccess = { activeScreen = "deck" }
                    )
                    "deck", "reels", "recommendations", "chats", "settings" -> MainHub(
                        activeTab = screen,
                        viewModel = viewModel,
                        onTabSelected = { activeScreen = it },
                        onNavigateToChat = { matchId ->
                            viewModel.setActiveMatchId(matchId)
                            activeScreen = "chat"
                        },
                        onNavigateToGroupChat = { groupId ->
                            viewModel.setActiveGroupId(groupId)
                            activeScreen = "group_chat"
                        },
                        onOpenSupport = { showSupportBot = true }
                    )
                    "chat" -> ChatScreen(
                        matchId = activeMatchId ?: 0,
                        viewModel = viewModel,
                        onBack = {
                            viewModel.setActiveMatchId(null)
                            activeScreen = "chats"
                        }
                    )
                    "group_chat" -> GroupChatScreen(
                        groupId = activeGroupId ?: 0,
                        viewModel = viewModel,
                        onBack = {
                            viewModel.setActiveGroupId(null)
                            activeScreen = "chats"
                        }
                    )
                }
            }

        // Float particle effect
        ConfettiOverlay(triggerCount = confettiCount)

        // Lock screen overlay blocks app
        if (isAppLocked) {
            PinLockOverlay(viewModel = viewModel)
        }

        if (showStartupPermissions) {
            StartupPermissionsRequestDialog(
                onDismiss = { showStartupPermissions = false },
                viewModel = viewModel
            )
        }

        // Floating In-App Match Splash Overlay
        val matchSplashProfile by viewModel.matchSplashProfile.collectAsStateWithLifecycle()
        if (matchSplashProfile != null) {
            MatchSplashOverlay(
                matchedProfile = matchSplashProfile!!,
                userProfile = userProfile,
                onKeepSwiping = { viewModel.dismissMatchSplash() },
                onChatNow = {
                    viewModel.dismissMatchSplash()
                    // Locate the match id
                    val activeMatch = viewModel.matches.value.find { 
                        it.matchedUserId == matchSplashProfile!!.id || it.userId == matchSplashProfile!!.id 
                    }
                    if (activeMatch != null) {
                        viewModel.setActiveMatchId(activeMatch.id)
                        activeScreen = "chat"
                    } else {
                        activeScreen = "chats"
                    }
                }
            )
        }

        // Global Toast/System Notification Banner
        if (systemNotification != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TealVibrant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { _, _ -> viewModel.clearNotification() }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (systemNotification!!.contains("Fake") || systemNotification!!.contains("Block")) 
                                Icons.Default.Warning else Icons.Default.Star,
                            contentDescription = "Notification Sparkle",
                            tint = if (systemNotification!!.contains("Fake") || systemNotification!!.contains("Block")) Color.Red else TealAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = systemNotification!!,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Customer Support Floating Assistive Bubble
        val isHubScreen = activeScreen in listOf("deck", "recommendations", "chats", "settings")
        if (isHubScreen && !isAppLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 20.dp)
            ) {
                FloatingSupportBubble(
                    onClick = { showSupportBot = true }
                )
            }
        }

        // Support Chat Modal Panel Overlay
        if (showSupportBot) {
            AISupportChatPanel(
                viewModel = viewModel,
                onClose = { showSupportBot = false }
            )
        }
    }
}
}

// --- Welcome / Onboarding Screen ---

@Composable
fun WelcomeScreen(onNavigateToOnboarding: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Branding Logo (Custom vector representation of FC)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(NavyLight, DarkBackground)))
                    .border(2.dp, TealVibrant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Drawing the FC logo dynamically
                 Canvas(modifier = Modifier.size(100.dp)) {
                    val w = size.width
                    val h = size.height

                    // Draw a subtle outer radar ring
                    drawCircle(
                        color = TealAccent.copy(alpha = 0.25f),
                        radius = w * 0.48f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )

                    // Draw an inner glowing ring with premium gradient
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(TealVibrant, TealAccent, TealVibrant)),
                        radius = w * 0.42f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )

                    // Draw elegant interlocking geometric arcs
                    // F-Arc: Left curved vertical arc + top bar
                    val pathF = Path().apply {
                        moveTo(w * 0.35f, h * 0.75f)
                        lineTo(w * 0.35f, h * 0.30f)
                        quadraticTo(w * 0.35f, h * 0.22f, w * 0.45f, h * 0.22f)
                        lineTo(w * 0.65f, h * 0.22f)
                    }
                    drawPath(
                        path = pathF,
                        color = TealAccent,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw middle F bar
                    drawLine(
                        color = TealAccent,
                        start = Offset(w * 0.35f, h * 0.45f),
                        end = Offset(w * 0.55f, h * 0.45f),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // C-Arc: Right curved arc
                    val pathC = Path().apply {
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(w * 0.48f, h * 0.32f, w * 0.82f, h * 0.68f),
                            startAngleDegrees = -120f,
                            sweepAngleDegrees = 240f,
                            forceMoveTo = false
                        )
                    }
                    drawPath(
                        path = pathC,
                        color = Color.White,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                
                // Let's place a beautiful typography logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FC",
                        color = Color.White,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Icon",
                            tint = TealAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VERIFIED",
                            color = TealAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FIND CORRECT",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Right Choice, Right Solution",
                color = TealAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        // Onboarding Interactive Signup Card
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Join now to find your real compatible matches certified by Gemini AI security.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = onNavigateToOnboarding,
                colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("signup_google_button"),
                shape = RoundedCornerShape(25.dp)
            ) {
                Icon(Icons.Default.AccountBox, contentDescription = "Google Icon", tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Google", color = Color.White, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateToOnboarding,
                border = BorderStroke(1.dp, TealVibrant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("signup_phone_button"),
                shape = RoundedCornerShape(25.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Phone Icon", tint = TealVibrant)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Phone Number", color = Color.White)
            }

            OutlinedButton(
                onClick = onNavigateToOnboarding,
                border = BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("signup_email_button"),
                shape = RoundedCornerShape(25.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = Color.LightGray)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Email", color = Color.LightGray)
            }
        }
    }
}

// --- Onboarding / Create Profile Screen ---

@Composable
fun OnboardingScreen(viewModel: DatingViewModel, onSignUpSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("San Francisco, CA") }
    var gender by remember { mutableStateOf("Female") }
    var interestedIn by remember { mutableStateOf("Everyone") }
    var qualities by remember { mutableStateOf("") }
    var selectedGradientIndex by remember { mutableStateOf(0) }
    
    // Choose dynamic funny emoji avatar representation
    val emojis = listOf("👩‍💻", "🎨", "🏋️‍♀️", "🌿", "🎮", "🌟")
    var selectedEmoji by remember { mutableStateOf("👩‍💻") }

    val aiBio by viewModel.aiBioGenerationState.collectAsStateWithLifecycle()
    val aiBioLoading by viewModel.aiBioLoading.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    // 3 compulsory profile image states
    var image1 by remember { mutableStateOf("") }
    var image2 by remember { mutableStateOf("") }
    var image3 by remember { mutableStateOf("") }
    var activeSlotIndex by remember { mutableStateOf<Int?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && activeSlotIndex != null) {
            when (activeSlotIndex) {
                1 -> image1 = uri.toString()
                2 -> image2 = uri.toString()
                3 -> image3 = uri.toString()
            }
        }
        showImageSourceDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Up Your Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = Color.White)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Welcome to Find Correct! Let's build your AI matching card.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }

            // Avatar picker
            item {
                Text("Choose Your Avatar Style", color = TealAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    emojis.forEachIndexed { index, em ->
                        val isSelected = selectedEmoji == em
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AvatarGradients[index])
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedEmoji = em
                                    selectedGradientIndex = index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = em, fontSize = 24.sp)
                        }
                    }
                }
            }

            // 3 Compulsory Profile Images Upload Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📸 Upload 3 Photos", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("COMPULSORY", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dating safety requires at least 3 active lifestyle photos to proceed to matching. Click any slot to upload.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(1 to image1, 2 to image2, 3 to image3).forEach { (index, imgUrl) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkBackground)
                                        .border(
                                            border = BorderStroke(
                                                width = 1.5.dp,
                                                color = if (imgUrl.isNotBlank()) TealAccent else Color.DarkGray
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            activeSlotIndex = index
                                            showImageSourceDialog = true
                                        }
                                        .testTag("upload_image_slot_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (imgUrl.isBlank()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.AddCircle,
                                                contentDescription = "Upload Photo",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Photo $index", color = Color.Gray, fontSize = 10.sp)
                                        }
                                    } else {
                                        AsyncImage(
                                            model = imgUrl,
                                            contentDescription = "Profile Photo $index",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(18.dp)
                                                .background(TealVibrant, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Uploaded",
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Inputs
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TealVibrant,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it },
                        label = { Text("Age", color = Color.LightGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TealVibrant,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1f).testTag("profile_age_input")
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TealVibrant,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.weight(2f).testTag("profile_location_input")
                    )
                }
            }

            // Genders
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("My Gender", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Female", "Male", "Non-binary").forEach { g ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { gender = g }
                            ) {
                                RadioButton(
                                    selected = gender == g,
                                    onClick = { gender = g },
                                    colors = RadioButtonDefaults.colors(selectedColor = TealVibrant)
                                )
                                Text(g, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Interested In", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Male", "Female", "Everyone").forEach { int ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { interestedIn = int }
                            ) {
                                RadioButton(
                                    selected = interestedIn == int,
                                    onClick = { interestedIn = int },
                                    colors = RadioButtonDefaults.colors(selectedColor = TealVibrant)
                                )
                                Text(int, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Gemini bio creator section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🤖 Gemini AI Bio Generator",
                            color = TealAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tell Gemini 3 qualities or hobbies about yourself, and we'll craft the perfect bio!",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = qualities,
                            onValueChange = { qualities = it },
                            placeholder = { Text("e.g. coffee fan, dog lover, backend developer", color = Color.Gray, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = TealVibrant,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("ai_traits_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.generateAIBio(qualities) },
                            enabled = qualities.isNotBlank() && !aiBioLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyLight),
                            modifier = Modifier.align(Alignment.End).testTag("generate_bio_button")
                        ) {
                            if (aiBioLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Generate Bio with AI", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        if (aiBio != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, TealVibrant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = aiBio!!,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }
                }
            }

            // Finish Signup
            item {
                if (image1.isBlank() || image2.isBlank() || image3.isBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Required",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Validation: Upload exactly 3 photos above to activate registration.",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val age = ageStr.toIntOrNull() ?: 24
                        viewModel.signUpAndCreateProfile(
                            name = if (name.isBlank()) "Explorer" else name,
                            age = age,
                            gender = gender,
                            interestedIn = interestedIn,
                            location = location,
                            interests = if (qualities.isBlank()) "Tech, Coffee" else qualities,
                            avatarGradientIndex = selectedGradientIndex,
                            avatarEmoji = selectedEmoji,
                            image1 = image1,
                            image2 = image2,
                            image3 = image3
                        )
                        onSignUpSuccess()
                    },
                    enabled = name.isNotBlank() && ageStr.isNotBlank() && image1.isNotBlank() && image2.isNotBlank() && image3.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealVibrant,
                        disabledContainerColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_complete_button"),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = if (image1.isNotBlank() && image2.isNotBlank() && image3.isNotBlank()) "Enter Find Correct ⚡" else "Upload 3 Photos to Enter 🔒",
                        color = if (image1.isNotBlank() && image2.isNotBlank() && image3.isNotBlank()) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // --- Dialog popup for Image Source ---
        if (showImageSourceDialog && activeSlotIndex != null) {
            val presetImages = listOf(
                "Cafe Chat" to "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&q=80&w=400",
                "Outdoorsy" to "https://images.unsplash.com/photo-1527631746610-bca00a040d60?auto=format&fit=crop&q=80&w=400",
                "Urban Life" to "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&q=80&w=400",
                "Tech Style" to "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&q=80&w=400",
                "Dog Lover" to "https://images.unsplash.com/photo-1477884213980-b770172b940a?auto=format&fit=crop&q=80&w=400",
                "Concert Fan" to "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=400"
            )

            AlertDialog(
                onDismissRequest = { showImageSourceDialog = false },
                title = {
                    Text(
                        text = "Add Photo for Slot $activeSlotIndex",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Choose a source to upload your photo. Select a high-quality preset or browse your device gallery.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("select_device_gallery_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Device Gallery", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "Or choose a styled preset photo:",
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presetImages) { preset ->
                                val label = preset.first
                                val url = preset.second
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clickable {
                                            when (activeSlotIndex) {
                                                1 -> image1 = url
                                                2 -> image2 = url
                                                3 -> image3 = url
                                            }
                                            showImageSourceDialog = false
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showImageSourceDialog = false }) {
                        Text("Cancel", color = Color.Gray, fontSize = 12.sp)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// --- Main Hub (Coordinates Tabs and Swipe Deck) ---

@Composable
fun MainHub(
    activeTab: String,
    viewModel: DatingViewModel,
    onTabSelected: (String) -> Unit,
    onNavigateToChat: (Int) -> Unit,
    onNavigateToGroupChat: (Int) -> Unit = {},
    onOpenSupport: () -> Unit = {}
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "deck",
                    onClick = { onTabSelected("deck") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dating Deck") },
                    label = { Text("Swipe") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealAccent,
                        selectedTextColor = TealAccent,
                        indicatorColor = NavyDark,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "reels",
                    onClick = { onTabSelected("reels") },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Video Reels") },
                    label = { Text("Reels") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealAccent,
                        selectedTextColor = TealAccent,
                        indicatorColor = NavyDark,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "recommendations",
                    onClick = { onTabSelected("recommendations") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "AI Recommendations") },
                    label = { Text("AI Match") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealAccent,
                        selectedTextColor = TealAccent,
                        indicatorColor = NavyDark,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "chats",
                    onClick = { onTabSelected("chats") },
                    icon = { Icon(Icons.Default.Email, contentDescription = "Chats List") },
                    label = { Text("Chats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealAccent,
                        selectedTextColor = TealAccent,
                        indicatorColor = NavyDark,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "settings",
                    onClick = { onTabSelected("settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Admin and Settings") },
                    label = { Text("System") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealAccent,
                        selectedTextColor = TealAccent,
                        indicatorColor = NavyDark,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        val notificationsList by viewModel.appNotifications.collectAsStateWithLifecycle()
        val unreadCount = notificationsList.count { !it.isRead }
        var showNotificationCenter by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "deck" -> SwipeDeckScreen(viewModel)
                "reels" -> ReelsScreen(viewModel, onNavigateToChat)
                "recommendations" -> AIRecommendationsScreen(viewModel, onNavigateToChat)
                "chats" -> ChatsListScreen(viewModel, onNavigateToChat, onNavigateToGroupChat)
                "settings" -> SystemAdminScreen(viewModel, onOpenSupport, onNavigateToChat)
            }

            // Floating Notification Bell Button with badge (Accessible on all tabs!)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .testTag("notification_bell_container")
            ) {
                IconButton(
                    onClick = { showNotificationCenter = true },
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(NavyLight.copy(alpha = 0.9f), DarkSurface.copy(alpha = 0.9f))
                            ),
                            shape = CircleShape
                        )
                        .border(1.dp, TealAccent.copy(alpha = 0.3f), CircleShape)
                        .testTag("notification_bell_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications bell",
                        tint = if (unreadCount > 0) TealAccent else Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .background(Color.Red, CircleShape)
                            .border(1.5.dp, DarkBackground, CircleShape)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showNotificationCenter) {
                NotificationCenterDialog(
                    viewModel = viewModel,
                    notifications = notificationsList,
                    onDismiss = { showNotificationCenter = false }
                )
            }
        }
    }
}

@Composable
fun NotificationCenterDialog(
    viewModel: DatingViewModel,
    notifications: List<AppNotification>,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    
    val filteredList = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Safety" -> notifications.filter { it.type == NotificationType.SAFETY }
            "Matches" -> notifications.filter { it.type == NotificationType.MATCH }
            "System" -> notifications.filter { it.type == NotificationType.SYSTEM || it.type == NotificationType.GPS }
            else -> notifications
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications logo",
                            tint = TealAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Notification Center",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(NavyLight, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close notifications panel",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.markAllNotificationsAsRead() },
                        colors = ButtonDefaults.textButtonColors(contentColor = TealAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Mark all as read",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark all read", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = { viewModel.clearAllNotifications() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear all notifications",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Safety", "Matches", "System")
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) TealAccent else NavyLight,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notifications List or Empty State
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Bell icon",
                                tint = Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "All caught up!",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No $selectedFilter notifications to show.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList) { item ->
                            val itemIcon = when (item.type) {
                                NotificationType.MATCH -> Icons.Default.Favorite
                                NotificationType.SAFETY -> Icons.Default.Lock
                                NotificationType.CHAT -> Icons.Default.Email
                                NotificationType.GPS -> Icons.Default.Place
                                NotificationType.SYSTEM -> Icons.Default.Info
                            }
                            val iconColor = when (item.type) {
                                NotificationType.MATCH -> Color(0xFFFF00CC)
                                NotificationType.SAFETY -> TealAccent
                                NotificationType.CHAT -> Color.Cyan
                                NotificationType.GPS -> Color.Yellow
                                NotificationType.SYSTEM -> Color.LightGray
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.isRead) DarkSurface else NavyLight
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (item.isRead) Color.Transparent else TealAccent.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { viewModel.markNotificationAsRead(item.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Notification Type Icon with background
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(iconColor.copy(alpha = 0.15f), CircleShape)
                                            .border(1.dp, iconColor.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = itemIcon,
                                            contentDescription = "Notification type icon",
                                            tint = iconColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            
                                            if (!item.isRead) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(TealAccent, CircleShape)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = item.message,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = formatTimeAgo(item.timestamp),
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
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

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}


@Composable
fun PremiumFutureHubScreen(viewModel: DatingViewModel, onNavigateToChat: (Int) -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val virtualPartnerName by viewModel.virtualPartnerName.collectAsStateWithLifecycle()
    val virtualScenario by viewModel.virtualScenario.collectAsStateWithLifecycle()
    val virtualDateMessages by viewModel.virtualDateMessages.collectAsStateWithLifecycle()
    val isVirtualDateLoading by viewModel.isVirtualDateLoading.collectAsStateWithLifecycle()

    var activePaymentPlan by remember { mutableStateOf<String?>(null) }
    val currentTier = userProfile?.premiumTier ?: "None"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Neon Premium Hub Title Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFF4081),
                                Color(0xFF00E5FF)
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(15.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "👑 CORRECT PREMIUM HUB",
                            color = Color(0xFFFFD700),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Unlock next-generation dating security & future simulation options.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Tier status panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Membership Tier", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (currentTier) {
                                "Premium" -> "👑 Premium (₹1/day)"
                                "Premium Plus", "Premium Pro" -> "💎 Premium Plus (₹9/day)"
                                else -> "Standard Account (Free)"
                            },
                            color = when (currentTier) {
                                "Premium" -> Color(0xFFFFD700)
                                "Premium Plus", "Premium Pro" -> Color(0xFF00E5FF)
                                else -> Color.White
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (currentTier != "None") TealVibrant.copy(alpha = 0.2f) else NavyLight,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentTier != "None") "ACTIVE" else "UPGRADE",
                            color = if (currentTier != "None") TealAccent else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section 1: Billing & Upgrade Options
        item {
            Text(
                text = "Choose Your Upgrade Plan",
                color = TealAccent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Premium Club Plan Card (₹1/day)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFFB300), Color(0xFFFF6F00))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { activePaymentPlan = "Premium" }
                    .padding(1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(15.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("👑 Premium Member", color = Color(0xFFFFD700), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("Standard security & matching package", color = Color.Gray, fontSize = 11.sp)
                        }
                        Text("₹1/day", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("✓ Priority Matching Boost: 5x profile visibility", color = Color.White, fontSize = 12.sp)
                    Text("✓ Dynamic Vibe customizers (Moods & Goals)", color = Color.White, fontSize = 12.sp)
                    Text("✓ Unlimited safe profile audits / integrity checks", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { activePaymentPlan = "Premium" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (currentTier == "Premium") "Current Plan (Tap to Reactivate)" else "Sign Up for Premium (₹1/day)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Premium Plus Plan Card (₹9/day)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFFE040FB), Color(0xFFFF4081))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { activePaymentPlan = "Premium Plus" }
                    .padding(1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(15.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💎 Premium Plus", color = Color(0xFF00E5FF), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFF4081), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("BEST VALUE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Full safety suite and all AI simulator powers", color = Color.Gray, fontSize = 11.sp)
                        }
                        Text("₹9/day", color = Color(0xFF00E5FF), fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("⚡ Unlocks ALL standard Premium privileges plus:", color = Color(0xFFE040FB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("✓ Device PIN startup protection lock", color = Color.White, fontSize = 12.sp)
                    Text("✓ Stealth Shield Incognito Mode visibility toggle", color = Color.White, fontSize = 12.sp)
                    Text("✓ Travel Teleport GPS companion to match globally", color = Color.White, fontSize = 12.sp)
                    Text("✓ Real-Time AI Fraud Prevention filter shield", color = Color.White, fontSize = 12.sp)
                    Text("✓ Unlimited access to AI Virtual Date Simulator", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { activePaymentPlan = "Premium Plus" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (currentTier == "Premium Plus" || currentTier == "Premium Pro") "Active Premium Plus (Tap to Re-verify)" else "Sign Up for Premium Plus (₹9/day)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Section 2: Future Feature GPS Teleportation (Travel Companion)
        item {
            Text(
                text = "✈️ GPS Teleport (Travel Companion)",
                color = TealAccent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Change your location instantly to match with singles anywhere on earth! Currently matching in:",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(NavyLight, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = "Pin", tint = TealAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(currentLocation, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (currentTier == "None") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🔒 Unlocks with Premium membership. Subscribe to teleport now!",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select a destination:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val destinations = listOf(
                            "Tokyo, Japan 🇯🇵",
                            "London, UK 🇬🇧",
                            "Paris, France 🇫🇷",
                            "Sydney, Australia 🇦🇺",
                            "New York, NY 🇺🇸"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            destinations.take(3).forEach { city ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (currentLocation == city) TealVibrant else NavyLight, RoundedCornerShape(10.dp))
                                        .border(1.dp, if (currentLocation == city) TealAccent else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.changeDatingLocation(city) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(city.substringBefore(" "), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            destinations.drop(3).forEach { city ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (currentLocation == city) TealVibrant else NavyLight, RoundedCornerShape(10.dp))
                                        .border(1.dp, if (currentLocation == city) TealAccent else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.changeDatingLocation(city) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(city.substringBefore(" "), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2.5: Real-time GPS Local Radar (Find Nearby)
        item {
            Text(
                text = "📍 Real-Time GPS Local Radar",
                color = TealAccent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            val gpsEnabled by viewModel.gpsEnabled.collectAsStateWithLifecycle()
            val nearbyRadiusKm by viewModel.nearbyRadiusKm.collectAsStateWithLifecycle()
            val isScanningNearby by viewModel.isScanningNearby.collectAsStateWithLifecycle()
            val otherProfilesList by viewModel.otherProfiles.collectAsStateWithLifecycle()

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Local Airspace Scan", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Search using high-precision GPS telemetry", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = gpsEnabled,
                            onCheckedChange = { viewModel.toggleGps(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TealAccent,
                                checkedTrackColor = TealVibrant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!gpsEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "GPS Inactive", tint = Color.Red, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GPS Satellites Disconnected. Enable GPS to scan nearby airspace.", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    } else {
                        // Radius Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Scan Radius", color = Color.LightGray, fontSize = 11.sp)
                            Text("${nearbyRadiusKm.toInt()} km", color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = nearbyRadiusKm,
                            onValueChange = { viewModel.setNearbyRadius(it) },
                            valueRange = 5f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = TealAccent,
                                activeTrackColor = TealVibrant,
                                inactiveTrackColor = NavyLight
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Radar Sonar Scan Animation
                        if (isScanningNearby) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
                                val angle by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "RadarAngle"
                                )
                                val pulseRadius by infiniteTransition.animateFloat(
                                    initialValue = 10f,
                                    targetValue = 200f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2500, easing = EaseOutExpo),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "RadarPulse"
                                )
                                val pulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.8f,
                                    targetValue = 0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2500, easing = EaseOutExpo),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "RadarAlpha"
                                )

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    val maxRadius = Math.min(size.width, size.height) / 2

                                    // Draw concentric radar rings
                                    drawCircle(
                                        color = TealAccent.copy(alpha = 0.15f),
                                        radius = maxRadius * 0.33f,
                                        center = center,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                    )
                                    drawCircle(
                                        color = TealAccent.copy(alpha = 0.15f),
                                        radius = maxRadius * 0.66f,
                                        center = center,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                    )
                                    drawCircle(
                                        color = TealAccent.copy(alpha = 0.3f),
                                        radius = maxRadius,
                                        center = center,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                    )

                                    // Draw pulsing wave
                                    drawCircle(
                                        color = TealAccent.copy(alpha = pulseAlpha),
                                        radius = Math.min(pulseRadius, maxRadius),
                                        center = center
                                    )

                                    // Draw radar sweep line
                                    val rad = Math.toRadians(angle.toDouble())
                                    val sweepX = center.x + maxRadius * Math.cos(rad).toFloat()
                                    val sweepY = center.y + maxRadius * Math.sin(rad).toFloat()
                                    drawLine(
                                        color = TealAccent,
                                        start = center,
                                        end = Offset(sweepX, sweepY),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = TealAccent, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("📡 PINGING LOCAL GPS AIRSPACE...", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                                    Text("E2EE Telemetry Protocol active", color = Color.Gray, fontSize = 8.sp)
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.triggerNearbyScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan Local Area Nearby", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Filtered nearby profiles list
                            val nearbyProfiles = otherProfilesList.filter { profile ->
                                val distance = viewModel.getDistanceToProfile(profile)
                                distance <= nearbyRadiusKm
                            }.sortedBy { viewModel.getDistanceToProfile(it) }

                            if (nearbyProfiles.isEmpty()) {
                                Text(
                                    text = "No one found nearby within ${nearbyRadiusKm.toInt()} km. Expand scan radius or teleport to another city!",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                )
                            } else {
                                Text(
                                    text = "📡 Nearby Profiles Detected (${nearbyProfiles.size}):",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    nearbyProfiles.forEach { profile ->
                                        val dist = viewModel.getDistanceToProfile(profile)
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = NavyLight),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(AvatarGradients[profile.avatarGradientIndex]),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(profile.avatarEmoji, fontSize = 18.sp)
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(profile.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text(", ${profile.age}", color = Color.LightGray, fontSize = 12.sp)
                                                            if (profile.isVerified) {
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Icon(Icons.Default.CheckCircle, "Verified", tint = TealAccent, modifier = Modifier.size(12.dp))
                                                            }
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Place, contentDescription = "Distance", tint = TealAccent, modifier = Modifier.size(10.dp))
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(String.format("%.1f km away", dist), color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(TealVibrant.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                                            ) {
                                                                Text("🛡️ ${profile.trustScore}% trust", color = TealAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        viewModel.startDirectChat(profile.id) { matchId ->
                                                            onNavigateToChat(matchId)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("💬 Chat Direct", color = NavyDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
        }

        // Section 3: AI Virtual Date Simulator
        item {
            Text(
                text = "🔮 AI Virtual Date Simulator",
                color = TealAccent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Practice dating by chatting with an AI virtual date partner in high-fidelity custom settings. Build your social safety confidence!",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    if (currentTier != "Premium" && currentTier != "Premium Plus" && currentTier != "Premium Pro") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🔒 Premium Exclusive. Upgrade to Premium or Premium Plus to chat with virtual dates!",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Configure Simulation Session:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Partner picker
                        Text("Partner Persona:", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Celeste 🤖", "Arthur 🤖", "Yuki 🤖", "Ryan 🤖").forEach { name ->
                                val isSelected = virtualPartnerName == name
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) TealVibrant else NavyLight, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.selectVirtualPartner(name, virtualScenario) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name.substringBefore(" "), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Scenario picker
                        Text("Date Setting / Location Theme:", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val scenarios = listOf(
                            "Charming Cafe in Paris ☕",
                            "Cyberpunk Rooftop in Tokyo 👾",
                            "Sunset Beach Malibu 🌅"
                        )
                        scenarios.forEach { setting ->
                            val isSelected = virtualScenario == setting
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .background(if (isSelected) TealVibrant else NavyLight, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectVirtualPartner(virtualPartnerName, setting) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(setting, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Chat Screen Simulator Container
                        Text(
                            text = "Live Chat Simulation (${virtualPartnerName} in ${virtualScenario.substringBefore(" ")}):",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(1.dp, TealAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            if (virtualDateMessages.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { viewModel.resetVirtualDate() },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealVibrant)
                                    ) {
                                        Text("Start Simulated Session", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Message List Scroll
                                    Box(modifier = Modifier.weight(1f)) {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            reverseLayout = true,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(virtualDateMessages.reversed()) { msg ->
                                                val isUser = msg.senderId == 1
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .widthIn(max = 180.dp)
                                                            .background(
                                                                if (isUser) TealVibrant else NavyLight,
                                                                RoundedCornerShape(
                                                                    topStart = 12.dp,
                                                                    topEnd = 12.dp,
                                                                    bottomStart = if (isUser) 12.dp else 0.dp,
                                                                    bottomEnd = if (isUser) 0.dp else 12.dp
                                                                )
                                                            )
                                                            .padding(8.dp)
                                                    ) {
                                                        Text(msg.content, color = Color.White, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (isVirtualDateLoading) {
                                        Text(
                                            text = "$virtualPartnerName is typing standard safety advice...",
                                            color = TealAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }

                                    // Input row
                                    var inputText by remember { mutableStateOf("") }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextField(
                                            value = inputText,
                                            onValueChange = { inputText = it },
                                            placeholder = { Text("Type reply...", fontSize = 11.sp, color = Color.Gray) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = DarkSurface,
                                                unfocusedContainerColor = DarkSurface,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            shape = RoundedCornerShape(21.dp),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        IconButton(
                                            onClick = {
                                                if (inputText.isNotBlank()) {
                                                    viewModel.sendVirtualDateMessage(inputText)
                                                    inputText = ""
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(TealVibrant, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Send Message",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.resetVirtualDate() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Reset Date Simulation", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (activePaymentPlan != null) {
        UPIPaymentDialog(
            planName = activePaymentPlan!!,
            viewModel = viewModel,
            onDismiss = { activePaymentPlan = null }
        )
    }
}

// --- Swipe Deck Tab ---

@Composable
fun SwipeDeckScreen(viewModel: DatingViewModel) {
    val profiles by viewModel.otherProfiles.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentSwipeIndex.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val searchRadius by viewModel.searchRadiusKm.collectAsStateWithLifecycle()
    val minAge by viewModel.minAgePreference.collectAsStateWithLifecycle()
    val maxAge by viewModel.maxAgePreference.collectAsStateWithLifecycle()

    // 1. Interactive Vibe Filters local states
    var selectedMoodFilter by remember { mutableStateOf("All") }
    var selectedGoalFilter by remember { mutableStateOf("All") }
    var activeSubMode by remember { mutableStateOf("deck") } // "deck" or "feed"

    val filteredProfiles = remember(profiles, selectedMoodFilter, selectedGoalFilter, searchRadius, minAge, maxAge, userProfile) {
        profiles.filter { profile ->
            val matchMood = selectedMoodFilter == "All" || profile.moodBadge == selectedMoodFilter
            val matchGoal = selectedGoalFilter == "All" || profile.datingGoal == selectedGoalFilter
            
            // Preference checks
            val distance = viewModel.getDistanceToProfile(profile)
            val matchDistance = distance <= searchRadius
            val matchAge = profile.age in minAge..maxAge
            
            // Gender matching
            val userGender = userProfile?.gender ?: "Male"
            val matchGender = when (userProfile?.interestedIn) {
                "Male" -> profile.gender == "Male"
                "Female" -> profile.gender == "Female"
                else -> true // Everyone
            }
            val matchMutualGender = when (profile.interestedIn) {
                "Male" -> userGender == "Male"
                "Female" -> userGender == "Female"
                else -> true
            }
            
            matchMood && matchGoal && matchDistance && matchAge && matchGender && matchMutualGender
        }
    }

    var filteredIndex by remember(filteredProfiles.size) { mutableStateOf(0) }
    val safeFilteredIndex = if (filteredProfiles.isNotEmpty()) {
        if (filteredIndex < filteredProfiles.size) filteredIndex else 0
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FIND CORRECT",
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 20.sp,
                letterSpacing = 1.sp
            )
            
            // Online status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(NavyLight, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TealAccent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gemini Safe Mode", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // High-Fidelity Segmented Tab Toggle between Classic Swipe and Instagram-style Social Feed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyDark, RoundedCornerShape(14.dp))
                .border(1.dp, NavyLight, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeSubMode == "deck") TealVibrant else Color.Transparent)
                    .clickable { activeSubMode = "deck" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Swipe Deck",
                        color = if (activeSubMode == "deck") Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeSubMode == "feed") TealVibrant else Color.Transparent)
                    .clickable { activeSubMode = "feed" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📱", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Social Feed",
                        color = if (activeSubMode == "feed") Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeSubMode == "feed") {
            Box(modifier = Modifier.weight(1f)) {
                SocialFeedScreen(viewModel = viewModel)
            }
        } else {
            // Vibe Filters Row (Decisions: Mood Alignment)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val moods = listOf("All", "☕ Cafe Chat", "🍷 Wine Date", "🍿 Movie Night", "🏔️ Outdoorsy", "🎮 Gaming Duo")
            items(moods) { mood ->
                val isSelected = selectedMoodFilter == mood
                val bgModifier = if (isSelected) {
                    Modifier.background(Brush.linearGradient(listOf(Color(0xFFE040FB), Color(0xFF00E5FF))))
                } else {
                    Modifier.background(NavyLight.copy(alpha = 0.6f))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(bgModifier)
                        .clickable { selectedMoodFilter = mood; filteredIndex = 0 }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (mood == "All") "✨ All Vibes" else mood,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Dating Goal Filters Row (Decisions: Goal Alignment)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val goals = listOf("All", "💍 Serious Match", "🥂 Casual Fun", "✨ Just Friends", "🧩 Chat & See")
            items(goals) { goal ->
                val isSelected = selectedGoalFilter == goal
                val bgModifier = if (isSelected) {
                    Modifier.background(Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFFFFC107))))
                } else {
                    Modifier.background(NavyLight.copy(alpha = 0.6f))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(bgModifier)
                        .clickable { selectedGoalFilter = goal; filteredIndex = 0 }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (goal == "All") "🎯 All Goals" else goal,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (profiles.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealVibrant)
            }
        } else if (filteredProfiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(DarkSurface, RoundedCornerShape(24.dp))
                    .border(1.dp, NavyLight, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text("🔮", fontSize = 44.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No vibe matches found",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try clearing your filters to see more profiles!",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            selectedMoodFilter = "All"
                            selectedGoalFilter = "All"
                            filteredIndex = 0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealVibrant)
                    ) {
                        Text("Reset Vibe Filters", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        } else {
            val profile = filteredProfiles[safeFilteredIndex]

            // Prefetch compatibility calculation early for fluid rendering
            LaunchedEffect(profile) {
                viewModel.loadCompatibilityRecommendation(profile)
            }

            // Matching Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ProfileCard(
                    profile = profile,
                    viewModel = viewModel,
                    onLeftSwipe = {
                        filteredIndex = (filteredIndex + 1) % filteredProfiles.size
                    },
                    onRightSwipe = {
                        viewModel.swipeRight(profile)
                        filteredIndex = (filteredIndex + 1) % filteredProfiles.size
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5-Button Decision Deck (Rewind, Pass, Boost Match, SuperLike, Like)
        if (profiles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Rewind/Undo (Yellow)
                IconButton(
                    onClick = {
                        if (filteredProfiles.isNotEmpty()) {
                            filteredIndex = if (filteredIndex - 1 < 0) filteredProfiles.size - 1 else filteredIndex - 1
                        } else {
                            viewModel.undoSwipe()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(2.dp, CircleShape)
                        .background(DarkSurface, CircleShape)
                        .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.6f), CircleShape)
                        .testTag("rewind_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rewind Swipe",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. Pass button (Red)
                IconButton(
                    onClick = {
                        if (filteredProfiles.isNotEmpty()) {
                            filteredIndex = (filteredIndex + 1) % filteredProfiles.size
                        } else {
                            viewModel.swipeLeft()
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(4.dp, CircleShape)
                        .background(DarkSurface, CircleShape)
                        .border(1.5.dp, Color.Red.copy(alpha = 0.6f), CircleShape)
                        .testTag("pass_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Pass Profile",
                        tint = Color.Red,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // 3. Instant Boost Match (Purple Gradient ⚡)
                IconButton(
                    onClick = {
                        val currentProfile = if (filteredProfiles.isNotEmpty()) {
                            filteredProfiles[safeFilteredIndex]
                        } else {
                            val safeIndex = if (currentIndex < profiles.size) currentIndex else 0
                            profiles[safeIndex]
                        }
                        viewModel.triggerSuperLikeConfetti()
                        viewModel.instantMatch(currentProfile)
                        if (filteredProfiles.isNotEmpty()) {
                            filteredIndex = (filteredIndex + 1) % filteredProfiles.size
                        }
                    },
                    modifier = Modifier
                        .size(58.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE040FB), Color(0xFF651FFF))
                            ),
                            CircleShape
                        )
                        .testTag("boost_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⚡",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 4. SuperLike / Compatibility Info (Teal/Blue)
                IconButton(
                    onClick = {
                        val currentProfile = if (filteredProfiles.isNotEmpty()) {
                            filteredProfiles[safeFilteredIndex]
                        } else {
                            val safeIndex = if (currentIndex < profiles.size) currentIndex else 0
                            profiles[safeIndex]
                        }
                        viewModel.triggerSuperLikeConfetti()
                        viewModel.swipeRight(currentProfile)
                        if (filteredProfiles.isNotEmpty()) {
                            filteredIndex = (filteredIndex + 1) % filteredProfiles.size
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(2.dp, CircleShape)
                        .background(DarkSurface, CircleShape)
                        .border(1.dp, TealAccent.copy(alpha = 0.6f), CircleShape)
                        .testTag("superlike_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Super Like",
                        tint = TealAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 5. Like button (Vibrant Green)
                IconButton(
                    onClick = {
                        val currentProfile = if (filteredProfiles.isNotEmpty()) {
                            filteredProfiles[safeFilteredIndex]
                        } else {
                            val safeIndex = if (currentIndex < profiles.size) currentIndex else 0
                            profiles[safeIndex]
                        }
                        viewModel.swipeRight(currentProfile)
                        if (filteredProfiles.isNotEmpty()) {
                            filteredIndex = (filteredIndex + 1) % filteredProfiles.size
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(4.dp, CircleShape)
                        .background(TealVibrant, CircleShape)
                        .testTag("like_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like Profile",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        }
    }
}

@Composable
fun ProfileCard(
    profile: Profile,
    viewModel: DatingViewModel,
    onLeftSwipe: () -> Unit,
    onRightSwipe: () -> Unit
) {
    val compatibilityMap by viewModel.compatibilityCache.collectAsStateWithLifecycle()
    val compatibilityInfo = compatibilityMap[profile.id]

    var dragOffsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = dragOffsetX)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .pointerInput(profile) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragOffsetX > 150f) {
                            onRightSwipe()
                        } else if (dragOffsetX < -150f) {
                            onLeftSwipe()
                        }
                        dragOffsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetX += dragAmount.x
                    }
                )
            }
            .border(
                width = 1.5.dp,
                color = if (profile.isVerified) TealVibrant.copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Image/Gradient Block
            val photos = listOf(profile.image1, profile.image2, profile.image3).filter { it.isNotBlank() }
            var activePhotoIndex by remember(profile.id) { mutableIntStateOf(0) }
            val currentPhotoUrl = remember(photos, activePhotoIndex) {
                if (photos.isNotEmpty() && activePhotoIndex < photos.size) photos[activePhotoIndex] else ""
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AvatarGradients[profile.avatarGradientIndex]),
                contentAlignment = Alignment.Center
            ) {
                if (currentPhotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = currentPhotoUrl,
                        contentDescription = "Lifestyle Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Swipe-tap overlays
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (photos.size > 1) {
                                        activePhotoIndex = (activePhotoIndex - 1 + photos.size) % photos.size
                                    }
                                }
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.65f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (photos.size > 1) {
                                        activePhotoIndex = (activePhotoIndex + 1) % photos.size
                                    }
                                }
                        )
                    }

                    // Multi-segmented photo progress bars at top
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(top = 10.dp, start = 14.dp, end = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        photos.forEachIndexed { idx, _ ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(
                                        if (idx == activePhotoIndex) TealAccent else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }

                    // Floating brand badge on bottom-right of image
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AvatarGradients[profile.avatarGradientIndex])
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(profile.avatarEmoji, fontSize = 22.sp)
                    }
                } else {
                    // Fallback to centered avatar emoji
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = profile.avatarEmoji, fontSize = 96.sp)
                    }
                }

                // Highlight Swipe Feedback
                if (dragOffsetX > 20f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(20.dp)
                            .background(TealVibrant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("LIKE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else if (dragOffsetX < -20f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp)
                            .background(Color.Red, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("PASS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // Hovering badges at bottom start of photo block
                var showScanDialog by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                ) {
                    // Verified / Safe Badge
                    VerifiedBadge(profile = profile)

                    // FC Trust Score Ring
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Trust Rating",
                            tint = if (profile.trustScore > 75) TealAccent else Color.Red,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "FC Trust: ${profile.trustScore}%",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // On-Demand AI integrity scanner trigger
                    IconButton(
                        onClick = {
                            showScanDialog = true
                            viewModel.scanProfileIntegrity(profile)
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, TealAccent.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "AI Audit",
                            tint = TealAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                if (showScanDialog) {
                    ProfileIntegrityScannerDialog(profile = profile, viewModel = viewModel, onDismiss = { showScanDialog = false })
                }
            }

            // Bottom Profile Content Details Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${profile.name}, ${profile.age}",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (profile.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            VerifiedBadge(profile = profile, showText = false, iconSize = 20.dp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(profile.location, color = Color.LightGray, fontSize = 12.sp)
                    }
                }

                // Interests Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeMood = if (profile.isUser) viewModel.activeUserMood.collectAsStateWithLifecycle().value else profile.moodBadge
                    val activeGoal = if (profile.isUser) viewModel.activeUserGoal.collectAsStateWithLifecycle().value else profile.datingGoal

                    if (!activeMood.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .background(Brush.linearGradient(listOf(Color(0xFFE040FB), Color(0xFF00E5FF))), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(activeMood, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!activeGoal.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .background(Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFFFFC107))), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(activeGoal, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    profile.interestList.take(2).forEach { interest ->
                        Box(
                            modifier = Modifier
                                .background(NavyLight, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(interest, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                Text(
                    text = profile.bio,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // --- NEW: Feed Interactions Bar (Like, Comment, Share, Bookmark) ---
                var isCardLiked by remember(profile.id) { mutableStateOf(false) }
                var cardLikesCount by remember(profile.id) { mutableIntStateOf(profile.trustScore + 42) }
                var showCardComments by remember { mutableStateOf(false) }
                var showCardShareMenu by remember { mutableStateOf(false) }
                var isBookmarked by remember(profile.id) { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Like Option
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isCardLiked = !isCardLiked
                                if (isCardLiked) {
                                    cardLikesCount++
                                    viewModel.triggerConfetti()
                                    viewModel.showNotification("💖 Liked ${profile.name}'s feed post!")
                                } else {
                                    cardLikesCount--
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCardLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Feed",
                            tint = if (isCardLiked) Color.Red else Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$cardLikesCount",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. Comment Option
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCardComments = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "Comment Feed",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "15",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 3. Share Option
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCardShareMenu = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Feed",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Share",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 4. Save/Bookmark (The "one more" option!)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isBookmarked = !isBookmarked
                                if (isBookmarked) {
                                    viewModel.showNotification("🔖 Added ${profile.name} to your Bookmark Feed list!")
                                } else {
                                    viewModel.showNotification("Removed ${profile.name} from bookmarks.")
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save Feed",
                            tint = if (isBookmarked) TealAccent else Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Interactive Overlays
                if (showCardComments) {
                    CommentsDialog(profile = profile, onDismiss = { showCardComments = false })
                }

                if (showCardShareMenu) {
                    ProfileCardShareDialog(profile = profile, viewModel = viewModel, onDismiss = { showCardShareMenu = false })
                }

                // Gemini AI Match recommendations on Card
                if (compatibilityInfo != null) {
                    Divider(color = NavyLight.copy(alpha = 0.4f), thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NavyDark.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, TealVibrant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TealVibrant.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${compatibilityInfo.first}%",
                                color = TealAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🤖 Gemini Compatibility Match", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(compatibilityInfo.second, color = Color.White, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = TealVibrant, strokeWidth = 1.5.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI analyzing compatibility score...", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCardShareDialog(profile: Profile, viewModel: DatingViewModel, onDismiss: () -> Unit) {
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val profiles by viewModel.otherProfiles.collectAsStateWithLifecycle()
    val groupChats by viewModel.groupChats.collectAsStateWithLifecycle()
    
    val friends = remember(matches, profiles) {
        matches.mapNotNull { match ->
            profiles.find { it.id == match.matchedUserId || it.id == match.userId }?.takeIf { it.id != profile.id }
        }.distinctBy { it.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Share ${profile.name}'s Profile",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recommend ${profile.name} to your friends and matched connections:",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                
                Text("Your Matches", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                if (friends.isEmpty()) {
                    Text("No matched connections yet. Match first to share!", color = Color.DarkGray, fontSize = 11.sp)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(friends) { friend ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.showNotification("🔗 Shared ${profile.name}'s profile with ${friend.name}!")
                                        onDismiss()
                                    }
                                    .padding(4.dp)
                                    .width(64.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(AvatarGradients[friend.avatarGradientIndex % AvatarGradients.size]),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(friend.avatarEmoji, fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = friend.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text("Message Groups", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                if (groupChats.isEmpty()) {
                    Text("No message groups active.", color = Color.DarkGray, fontSize = 11.sp)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(groupChats) { group ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.showNotification("🔗 Shared ${profile.name}'s profile in group: ${group.name}!")
                                        onDismiss()
                                    }
                                    .padding(4.dp)
                                    .width(64.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(AvatarGradients[group.avatarGradientIndex % AvatarGradients.size]),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(group.avatarEmoji, fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = group.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

// --- INSTAGRAM-STYLE SOCIAL FEED DATA CLASS & SCREENS ---

data class SocialPost(
    val id: String,
    val profile: Profile,
    val timeAgo: String,
    val location: String,
    val images: List<String>,
    val caption: String,
    val isLiked: Boolean,
    val likesCount: Int,
    val isBookmarked: Boolean,
    val comments: List<Triple<String, String, String>> // Author, text, emoji
)

@Composable
fun SocialFeedScreen(viewModel: DatingViewModel) {
    val profiles by viewModel.otherProfiles.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Create persistent social posts state mapped from dynamic profiles list
    val postsState = remember(profiles) {
        val initialPosts = mutableListOf<SocialPost>()
        profiles.forEachIndexed { index, profile ->
            initialPosts.add(
                SocialPost(
                    id = "post_${profile.id}",
                    profile = profile,
                    timeAgo = when (index % 4) {
                        0 -> "Just now"
                        1 -> "2 hours ago"
                        2 -> "5 hours ago"
                        else -> "1 day ago"
                    },
                    location = profile.location,
                    images = listOf(profile.image1, profile.image2, profile.image3).filter { it.isNotBlank() },
                    caption = when (index % 5) {
                        0 -> "Sunday brunch vibes! Sunday is for coffee, laughs, and searching for that perfect vibe connection. 🥞☕✨"
                        1 -> "Conquered the mountain trail today! Nothing beats fresh air, panoramic peaks, and zero compromises. 🌲⛰️ Let's hit the outdoors!"
                        2 -> "Exhibition walk. Art captures what words fail to say. Let's debate modern design over some premium gelato! 🎨🍕"
                        3 -> "Pottery studio session today. Crafting clay teaches you focus, patience, and how to embrace beautiful imperfections. 🏺🧩"
                        else -> "Rainy Friday gaming setup. Cozy blankets, vintage audio synth, and premium woodfired pizza. 🎮🍕 Who's my co-op player?"
                    },
                    isLiked = index % 3 == 0,
                    likesCount = profile.trustScore * 2 + 15,
                    isBookmarked = false,
                    comments = listOf(
                        Triple("Mia_VibeCoach", "This photo has amazing natural lighting! Huge green flag! 💚", "👩‍💼"),
                        Triple("Aiden_Check", "Love the absolute design aesthetic here! Visual masterpiece.", "🔥")
                    )
                )
            )
        }
        initialPosts
    }

    // Dynamic state list containing both original and infinitely loaded extra posts
    val postsList = remember(postsState) {
        mutableStateListOf<SocialPost>().apply { addAll(postsState) }
    }

    // Helper to generate dynamic, authentic random extra posts endlessly
    fun generateRandomExtraPost(index: Int): SocialPost {
        val randomNames = listOf("Sophia", "Liam", "Olivia", "Noah", "Emma", "Jackson", "Ava", "Lucas", "Isabella", "Oliver", "Maya", "Ethan", "Zoe")
        val randomEmojis = listOf("✨", "🌟", "🌸", "🎨", "🍵", "⛰️", "🎸", "🎧", "🍕", "🐶", "🐱", "🥂", "✈️", "🌿", "📷")
        val randomLocations = listOf("Downtown Lounge", "Sunset Cliffs", "Metropolitan Museum", "Botanical Garden", "The Roastery Coffee", "Summit View Trail", "Cozy Corner Bakery", "Art District Loft")
        val randomCaptions = listOf(
            "Catching golden hour! Let's find some good music and discuss philosophy over dynamic views. 🌅🎧",
            "Weekend getaway. Unplugging from the tech world, connecting with real, raw nature vibes. 🌲💚",
            "Exploring local record stores today. Vinyl hits different. What's your favorite track of all time? 🎵📻",
            "Tasting the best sourdough pizza in town. Good food is meant to be shared! 🍕✨ Let's plan a foodie date.",
            "Just a quiet morning with my favorite journal and a pour-over coffee. Intentional living wins every single time. ☕🌿",
            "Late-night programming playlist and dynamic ambient lights. Cyberpunk aesthetic complete. 🌌💻",
            "Self-care afternoon. Face masks, peaceful lofi music, and a warm cup of matcha latte. 🍵✨ Remember to slow down!",
            "Strolling through the botanical gardens. Nature has this beautiful way of reminding us that growth takes time. 🌺🌿"
        )
        val randomImages = listOf(
            "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&w=800&q=80"
        )
        
        val name = randomNames[index % randomNames.size]
        val emoji = randomEmojis[index % randomEmojis.size]
        val gradIdx = index % 6
        val loc = randomLocations[index % randomLocations.size]
        val caption = randomCaptions[index % randomCaptions.size]
        val image = randomImages[index % randomImages.size]
        
        val customProfile = Profile(
            id = 1000 + index,
            name = name,
            age = 21 + (index % 10),
            bio = "Exploring the beauty of authentic connections.",
            gender = "Everyone",
            interestedIn = "Everyone",
            location = loc,
            interests = "Music, Art, Travel",
            avatarGradientIndex = gradIdx,
            avatarEmoji = emoji,
            isVerified = true,
            trustScore = 80 + (index % 21),
            moodBadge = "✨ Cosmic Slider",
            datingGoal = "✨ Open to Options",
            image1 = image
        )
        
        return SocialPost(
            id = "extra_post_$index",
            profile = customProfile,
            timeAgo = "${(index % 5) + 1} hours ago",
            location = loc,
            images = listOf(image),
            caption = caption,
            isLiked = index % 2 == 0,
            likesCount = 45 + (index * 12) % 300,
            isBookmarked = false,
            comments = listOf(
                Triple("VibeExplorer", "This looks incredibly cozy! Count me in. 🥂", "✨"),
                Triple("DynamicPioneer", "The composition of this photo is elite.", "🎨")
            )
        )
    }

    val listState = rememberLazyListState()

    // Derived state to check if we are scrolling near the bottom (triggers continuous load)
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
            }
        }
    }

    // Effect to continuously load more entries endlessly
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoadingMore && !isRefreshing) {
            isLoadingMore = true
            delay(1000) // Beautiful delay simulating visual skeleton loading
            val currentSize = postsList.size
            for (i in 0 until 3) {
                postsList.add(generateRandomExtraPost(currentSize + i))
            }
            isLoadingMore = false
        }
    }

    var selectedStoryProfile by remember { mutableStateOf<Profile?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Stories tray at the very top
            StoriesTray(
                profiles = profiles,
                onStoryClick = { selectedStoryProfile = it }
            )

            Divider(color = NavyLight.copy(alpha = 0.5f), thickness = 1.dp)

            if (isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TealVibrant, modifier = Modifier.size(24.dp))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(postsList, key = { it.id }) { post ->
                    InstagramPostCard(
                        post = post,
                        viewModel = viewModel
                    )
                }

                if (isLoadingMore) {
                    item {
                        PostCardSkeleton()
                    }
                }
            }
        }

        // Pull to refresh simulation floating button
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    isRefreshing = true
                    viewModel.showNotification("🔄 Refreshing social feed posts...")
                    delay(1200)
                    postsList.clear()
                    postsList.addAll(postsState)
                    isRefreshing = false
                    viewModel.showNotification("✨ Feed updated successfully!")
                }
            },
            containerColor = TealVibrant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(46.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh Feed", tint = Color.White)
        }

        // Full Screen Story viewer dialog
        selectedStoryProfile?.let { storyProfile ->
            StoryViewerDialog(
                profile = storyProfile,
                onDismiss = { selectedStoryProfile = null },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun StoriesTray(profiles: List<Profile>, onStoryClick: (Profile) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(vertical = 12.dp, horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { }
            ) {
                Box(
                    modifier = Modifier.size(62.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(NavyLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👋", fontSize = 32.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(TealVibrant)
                            .border(2.dp, DarkBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add story",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Your Story", color = Color.Gray, fontSize = 10.sp)
            }
        }

        items(profiles) { profile ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onStoryClick(profile) }
                    .testTag("story_item_${profile.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(Color(0xFFFF3D00), Color(0xFFFF007F), Color(0xFFE040FB), Color(0xFF00E5FF), Color(0xFFFF3D00))
                            ),
                            shape = CircleShape
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(AvatarGradients[profile.avatarGradientIndex % AvatarGradients.size]),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(profile.avatarEmoji, fontSize = 30.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = profile.name,
                    color = Color.White,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(62.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StoryViewerDialog(profile: Profile, onDismiss: () -> Unit, viewModel: DatingViewModel) {
    var progress by remember { mutableStateOf(0f) }
    var paused by remember { mutableStateOf(false) }

    LaunchedEffect(paused) {
        if (!paused) {
            while (progress < 1.0f) {
                delay(100)
                progress += 0.02f
            }
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = profile.image1.ifEmpty { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80" },
                contentDescription = "${profile.name} story image",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { paused = !paused },
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = TealAccent,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AvatarGradients[profile.avatarGradientIndex % AvatarGradients.size]),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(profile.avatarEmoji, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (profile.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified profile",
                                    tint = TealVibrant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "Active 2h ago • ${profile.location}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Story", tint = Color.White)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val emojis = listOf("❤️", "🔥", "😂", "😮", "😢", "👏")
                    emojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    viewModel.triggerConfetti()
                                    viewModel.showNotification("Sent $emoji reaction to ${profile.name}'s story!")
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var replyText by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Send quick reply...", color = Color.LightGray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                viewModel.showNotification("Sent reply to ${profile.name}: \"$replyText\"")
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(TealVibrant, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InstagramPostCard(post: SocialPost, viewModel: DatingViewModel) {
    var isLiked by remember(post.id) { mutableStateOf(post.isLiked) }
    var likesCount by remember(post.id) { mutableIntStateOf(post.likesCount) }
    var isBookmarked by remember(post.id) { mutableStateOf(post.isBookmarked) }

    val localComments = remember(post.id) { mutableStateListOf<Triple<String, String, String>>().apply { addAll(post.comments) } }
    var commentText by remember { mutableStateOf("") }

    var showHeartPop by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, NavyLight, RoundedCornerShape(16.dp))
            .testTag("post_card_${post.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AvatarGradients[post.profile.avatarGradientIndex % AvatarGradients.size]),
                    contentAlignment = Alignment.Center
                ) {
                    Text(post.profile.avatarEmoji, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.profile.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${post.profile.age}",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        if (post.profile.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge(profile = post.profile, showText = false, iconSize = 14.dp)
                        }
                    }
                    Text(
                        text = "${post.location} • ${post.timeAgo}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = { showOptionsDialog = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                }
            }

            val photos = post.images
            var currentPhotoIndex by remember { mutableIntStateOf(0) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!isLiked) {
                                    isLiked = true
                                    likesCount++
                                }
                                showHeartPop = true
                                viewModel.triggerConfetti()
                                viewModel.showNotification("💖 Double tap Liked ${post.profile.name}'s post!")
                            },
                            onTap = {
                                if (photos.isNotEmpty()) {
                                    currentPhotoIndex = (currentPhotoIndex + 1) % photos.size
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (photos.isNotEmpty()) {
                    AsyncImage(
                        model = photos[currentPhotoIndex],
                        contentDescription = "Post image index $currentPhotoIndex",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (photos.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${currentPhotoIndex + 1}/${photos.size}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NavyDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📷 Visual feed processing...", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                if (showHeartPop) {
                    LaunchedEffect(Unit) {
                        delay(700)
                        showHeartPop = false
                    }
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Liked Heart Pop",
                            tint = Color.Red,
                            modifier = Modifier.size(90.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            if (isLiked) {
                                likesCount++
                                viewModel.triggerConfetti()
                                viewModel.showNotification("💖 Liked ${post.profile.name}'s feed post!")
                            } else {
                                likesCount--
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Button",
                            tint = if (isLiked) Color.Red else Color.LightGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.showNotification("💬 Scroll down to add a custom comment!") }) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "Comment Button",
                            tint = Color.LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(onClick = { showShareMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Button",
                            tint = Color.LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        isBookmarked = !isBookmarked
                        if (isBookmarked) {
                            viewModel.showNotification("🔖 Bookmarked ${post.profile.name}'s post!")
                        } else {
                            viewModel.showNotification("Removed bookmark.")
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark Button",
                        tint = if (isBookmarked) TealAccent else Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Liked by ${if (isLiked) "you and " else ""}$likesCount others",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                                append("${post.profile.name} ")
                            }
                            withStyle(style = SpanStyle(color = Color.LightGray)) {
                                append(post.caption)
                            }
                        },
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(NavyDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dating compatibility: Verified trust score is ${post.profile.trustScore}% (${post.profile.datingGoal})",
                            color = TealAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (localComments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Comments (${localComments.size})",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        localComments.forEach { (author, text, emoji) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TealAccent)) {
                                            append("$author ")
                                        }
                                        withStyle(style = SpanStyle(color = Color.White)) {
                                            append(text)
                                        }
                                    },
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add comment...", color = Color.Gray, fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("comment_input_${post.id}"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = NavyLight,
                            focusedContainerColor = NavyDark.copy(alpha = 0.3f),
                            unfocusedContainerColor = NavyDark.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    TextButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                localComments.add(Triple("You", commentText, "😎"))
                                viewModel.showNotification("💬 Added your comment on ${post.profile.name}'s post!")
                                commentText = ""
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = TealAccent)
                    ) {
                        Text("Post", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showShareMenu) {
        ProfileCardShareDialog(
            profile = post.profile,
            viewModel = viewModel,
            onDismiss = { showShareMenu = false }
        )
    }

    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Vibe Feed Actions", color = Color.White) },
            text = { Text("Customize notifications, mute alerts, report profile, or bookmark this secure feed entry.", color = Color.Gray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.showNotification("🚫 Reporting process initiated for ${post.profile.name}...")
                        showOptionsDialog = false
                    }
                ) {
                    Text("Report Post", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOptionsDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun PostCardSkeleton() {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Box(modifier = Modifier.width(100.dp).height(12.dp).background(Color.Gray.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.width(60.dp).height(8.dp).background(Color.Gray.copy(alpha = 0.2f)))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Gray.copy(alpha = 0.15f)))
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.width(150.dp).height(10.dp).background(Color.Gray.copy(alpha = 0.2f)))
        }
    }
}

// --- AI Recommendations Screen ---

@Composable
fun AIRecommendationsScreen(viewModel: DatingViewModel, onNavigateToChat: (Int) -> Unit) {
    val profiles by viewModel.otherProfiles.collectAsStateWithLifecycle()
    val compatibilityMap by viewModel.compatibilityCache.collectAsStateWithLifecycle()
    val matchesList by viewModel.matches.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Curated AI Recommendations",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gemini has scanned interest maps and personality alignments to curate these highest matching potentials.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        val sortedRecommendations = profiles
            .map { p -> p to (compatibilityMap[p.id]?.first ?: 80) }
            .sortedByDescending { it.second }

        if (sortedRecommendations.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TealVibrant)
                }
            }
        } else {
            items(sortedRecommendations) { (profile, score) ->
                val compInfo = compatibilityMap[profile.id]
                val isMatchedAlready = matchesList.any { it.matchedUserId == profile.id || it.userId == profile.id }

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Icon Circle
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AvatarGradients[profile.avatarGradientIndex]),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(profile.avatarEmoji, fontSize = 32.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Text content
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${profile.name}, ${profile.age}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (profile.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.CheckCircle, "Verified", tint = TealAccent, modifier = Modifier.size(16.dp))
                                }
                            }

                            Text("Compatibility Score: $score%", color = TealAccent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = compInfo?.second ?: "Analyzing personality traits alignment...",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Match Action Button
                        if (isMatchedAlready) {
                            Button(
                                onClick = {
                                    val activeMatch = matchesList.find { it.matchedUserId == profile.id || it.userId == profile.id }
                                    if (activeMatch != null) onNavigateToChat(activeMatch.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyLight),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("Chat", color = Color.White, fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.swipeRight(profile) },
                                colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("Match", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Chats List Screen ---

@Composable
fun ChatsListScreen(
    viewModel: DatingViewModel,
    onNavigateToChat: (Int) -> Unit,
    onNavigateToGroupChat: (Int) -> Unit
) {
    val matchesList by viewModel.matches.collectAsStateWithLifecycle()
    val profiles by viewModel.otherProfiles.collectAsStateWithLifecycle()
    val groupChats by viewModel.groupChats.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("direct") } // "direct" or "groups"
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Connections",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Initiate chats with your matched verified partners or connect in thematic group chats.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedTab = "direct" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "direct") TealVibrant else Color.Transparent,
                    contentColor = if (selectedTab == "direct") Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = "DMs", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Direct Messages", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { selectedTab = "groups" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "groups") TealVibrant else Color.Transparent,
                    contentColor = if (selectedTab == "groups") Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Groups", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Message Groups", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == "direct") {
            if (matchesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = "No Matches", tint = NavyLight, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matches yet!", color = Color.LightGray, fontWeight = FontWeight.Bold)
                        Text("Go back to the Deck and swipe right to build connections.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(matchesList) { match ->
                        val otherProfile = profiles.find { it.id == match.matchedUserId || it.id == match.userId }
                        if (otherProfile != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToChat(match.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar circle
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(AvatarGradients[otherProfile.avatarGradientIndex]),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(otherProfile.avatarEmoji, fontSize = 24.sp)
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = otherProfile.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            if (otherProfile.isVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.CheckCircle, "Verified", tint = TealAccent, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Text(
                                            text = otherProfile.bio,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Matched",
                                        tint = TealVibrant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Groups Tab
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thematic Groups", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Button(
                        onClick = { showCreateGroupDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, "Create Group", tint = TealAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Group", color = TealAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (groupChats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No message groups found. Create one above!", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(groupChats) { group ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToGroupChat(group.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(AvatarGradients[group.avatarGradientIndex]),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(group.avatarEmoji, fontSize = 24.sp)
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = group.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = group.description,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Enter Group",
                                        tint = TealAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }
        var groupEmoji by remember { mutableStateOf("💬") }

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create Message Group", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = groupEmoji,
                        onValueChange = { groupEmoji = it },
                        label = { Text("Group Emoji Icon (e.g. 👾, 🥐, 💃)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank() && groupDesc.isNotBlank()) {
                            viewModel.createGroupChat(groupName, groupDesc, groupEmoji)
                            showCreateGroupDialog = false
                        } else {
                            viewModel.showNotification("⚠️ Please fill out all fields.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealVibrant)
                ) {
                    Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// --- Active Chat Conversation Screen ---

@Composable
fun ChatScreen(
    matchId: Int,
    viewModel: DatingViewModel,
    onBack: () -> Unit
) {
    val profiles by viewModel.otherProfiles.collectAsStateWithLifecycle()
    val matchesList by viewModel.matches.collectAsStateWithLifecycle()
    val messages by viewModel.getMessagesForMatch(matchId).collectAsStateWithLifecycle(initialValue = emptyList())

    val activeMatch = matchesList.find { it.id == matchId }
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val otherProfile = activeMatch?.let { match ->
        profiles.find { it.id == match.matchedUserId || it.id == match.userId }
    }

    var messageText by remember { mutableStateOf("") }

    // Icebreaker States
    val icebreakersList by viewModel.icebreakers.collectAsStateWithLifecycle()
    val icebreakersLoading by viewModel.icebreakersLoading.collectAsStateWithLifecycle()

    // Call Engine States
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val callType by viewModel.callType.collectAsStateWithLifecycle()
    val callDurationSeconds by viewModel.callDurationSeconds.collectAsStateWithLifecycle()
    val isCallMuted by viewModel.isCallMuted.collectAsStateWithLifecycle()
    val isCallCameraOff by viewModel.isCallCameraOff.collectAsStateWithLifecycle()
    val isCallSpeakerOn by viewModel.isCallSpeakerOn.collectAsStateWithLifecycle()
    val simulatedCallTranscript by viewModel.simulatedCallTranscript.collectAsStateWithLifecycle()

    // Self-Destruct Message Shredder timer (0 = Off)
    var selfDestructSeconds by remember { mutableStateOf(0) }

    val currentTimestamp = System.currentTimeMillis()
    val filteredMessages = if (selfDestructSeconds > 0) {
        messages.filter { currentTimestamp - it.timestamp < selfDestructSeconds * 1000 }
    } else {
        messages
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (otherProfile != null) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AvatarGradients[otherProfile.avatarGradientIndex]),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(otherProfile.avatarEmoji, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(otherProfile.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        if (otherProfile.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedBadge(profile = otherProfile, showText = false, iconSize = 16.dp)
                                        }
                                    }
                                    Text("Online • verified safety", fontSize = 10.sp, color = TealAccent)
                                }
                            } else {
                                Text("Chat", fontSize = 16.sp, color = Color.White)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        if (otherProfile != null) {
                            // Audio Call Action
                            IconButton(
                                onClick = { viewModel.initiateCall(otherProfile, "AUDIO") },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .background(NavyDark, RoundedCornerShape(8.dp))
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = TealAccent, modifier = Modifier.size(18.dp))
                            }

                            // Video Call Action
                            IconButton(
                                onClick = { viewModel.initiateCall(otherProfile, "VIDEO") },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .background(NavyDark, RoundedCornerShape(8.dp))
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Video Call", tint = TealAccent, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Icebreaker Trigger Button
                        IconButton(
                            onClick = {
                                if (otherProfile != null) {
                                    viewModel.fetchIcebreakersForActiveMatch(
                                        matchName = otherProfile.name,
                                        matchBio = otherProfile.bio,
                                        matchInterests = otherProfile.interests
                                    )
                                }
                            },
                            modifier = Modifier
                                .background(NavyDark, RoundedCornerShape(8.dp))
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Suggest Icebreakers", tint = TealAccent, modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            },
            containerColor = DarkBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 🛡️ Quantum Security & Self Destruct Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyDark)
                        .border(0.5.dp, TealAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Secure Link",
                            tint = TealAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quantum E2E Encrypted (AES-256)",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (selfDestructSeconds > 0) Color.Red.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                selfDestructSeconds = when (selfDestructSeconds) {
                                    0 -> 10
                                    10 -> 30
                                    30 -> 60
                                    else -> 0
                                }
                                viewModel.showNotification(
                                    if (selfDestructSeconds > 0) 
                                        "⏱️ Self-Destruct Shred Timer active: messages wipe after $selfDestructSeconds seconds!" 
                                    else 
                                        "⏱️ Self-Destruct Timer deactivated."
                                )
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Shredder",
                            tint = if (selfDestructSeconds > 0) Color.Red else TealAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selfDestructSeconds > 0) "Shred in ${selfDestructSeconds}s" else "Shredder Off",
                            color = if (selfDestructSeconds > 0) Color.Red else TealAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Icebreakers suggestions panel
                if (icebreakersLoading || icebreakersList.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "AI Sparkle", tint = TealAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🤖 Gemini Custom Icebreakers", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (icebreakersLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = TealVibrant, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gemini composing original openers...", color = Color.Gray, fontSize = 11.sp)
                                }
                            } else {
                                icebreakersList.forEach { opener ->
                                    Text(
                                        text = opener,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { messageText = opener }
                                            .border(0.5.dp, NavyLight, RoundedCornerShape(6.dp))
                                            .padding(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }

                // Message dialogue bubbles list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(10.dp)) }

                    items(filteredMessages) { msg ->
                        val isMyMsg = msg.senderId == (userProfile?.id ?: 0)
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMyMsg) Alignment.End else Alignment.Start
                        ) {
                            if (msg.isModerated) {
                                // Blocked message overlay for moderation checks
                                Box(
                                    modifier = Modifier
                                        .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.Red, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .widthIn(max = 280.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = "Safety Alert", tint = Color.Red, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("MESSAGE BLOCKED", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${msg.content}\"",
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.moderationWarning ?: "Content flagged by Gemini Safety Filter.",
                                            color = Color.Red,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            } else {
                                // Regular beautiful bubbly message or Shared Reel Card
                                if (msg.content.startsWith("🎬 Shared Reel:")) {
                                    val contentParts = msg.content.removePrefix("🎬 Shared Reel:").trim()
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = NavyDark),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .border(1.5.dp, TealAccent, RoundedCornerShape(16.dp))
                                            .clickable {
                                                viewModel.showNotification("🎬 Launching shared video reel: $contentParts")
                                            }
                                            .widthIn(max = 280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(TealAccent.copy(alpha = 0.2f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Play Reel",
                                                        tint = TealAccent,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "Shared Video Reel",
                                                        color = TealAccent,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = contentParts,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "🎬 Tap to watch this video reel and listen to the song!",
                                                color = Color.LightGray,
                                                fontSize = 10.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                brush = if (isMyMsg) 
                                                    Brush.linearGradient(listOf(TealVibrant, TealAccent))
                                                else 
                                                    Brush.linearGradient(listOf(DarkSurface, DarkSurface)),
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isMyMsg) 16.dp else 0.dp,
                                                    bottomEnd = if (isMyMsg) 0.dp else 16.dp
                                                )
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                            .widthIn(max = 280.dp)
                                    ) {
                                        Text(
                                            text = msg.content,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Chat Input row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Send a secure message...", color = Color.Gray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TealVibrant,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(matchId, messageText)
                                messageText = ""
                                keyboardController?.hide()
                            }
                        })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(matchId, messageText)
                                messageText = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(TealVibrant, CircleShape)
                            .testTag("send_chat_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Msg", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Active Encrypted Call Overlay
        if (callState != "IDLE") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NavyDark.copy(alpha = 0.98f))
                    .clickable { /* prevent click-through */ }
            ) {
                if (callState == "CONNECTING" || callState == "RINGING") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Pulsing Avatar
                            val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "AvatarScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .background(
                                        if (otherProfile != null) 
                                            AvatarGradients[otherProfile.avatarGradientIndex]
                                        else 
                                            Brush.linearGradient(listOf(TealVibrant, TealAccent))
                                    )
                                    .border(2.dp, TealAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(otherProfile?.avatarEmoji ?: "👤", fontSize = 54.sp)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = otherProfile?.name ?: "Unknown Peer",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, "Lock", tint = TealAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (callState == "CONNECTING") "Securing quantum tunnel..." else "Ringing secure line...",
                                    color = TealAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Encryption metadata
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SECURE HANDSHAKE META:", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("E2EE: AES-256-GCM / Peer-to-Peer Direct", color = Color.LightGray, fontSize = 10.sp)
                                Text("Key Fingerprint: SHA-256 [${matchId}-${otherProfile?.id ?: 0}-OK]", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Cancel call button
                        IconButton(
                            onClick = { viewModel.endActiveCall() },
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Red, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Decline Call", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                } else if (callState == "CONNECTED") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, "E2EE Verified", tint = TealAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Secure Direct Peer Line", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            val minutes = callDurationSeconds / 60
                            val seconds = callDurationSeconds % 60
                            val timeStr = String.format("%02d:%02d", minutes, seconds)
                            Text(timeStr, color = TealAccent, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }

                        // Immersive background visual track
                        if (callType == "VIDEO") {
                            // Immersive video simulation
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        AvatarGradients[otherProfile?.avatarGradientIndex ?: 0]
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCallCameraOff) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Face, "Camera Off", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Camera Feed Suspended", color = Color.Gray, fontSize = 12.sp)
                                    }
                                } else {
                                    // Simulated user video track
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(otherProfile?.avatarEmoji ?: "👤", fontSize = 90.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("${otherProfile?.name} (Direct Stream)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("E2EE Live Match Cam", color = TealAccent, fontSize = 10.sp)
                                    }

                                    // Local PIP floating preview card (represents user's own front camera)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(16.dp)
                                            .size(70.dp, 100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NavyLight)
                                            .border(1.dp, TealAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(userProfile?.avatarEmoji ?: "🤩", fontSize = 24.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("You (Live)", color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Immersive audio simulation
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "Waveform")
                                val wave1 by infiniteTransition.animateFloat(
                                    initialValue = 20f,
                                    targetValue = 90f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "Wave1"
                                )
                                val wave2 by infiniteTransition.animateFloat(
                                    initialValue = 40f,
                                    targetValue = 130f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "Wave2"
                                )

                                Canvas(modifier = Modifier.size(240.dp)) {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    drawCircle(
                                        color = TealAccent.copy(alpha = 0.1f),
                                        radius = wave2,
                                        center = center
                                    )
                                    drawCircle(
                                        color = TealVibrant.copy(alpha = 0.15f),
                                        radius = wave1,
                                        center = center
                                    )
                                    drawCircle(
                                        color = NavyDark,
                                        radius = 60.dp.toPx(),
                                        center = center
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (otherProfile != null) 
                                                    AvatarGradients[otherProfile.avatarGradientIndex]
                                                else 
                                                    Brush.linearGradient(listOf(TealVibrant, TealAccent))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(otherProfile?.avatarEmoji ?: "👤", fontSize = 38.sp)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(otherProfile?.name ?: "Unknown Peer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("🔒 Quantum Audio Feed Active", color = TealAccent, fontSize = 10.sp)
                                }
                            }
                        }

                        // Simulated Dialogue Real-time Subtitles Feed
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(TealVibrant.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Info, "Live Text", tint = TealAccent, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SECURE TRANSCRIBE FEED:", color = TealAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = simulatedCallTranscript,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }

                        // Call Action Controls Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute button
                            IconButton(
                                onClick = { viewModel.toggleCallMute() },
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(if (isCallMuted) Color.Red.copy(alpha = 0.4f) else DarkSurface, CircleShape)
                                    .border(1.dp, if (isCallMuted) Color.Red else Color.DarkGray, CircleShape)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Mute", tint = if (isCallMuted) Color.Red else Color.White, modifier = Modifier.size(20.dp))
                            }

                            // Camera on/off (Video only)
                            if (callType == "VIDEO") {
                                IconButton(
                                    onClick = { viewModel.toggleCallCamera() },
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(if (isCallCameraOff) Color.Red.copy(alpha = 0.4f) else DarkSurface, CircleShape)
                                        .border(1.dp, if (isCallCameraOff) Color.Red else Color.DarkGray, CircleShape)
                                ) {
                                    Icon(Icons.Default.Face, contentDescription = "Camera Toggle", tint = if (isCallCameraOff) Color.Red else Color.White, modifier = Modifier.size(20.dp))
                                }
                            }

                            // Speaker on/off
                            IconButton(
                                onClick = { viewModel.toggleCallSpeaker() },
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(if (isCallSpeakerOn) TealVibrant.copy(alpha = 0.2f) else DarkSurface, CircleShape)
                                    .border(1.dp, if (isCallSpeakerOn) TealAccent else Color.DarkGray, CircleShape)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Speaker Toggle", tint = if (isCallSpeakerOn) TealAccent else Color.White, modifier = Modifier.size(20.dp))
                            }

                            // Hang Up red button
                            IconButton(
                                onClick = { viewModel.endActiveCall() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.Red, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Hang Up", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                } else if (callState == "ENDED") {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, "Call Secure Summary", tint = TealAccent, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Secure Connection Terminated", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tunnel destroyed cleanly. No metadata retained.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: Int,
    viewModel: DatingViewModel,
    onBack: () -> Unit
) {
    val groupChats by viewModel.groupChats.collectAsStateWithLifecycle()
    val groupChat = groupChats.find { it.id == groupId }
    val messages by viewModel.getMessagesForGroup(groupId).collectAsStateWithLifecycle(initialValue = emptyList())
    var messageText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (groupChat != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AvatarGradients[groupChat.avatarGradientIndex]),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(groupChat.avatarEmoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(groupChat.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(groupChat.description, fontSize = 10.sp, color = TealAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            Text("Group Chat", fontSize = 16.sp, color = Color.White)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Group Chat Status / Shield info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyDark)
                    .border(0.5.dp, TealAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Secure Link",
                        tint = TealAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Moderated Group • verified partners only",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Message dialogue bubbles list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                items(messages) { msg ->
                    val isMyMsg = msg.senderId == 0 // 0 is User
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMyMsg) Alignment.End else Alignment.Start
                    ) {
                        if (!isMyMsg) {
                            // Sender metadata
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(AvatarGradients[msg.senderGradientIndex]),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(msg.senderEmoji, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = msg.senderName,
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (msg.content.startsWith("🎬 Shared Reel:")) {
                            // Custom Shared Reel Card
                            val contentParts = msg.content.removePrefix("🎬 Shared Reel:").trim()
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .border(1.5.dp, TealAccent, RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.showNotification("🎬 Launching shared video reel: $contentParts")
                                    }
                                    .widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(TealAccent.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play Reel",
                                                tint = TealAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Shared Video Reel",
                                                color = TealAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = contentParts,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "🎬 Tap to watch this video reel and listen to the song!",
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        } else {
                            // Regular message bubble
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = if (isMyMsg) 
                                            Brush.linearGradient(listOf(TealVibrant, TealAccent))
                                        else 
                                            Brush.linearGradient(listOf(DarkSurface, DarkSurface)),
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isMyMsg) 16.dp else 0.dp,
                                            bottomEnd = if (isMyMsg) 0.dp else 16.dp
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.content,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Send group message...", color = Color.Gray, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TealVibrant,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("group_chat_input"),
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendGroupMessage(groupId, messageText)
                            messageText = ""
                            keyboardController?.hide()
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendGroupMessage(groupId, messageText)
                            messageText = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(TealVibrant, CircleShape)
                        .testTag("send_group_chat_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Msg", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// --- Profile Analyzer Form ---

@Composable
fun ProfileAnalyzerForm(viewModel: DatingViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    if (userProfile == null) return

    val analysisResult by viewModel.profileAnalysisResult.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.profileAnalysisLoading.collectAsStateWithLifecycle()

    var bioInput by remember(userProfile) { mutableStateOf(userProfile?.bio ?: "") }
    var interestsInput by remember(userProfile) { mutableStateOf(userProfile?.interests ?: "") }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🤖 AI Profile Counselor", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(TealAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("GEMINI POWERED", color = TealAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Analyze your bio & interests to find weak spots, clichés, and get a high-converting optimized bio suggestion.",
                color = Color.Gray,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Bio Input Field
            Text("Your Bio", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = bioInput,
                onValueChange = { bioInput = it },
                placeholder = { Text("Describe yourself...", color = Color.Gray, fontSize = 12.sp) },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TealVibrant,
                    unfocusedBorderColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth().testTag("profile_analyzer_bio_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Interests Input Field
            Text("Your Interests (comma separated)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = interestsInput,
                onValueChange = { interestsInput = it },
                placeholder = { Text("e.g. Coffee, Hiking, Music, Tech", color = Color.Gray, fontSize = 12.sp) },
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TealVibrant,
                    unfocusedBorderColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth().testTag("profile_analyzer_interests_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Analyze Button
                Button(
                    onClick = { viewModel.analyzeUserProfile(bioInput, interestsInput) },
                    enabled = !isAnalyzing && (bioInput.isNotBlank() || interestsInput.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("analyze_profile_btn")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Analyze Profile", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Apply Changes Button
                Button(
                    onClick = { viewModel.updateUserBioAndInterests(bioInput, interestsInput) },
                    enabled = bioInput != userProfile?.bio || interestsInput != userProfile?.interests,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyLight),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("save_profile_changes_btn")
                ) {
                    Text("Apply & Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Results Display Section
            if (analysisResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = NavyLight.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("📈 Analysis & Recommendations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, TealVibrant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        // Simple custom markdown renderer
                        analysisResult!!.lines().forEach { line ->
                            val isHeading = line.startsWith("###") || line.startsWith("##") || line.startsWith("#")
                            val isBullet = line.trim().startsWith("-") || line.trim().startsWith("*")
                            val isBold = line.contains("**")
                            
                            val text = line
                                .replace("#", "")
                                .replace("**", "")
                                .replace("*", "")
                                .trim()
                            
                            if (text.isNotEmpty()) {
                                Text(
                                    text = if (isBullet) "• $text" else text,
                                    color = if (isHeading) TealAccent else Color.White,
                                    fontSize = if (isHeading) 13.sp else 12.sp,
                                    fontWeight = if (isHeading || isBold) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = if (isHeading) FontFamily.Default else FontFamily.SansSerif,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Action: Extract suggested bio or copy suggestions
                        Button(
                            onClick = {
                                // Find any text enclosed in double quotes in the suggestion result, and use it to autofill the Bio field!
                                val regex = Regex("\"([^\"]+)\"")
                                val match = regex.find(analysisResult!!)
                                if (match != null) {
                                    val suggestedBio = match.groupValues[1]
                                    bioInput = suggestedBio
                                    viewModel.showNotification("✨ Autofilled the form with Gemini's suggested bio! Click 'Apply & Save' to write to DB.")
                                } else {
                                    // Fallback: Copy the whole analysis to clipboard or alert
                                    viewModel.showNotification("💡 Read the audit guidelines above to polish your bio manually!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealVibrant.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, TealVibrant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("apply_gemini_suggestion_btn")
                        ) {
                            Text("✨ Use Suggested Bio Draft", color = TealAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComprehensiveProfileBuilder(
    viewModel: DatingViewModel,
    userProfile: Profile
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showBuilderFaceScanDialog by remember { mutableStateOf(false) }
    var editVerified by remember(userProfile) { mutableStateOf(userProfile.isVerified) }
    val context = LocalContext.current

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.showNotification("📍 Location access granted! Coordinates updated.")
        } else {
            viewModel.showNotification("⚠️ Location access is required to find local singles.")
        }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showNotification("🖼️ Photos permission granted! Media library unlocked.")
        } else {
            viewModel.showNotification("⚠️ Media permission denied. Using premium system presets.")
        }
    }

    val cameraVerificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showNotification("📸 Camera granted! Initializing secure face scan...")
            showBuilderFaceScanDialog = true
            editVerified = true
        } else {
            viewModel.showNotification("⚠️ Camera access is required for real-time video verification.")
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with toggle to expand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AvatarGradients[userProfile.avatarGradientIndex]),
                    contentAlignment = Alignment.Center
                ) {
                    Text(userProfile.avatarEmoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "👑 Premium Profile Builder",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (userProfile.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge(profile = userProfile, showText = false, iconSize = 16.dp)
                        }
                    }
                    Text(
                        text = if (isExpanded) "Tap to collapse settings" else "Tap to edit bio, photos & matching preferences",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }
            }

            if (!isExpanded) {
                // Short summary layout
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = NavyLight.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Name: ${userProfile.name}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Age: ${userProfile.age}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Gender: ${userProfile.gender}", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Location: ${userProfile.location}", color = Color.LightGray, fontSize = 12.sp)
                        Text("Tier: ${userProfile.premiumTier}", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Trust Score: ${userProfile.trustScore}%", color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // COMPREHENSIVE DETAILED BUILDER WITH 4 BEAUTIFUL TABS
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = NavyLight.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(14.dp))

                var editName by remember(userProfile) { mutableStateOf(userProfile.name) }
                var editAge by remember(userProfile) { mutableStateOf(userProfile.age.toString()) }
                var editLocation by remember(userProfile) { mutableStateOf(userProfile.location) }
                var editGender by remember(userProfile) { mutableStateOf(userProfile.gender) }
                var editInterestedIn by remember(userProfile) { mutableStateOf(userProfile.interestedIn) }
                
                var editBio by remember(userProfile) { mutableStateOf(userProfile.bio) }
                var editInterests by remember(userProfile) { mutableStateOf(userProfile.interests) }
                
                var editEmoji by remember(userProfile) { mutableStateOf(userProfile.avatarEmoji) }
                var editGradientIndex by remember(userProfile) { mutableIntStateOf(userProfile.avatarGradientIndex) }
                
                var editLat by remember(userProfile) { mutableStateOf(userProfile.latitude.toString()) }
                var editLng by remember(userProfile) { mutableStateOf(userProfile.longitude.toString()) }
                
                var editTier by remember(userProfile) { mutableStateOf(userProfile.premiumTier) }
                
                var editImg1 by remember(userProfile) { mutableStateOf(userProfile.image1) }
                var editImg2 by remember(userProfile) { mutableStateOf(userProfile.image2) }
                var editImg3 by remember(userProfile) { mutableStateOf(userProfile.image3) }
                var showPhotoStudioForSlot by remember { mutableStateOf<Int?>(null) }

                // Collect preferences
                val searchRadius by viewModel.searchRadiusKm.collectAsStateWithLifecycle()
                val minAgePref by viewModel.minAgePreference.collectAsStateWithLifecycle()
                val maxAgePref by viewModel.maxAgePreference.collectAsStateWithLifecycle()

                var editSearchRadius by remember(searchRadius) { mutableIntStateOf(searchRadius) }
                var editMinAge by remember(minAgePref) { mutableIntStateOf(minAgePref) }
                var editMaxAge by remember(maxAgePref) { mutableIntStateOf(maxAgePref) }

                var isSimulatingGps by remember { mutableStateOf(false) }
                var activeSetupTab by remember { mutableStateOf("Basic") } // "Basic", "Bio", "Vibe", "Target"

                // Horizontal Tab Bar Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        "Basic" to "👤 Info",
                        "Bio" to "📝 Story",
                        "Vibe" to "🎨 Vibe",
                        "Target" to "🎯 Matches"
                    )
                    tabs.forEach { (tabKey, label) ->
                        val isSelected = activeSetupTab == tabKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TealAccent.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) TealAccent.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { activeSetupTab = tabKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) TealAccent else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (activeSetupTab) {
                    "Basic" -> {
                        // TAB 1: BASIC INFO
                        Text("👤 Personal Identity Details", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Display Name", color = Color.Gray, fontSize = 11.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent, unfocusedBorderColor = Color.DarkGray),
                                modifier = Modifier.weight(1.5f).testTag("builder_name_input")
                            )
                            OutlinedTextField(
                                value = editAge,
                                onValueChange = { editAge = it },
                                label = { Text("Age", color = Color.Gray, fontSize = 11.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent, unfocusedBorderColor = Color.DarkGray),
                                modifier = Modifier.weight(1f).testTag("builder_age_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editLocation,
                            onValueChange = { editLocation = it },
                            label = { Text("Location City", color = Color.Gray, fontSize = 11.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent, unfocusedBorderColor = Color.DarkGray),
                            modifier = Modifier.fillMaxWidth().testTag("builder_location_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editLat,
                                onValueChange = { editLat = it },
                                label = { Text("Latitude (GPS)", color = Color.Gray, fontSize = 10.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent, unfocusedBorderColor = Color.DarkGray),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editLng,
                                onValueChange = { editLng = it },
                                label = { Text("Longitude (GPS)", color = Color.Gray, fontSize = 10.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent, unfocusedBorderColor = Color.DarkGray),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (fineGranted || coarseGranted) {
                                        isSimulatingGps = true
                                        editLat = "37." + (1000..9999).random()
                                        editLng = "-122." + (1000..9999).random()
                                        isSimulatingGps = false
                                        viewModel.showNotification("📍 Satellites Synchronized! Coordinates updated.")
                                    } else {
                                        locationLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyLight),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text(if (isSimulatingGps) "📡..." else "📍 Sync", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column {
                            Text("My Gender Identity", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Male", "Female", "Non-binary").forEach { g ->
                                    val selected = editGender == g
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (selected) TealAccent else Color.DarkGray, RoundedCornerShape(8.dp))
                                            .clickable { editGender = g }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(g, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    "Bio" -> {
                        // TAB 2: BIO & PHOTOS
                        Text("📝 Bio Narrative & Compulsory Photos", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Bio Character Count Display
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Describe Yourself", color = Color.Gray, fontSize = 11.sp)
                            Text("${editBio.length}/300 chars", color = if (editBio.length > 250) Color.Yellow else Color.Gray, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { if (it.length <= 300) editBio = it },
                            placeholder = { Text("Write your catchy bio here...", color = Color.Gray, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent, unfocusedBorderColor = Color.DarkGray),
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bio Prompt Templates
                        Text("💡 Quick Smart-Bio Suggestions (Tap to insert):", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        val bioPresets = listOf(
                            "☕ Tech architect & coffee purist looking for a Player 2 to explore indie game bars and sunset hiking trails with.",
                            "🎨 Fluid artist & yoga teacher seeking authentic connections. Let's debate sci-fi cinema and discover the best late-night tacos."
                        )
                        bioPresets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NavyDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, TealVibrant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable { editBio = preset; viewModel.showNotification("✨ Bio preset template applied!") }
                                    .padding(8.dp)
                            ) {
                                Text(preset, color = Color.LightGray, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Manage 3 Compulsory Lifestyle Photo Slots", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(1 to editImg1, 2 to editImg2, 3 to editImg3).forEach { (num, url) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkBackground)
                                        .border(
                                            width = 1.dp,
                                            color = if (url.isNotBlank()) TealAccent else Color.DarkGray,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            showPhotoStudioForSlot = num
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (url.isBlank()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            Text("Slot $num", color = Color.Gray, fontSize = 8.sp)
                                        }
                                    } else {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Slot $num",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Active", color = TealAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Vibe" -> {
                        // TAB 3: INTERESTS & VIBE
                        Text("🎨 Vibe Alignments & Interest Tags", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Interactive Smart Tags (Tap to toggle):", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Predefined Tags Grid
                        val predefinedInterests = listOf("Hiking", "Coffee", "Tech", "Music", "Fitness", "Gaming", "Travel", "Art", "Books", "Foodie", "Pets", "Movies")
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val chunked = predefinedInterests.chunked(4)
                            chunked.forEach { rowTags ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowTags.forEach { tag ->
                                        val isSelected = editInterests.split(",").map { it.trim().lowercase() }.contains(tag.lowercase())
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) TealAccent.copy(alpha = 0.2f) else NavyLight)
                                                .border(1.dp, if (isSelected) TealAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val currentList = editInterests.split(",")
                                                        .map { it.trim() }
                                                        .filter { it.isNotBlank() }
                                                        .toMutableList()
                                                    val foundIdx = currentList.indexOfFirst { it.equals(tag, ignoreCase = true) }
                                                    if (foundIdx != -1) {
                                                        currentList.removeAt(foundIdx)
                                                    } else {
                                                        currentList.add(tag)
                                                    }
                                                    editInterests = currentList.joinToString(", ")
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(tag, color = if (isSelected) TealAccent else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editInterests,
                            onValueChange = { editInterests = it },
                            label = { Text("Custom Interests (comma separated)", color = Color.Gray, fontSize = 11.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent, unfocusedBorderColor = Color.DarkGray),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Personalized Avatar Emojis Customizer
                        Text("Personalized Avatar Emoji", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        val emojiGrid = listOf("👩‍💻", "🎨", "🏋️‍♀️", "🌿", "🎮", "🌟", "👾", "🦊", "👑", "🎸", "🍕", "🦾")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            emojiGrid.take(6).forEach { em ->
                                val selected = editEmoji == em
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) TealAccent else Color.DarkGray)
                                        .clickable { editEmoji = em },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(em, fontSize = 18.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            emojiGrid.drop(6).forEach { em ->
                                val selected = editEmoji == em
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) TealAccent else Color.DarkGray)
                                        .clickable { editEmoji = em },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(em, fontSize = 18.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Avatar Gradients Customizer
                        Text("Avatar Gradient Theme", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AvatarGradients.forEachIndexed { idx, brush ->
                                val selected = editGradientIndex == idx
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(brush)
                                        .border(
                                            width = if (selected) 3.dp else 0.dp,
                                            color = if (selected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { editGradientIndex = idx }
                                )
                            }
                        }
                    }
                    "Target" -> {
                        // TAB 4: TARGET & MATCH PREFERENCES
                        Text("🎯 Matchmaking Prefs & Target Criteria", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Column {
                            Text("Dating Intent Target Goal", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            val goals = listOf("💍 Serious Match", "🥂 Casual Fun", "✨ Just Friends", "🧩 Chat & See")
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                goals.take(2).forEach { goal ->
                                    val activeGoal = viewModel.activeUserGoal.collectAsStateWithLifecycle().value
                                    val isSelected = activeGoal == goal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSelected) TealAccent.copy(alpha = 0.2f) else NavyLight, RoundedCornerShape(8.dp))
                                            .border(1.dp, if (isSelected) TealAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.changeUserDatingGoal(goal) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(goal, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                goals.drop(2).forEach { goal ->
                                    val activeGoal = viewModel.activeUserGoal.collectAsStateWithLifecycle().value
                                    val isSelected = activeGoal == goal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSelected) TealAccent.copy(alpha = 0.2f) else NavyLight, RoundedCornerShape(8.dp))
                                            .border(1.dp, if (isSelected) TealAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.changeUserDatingGoal(goal) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(goal, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column {
                            Text("Interested In Gender", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Male", "Female", "Everyone").forEach { int ->
                                    val selected = editInterestedIn == int
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (selected) TealAccent else Color.DarkGray, RoundedCornerShape(8.dp))
                                            .clickable { editInterestedIn = int }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(int, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Match Search Distance Preference Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Maximum Search Distance", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("$editSearchRadius km", color = TealAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = editSearchRadius.toFloat(),
                                onValueChange = { editSearchRadius = it.roundToInt() },
                                valueRange = 10f..150f,
                                colors = SliderDefaults.colors(
                                    thumbColor = TealAccent,
                                    activeTrackColor = TealVibrant,
                                    inactiveTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Age Filter Range Preference Selector
                        Column {
                            Text("Preferred Match Age Range", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Min Age: $editMinAge", color = Color.Gray, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = { if (editMinAge > 18) editMinAge-- },
                                            modifier = Modifier.size(28.dp).background(NavyLight, CircleShape)
                                        ) {
                                            Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("$editMinAge", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        IconButton(
                                            onClick = { if (editMinAge < editMaxAge) editMinAge++ },
                                            modifier = Modifier.size(28.dp).background(NavyLight, CircleShape)
                                        ) {
                                            Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Max Age: $editMaxAge", color = Color.Gray, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = { if (editMaxAge > editMinAge) editMaxAge-- },
                                            modifier = Modifier.size(28.dp).background(NavyLight, CircleShape)
                                        ) {
                                            Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("$editMaxAge", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        IconButton(
                                            onClick = { if (editMaxAge < 80) editMaxAge++ },
                                            modifier = Modifier.size(28.dp).background(NavyLight, CircleShape)
                                        ) {
                                            Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Verified & Membership settings (Better Options)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🛡️ Verified Blue Badge", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Instant verified checkmark badge approval.", color = Color.Gray, fontSize = 9.sp)
                            }
                            Switch(
                                checked = editVerified,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                        if (hasCam) {
                                            showBuilderFaceScanDialog = true
                                            editVerified = true
                                        } else {
                                            cameraVerificationLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    } else {
                                        editVerified = false
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = TealAccent, checkedTrackColor = TealVibrant)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Membership Subscription Tier", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("None" to Color.Gray, "Premium" to TealAccent, "Premium Pro" to Color(0xFFFFD700)).forEach { (tier, color) ->
                                val selected = editTier == tier
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selected) color.copy(alpha = 0.2f) else NavyLight, RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) color else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { editTier = tier }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tier, color = if (selected) color else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SAVE & APPLY ALL PROFILE CUSTOMIZATIONS
                Button(
                    onClick = {
                        val ageInt = editAge.toIntOrNull() ?: userProfile.age
                        val latD = editLat.toDoubleOrNull() ?: userProfile.latitude
                        val lngD = editLng.toDoubleOrNull() ?: userProfile.longitude
                        
                        // Save basic and complete profile fields
                        viewModel.updateUserProfileComplete(
                            name = editName,
                            age = ageInt,
                            location = editLocation,
                            gender = editGender,
                            interestedIn = editInterestedIn,
                            bio = editBio,
                            interests = editInterests,
                            avatarEmoji = editEmoji,
                            avatarGradientIndex = editGradientIndex,
                            latitude = latD,
                            longitude = lngD,
                            premiumTier = editTier,
                            isVerified = editVerified,
                            image1 = editImg1,
                            image2 = editImg2,
                            image3 = editImg3
                        )
                        
                        // Save matchmaking filters StateFlows in ViewModel
                        viewModel.updateMatchPreferences(
                            radius = editSearchRadius,
                            minAge = editMinAge,
                            maxAge = editMaxAge
                        )
                        isExpanded = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("save_profile_complete_btn")
                ) {
                    Text("Save & Apply All Profile Customizations", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (showPhotoStudioForSlot != null) {
                    PhotoUploadStudioDialog(
                        slotNum = showPhotoStudioForSlot ?: 1,
                        currentUrl = when (showPhotoStudioForSlot) {
                            1 -> editImg1
                            2 -> editImg2
                            3 -> editImg3
                            else -> ""
                        },
                        onDismiss = { showPhotoStudioForSlot = null },
                        onPhotoSelected = { hqUrl ->
                            val slot = showPhotoStudioForSlot ?: 1
                            when (slot) {
                                1 -> editImg1 = hqUrl
                                2 -> editImg2 = hqUrl
                                3 -> editImg3 = hqUrl
                            }
                            showPhotoStudioForSlot = null
                            viewModel.showNotification("📸 Slot $slot Photo updated with premium high-quality image!")
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showBuilderFaceScanDialog) {
        VideoVerificationDialog(
            onDismiss = { showBuilderFaceScanDialog = false },
            viewModel = viewModel
        )
    }
}

// --- System Admin and Settings Screen ---

@Composable
fun SystemAdminScreen(
    viewModel: DatingViewModel,
    onOpenSupport: () -> Unit = {},
    onNavigateToChat: (Int) -> Unit = {}
) {
    val profiles by viewModel.otherProfiles.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val isCloudSynced by viewModel.isCloudSynced.collectAsStateWithLifecycle()
    val cloudSyncTimestamp by viewModel.cloudSyncTimestamp.collectAsStateWithLifecycle()
    val cloudBackupProgress by viewModel.cloudBackupProgress.collectAsStateWithLifecycle()
    val cloudServerLatency by viewModel.cloudServerLatency.collectAsStateWithLifecycle()
    val cloudServerStatus by viewModel.cloudServerStatus.collectAsStateWithLifecycle()
    val cloudDatabaseCount by viewModel.cloudDatabaseCount.collectAsStateWithLifecycle()
    val cloudSyncLog by viewModel.cloudSyncLog.collectAsStateWithLifecycle()

    var showAddCustomProfileDialog by remember { mutableStateOf(false) }
    var showCloudLogConsole by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dating System & Admin",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Scan for fake profiles using Gemini model detection, manage local SQLite parameters, and reset matching stacks.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        // Beautiful Theme Switcher Card (Dynamic Light Low Theme / Dark Theme)
        item {
            val colors = LocalAppColors.current
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (colors.isLight) colors.border else Color.Transparent, RoundedCornerShape(14.dp))
                    .testTag("theme_switcher_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleLightTheme() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (colors.isLight) Icons.Default.Star else Icons.Default.Lock,
                                contentDescription = "Theme Icon",
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Light Low-Saturation Theme",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (colors.isLight) "Active: Pastel Low-Contrast Mode" else "Tap to switch to eye-safe light colors",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = colors.isLight,
                        onCheckedChange = { viewModel.toggleLightTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.accent.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.LightGray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("theme_switch")
                    )
                }
            }
        }

        // App Privacy & Permissions Center
        item {
            PrivacyPermissionsCard(viewModel = viewModel)
        }

        // Premium Add-ons & Subscription Plans Setting Option
        item {
            PremiumAddonsCard(
                viewModel = viewModel,
                onNavigateToChat = onNavigateToChat
            )
        }

        // Active user card display
        if (userProfile != null) {
            item {
                ComprehensiveProfileBuilder(
                    viewModel = viewModel,
                    userProfile = userProfile!!
                )
            }
        }
        if (false && userProfile != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AvatarGradients[userProfile!!.avatarGradientIndex]),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(userProfile!!.avatarEmoji, fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Signed In: ${userProfile!!.name}, ${userProfile!!.age}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Bio: ${userProfile!!.bio}", color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
                            }
                        }

                        // Display the 3 compulsory uploaded photos if present
                        if (userProfile!!.image1.isNotBlank() || userProfile!!.image2.isNotBlank() || userProfile!!.image3.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = NavyLight.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Your 3 Compulsory Profile Photos", color = TealAccent, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(userProfile!!.image1, userProfile!!.image2, userProfile!!.image3).forEachIndexed { i, url ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkBackground)
                                            .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (url.isNotBlank()) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = "My Photo ${i+1}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Default.Warning, contentDescription = "Missing", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                ProfileAnalyzerForm(viewModel = viewModel)
            }

            // --- CLOUD BACKEND DATABASE & SERVER CONSOLE ---
            item {
                Text(
                    text = "Cloud Server & Database Sync",
                    color = TealAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 1. Connection Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Cloud Status",
                                tint = TealAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Secure Cloud Server Cluster", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("TLS 1.3 • AES-256 Room-to-Cloud DB Sync", color = Color.Gray, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Online/Offline status badge
                            val statusColor = when (cloudServerStatus) {
                                "ONLINE" -> TealAccent
                                "CONNECTING" -> Color.Yellow
                                else -> Color.Red
                            }
                            Box(
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Text(
                                        text = cloudServerStatus,
                                        color = statusColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = NavyLight.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Metrics Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cloud Host", color = Color.Gray, fontSize = 10.sp)
                                Text("api.safecupid.io", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Schema Version", color = Color.Gray, fontSize = 10.sp)
                                Text("v5.0.2-Room", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Endpoint Ping", color = Color.Gray, fontSize = 10.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (cloudServerLatency > 0) "${cloudServerLatency}ms" else "Not Measured",
                                        color = if (cloudServerLatency > 0) TealAccent else Color.LightGray,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                    IconButton(
                                        onClick = { viewModel.pingCloudServer() },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Ping",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cloud Record Count", color = Color.Gray, fontSize = 10.sp)
                                Text("$cloudDatabaseCount entities", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Sync Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.syncToCloud() },
                                enabled = !isCloudSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TealVibrant,
                                    disabledContainerColor = Color.DarkGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("sync_to_cloud_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Local DB", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.fetchCloudTemplates() },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyLight),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("fetch_cloud_templates_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Fetch", tint = TealAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetch Cloud Users", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 4. Progress bar if Syncing
                        if (isCloudSyncing) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cryptographic syncing in progress...", color = Color.LightGray, fontSize = 10.sp)
                                    Text("${(cloudBackupProgress * 100).toInt()}%", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                LinearProgressIndicator(
                                    progress = cloudBackupProgress,
                                    color = TealAccent,
                                    trackColor = DarkBackground,
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 5. Cloud log console toggler
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCloudLogConsole = !showCloudLogConsole }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Show SSL Handshake & Sync Transaction Logs",
                                color = if (showCloudLogConsole) TealAccent else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (showCloudLogConsole) Icons.Default.Settings else Icons.Default.PlayArrow,
                                contentDescription = "Toggle logs",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (showCloudLogConsole) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(cloudSyncLog) { log ->
                                        Text(
                                            text = log,
                                            color = if (log.contains("success", ignoreCase = true) || log.contains("synchronized", ignoreCase = true)) TealAccent else Color.LightGray,
                                            fontSize = 9.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Last synced status: $cloudSyncTimestamp",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // --- NEW PREMIUM SETTINGS & SECURITY CONTROL PANEL ---
        if (userProfile != null) {
            item {
                Text(
                    text = "Security & Privacy Center",
                    color = TealAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Toggle 1: Stealth Shield / Incognito Private Mode
                        val incognitoEnabled = userProfile!!.isIncognito
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("👤 Stealth Shield (Incognito)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Hide your profile from swipes while keeping matches.", color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = incognitoEnabled,
                                onCheckedChange = { viewModel.toggleIncognitoMode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = TealAccent, checkedTrackColor = TealVibrant)
                            )
                        }

                        Divider(color = NavyLight.copy(alpha = 0.3f))

                        // Toggle 2: PIN Lock Secure Screen Lock
                        val pinLockEnabled = userProfile!!.pinLocked
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🔒 Device Biometric PIN Lock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Require secure PIN '1234' on application startup.", color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = pinLockEnabled,
                                onCheckedChange = { viewModel.toggleUserProfilePinLock(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = TealAccent, checkedTrackColor = TealVibrant)
                            )
                        }
                    }
                }
            }

            // Interactive Vibe & Goal Customizer Grid
            item {
                Text(
                    text = "Personalize Profile Vibe",
                    color = TealAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("🎨 Current Mood Badge", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val activeMood = viewModel.activeUserMood.collectAsStateWithLifecycle().value
                        val moods = listOf("☕ Cozy Coffee", "🏔️ Wild Adventure", "🎮 Co-op Gaming", "🍕 Pizza Craze")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            moods.forEach { mood ->
                                val isSelected = activeMood == mood
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) TealVibrant else NavyLight, RoundedCornerShape(12.dp))
                                        .border(1.dp, if (isSelected) TealAccent else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.changeUserMood(mood) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(mood, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }

                        Divider(color = NavyLight.copy(alpha = 0.3f))

                        Text("🎯 Dating Intent Goal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val activeGoal = viewModel.activeUserGoal.collectAsStateWithLifecycle().value
                        val goals = listOf("💍 Serious Match", "☕ Cafe Only", "👋 New Friends")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            goals.forEach { goal ->
                                val isSelected = activeGoal == goal
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) TealVibrant else NavyLight, RoundedCornerShape(12.dp))
                                        .border(1.dp, if (isSelected) TealAccent else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.changeUserDatingGoal(goal) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(goal, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }

            // Biometric Verification trigger
            item {
                Text(
                    text = "Identity Protection Audit",
                    color = TealAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📸 Photo Verification Biometric Simulator",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Prove you match your photos to get a Verified checkmark and increase trust score.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        
                        Button(
                            onClick = { viewModel.startBiometricVerification() },
                            colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Trigger Biometric Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        BiometricVerificationPortal(viewModel = viewModel)
                    }
                }
            }

            // Safety Guides Carousel
            item {
                Text(
                    text = "Dating Safety Guides",
                    color = TealAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                SecuritySafetyGuideCarousel()
            }

            // Customer Support section
            item {
                Text("Help Desk & Support Center", color = TealAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🤖 24/7 Virtual Support Assistant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Get instant, verified answers regarding GPS Local Radar, Quantum Encrypted calls, Biometric Selfie verification, or stealth protection.", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenSupport,
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("open_support_from_settings_btn")
                        ) {
                            Text("Launch AI Chatbot Support", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Core system action buttons
        item {
            Text("Admin Controls", color = TealAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showAddCustomProfileDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                    modifier = Modifier.weight(1f).testTag("admin_add_profile_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Add User", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Profile", fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.resetAllData() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f).testTag("admin_reset_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Stack", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Stack", fontSize = 11.sp)
                }
            }
        }

        // List profiles with Fake profile audit action
        item {
            Text("All Profiles Audit Logs", color = TealAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        items(profiles) { profile ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(AvatarGradients[profile.avatarGradientIndex]),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(profile.avatarEmoji, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${profile.name} (${profile.age})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Trigger verification analysis
                        Button(
                            onClick = { viewModel.runFakeProfileDetection(profile) },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyLight),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("audit_fake_button_${profile.name}")
                        ) {
                            Text("Gemini Audit", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bio display
                    Text(
                        text = profile.bio,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Fake logs display
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VerifiedBadge(profile = profile, showText = false, iconSize = 14.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (profile.isFakeFlagged) "Flagged: Fake Account" else "Verified Active Member",
                            color = if (profile.isFakeFlagged) Color.Red else TealAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (profile.fakeAnalysis != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = profile.fakeAnalysis!!,
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue to create a new profile manually
    if (showAddCustomProfileDialog) {
        var newName by remember { mutableStateOf("") }
        var newAge by remember { mutableStateOf("25") }
        var newBio by remember { mutableStateOf("") }
        var newInterests by remember { mutableStateOf("") }
        var isFakeSim by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddCustomProfileDialog = false },
            title = { Text("Add Dating Profile", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = newAge,
                        onValueChange = { newAge = it },
                        label = { Text("Age") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = newBio,
                        onValueChange = { newBio = it },
                        label = { Text("Bio") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = newInterests,
                        onValueChange = { newInterests = it },
                        label = { Text("Interests (comma separated)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isFakeSim,
                            onCheckedChange = { isFakeSim = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                        )
                        Text("Simulate Spammer / Fake Profile", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emojiList = listOf("👩", "👨", "🧑", "👾", "🤖", "🔥")
                        viewModel.createCustomProfile(
                            Profile(
                                name = if (newName.isBlank()) "Guest" else newName,
                                age = newAge.toIntOrNull() ?: 25,
                                bio = if (newBio.isBlank()) "Adventures await us!" else newBio,
                                gender = "Everyone",
                                interestedIn = "Everyone",
                                location = "San Francisco, CA",
                                interests = if (newInterests.isBlank()) "Travel, Fun" else newInterests,
                                avatarGradientIndex = (0..5).random(),
                                avatarEmoji = emojiList.random(),
                                isVerified = !isFakeSim
                            )
                        )
                        showAddCustomProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealVibrant)
                ) {
                    Text("Insert User")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomProfileDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun PremiumAddonsCard(
    viewModel: DatingViewModel,
    onNavigateToChat: (Int) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val virtualPartnerName by viewModel.virtualPartnerName.collectAsStateWithLifecycle()
    val virtualScenario by viewModel.virtualScenario.collectAsStateWithLifecycle()
    val virtualDateMessages by viewModel.virtualDateMessages.collectAsStateWithLifecycle()
    val isVirtualDateLoading by viewModel.isVirtualDateLoading.collectAsStateWithLifecycle()

    var activePaymentPlan by remember { mutableStateOf<String?>(null) }
    val currentTier = userProfile?.premiumTier ?: "None"
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isExpanded) Color(0xFFFFD700).copy(alpha = 0.5f) else Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("premium_addons_settings_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium Add-ons",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "👑 Premium Add-ons & Plans",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isExpanded) "Tap to collapse options" else "Manage UPI plans, teleport GPS, nearby radar & virtual dates",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = NavyLight.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Neon Premium Hub Title Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFF4081),
                                        Color(0xFF00E5FF)
                                    )
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(15.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "👑 CORRECT PREMIUM HUB",
                                    color = Color(0xFFFFD700),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Unlock next-generation dating security & future simulation options.",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Tier status panel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyLight.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Membership Tier", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (currentTier) {
                                        "Premium" -> "👑 Premium (₹1/day)"
                                        "Premium Plus", "Premium Pro" -> "💎 Premium Plus (₹9/day)"
                                        else -> "Standard Account (Free)"
                                    },
                                    color = when (currentTier) {
                                        "Premium" -> Color(0xFFFFD700)
                                        "Premium Plus", "Premium Pro" -> Color(0xFF00E5FF)
                                        else -> Color.White
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (currentTier != "None") TealVibrant.copy(alpha = 0.2f) else DarkBackground,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (currentTier != "None") "ACTIVE" else "UPGRADE",
                                    color = if (currentTier != "None") TealAccent else Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Choose Your Upgrade Plan Header
                    Text(
                        text = "Choose Your Upgrade Plan",
                        color = TealAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    // Premium Club Plan Card (₹1/day)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFB300), Color(0xFFFF6F00))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { activePaymentPlan = "Premium" }
                            .padding(1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(15.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("👑 Premium Member", color = Color(0xFFFFD700), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                    Text("Standard security & matching package", color = Color.Gray, fontSize = 11.sp)
                                }
                                Text("₹1/day", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("✓ Priority Matching Boost: 5x profile visibility", color = Color.White, fontSize = 12.sp)
                            Text("✓ Dynamic Vibe customizers (Moods & Goals)", color = Color.White, fontSize = 12.sp)
                            Text("✓ Unlimited safe profile audits / integrity checks", color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { activePaymentPlan = "Premium" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (currentTier == "Premium") "Current Plan (Tap to Reactivate)" else "Sign Up for Premium (₹1/day)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Premium Plus Plan Card (₹9/day)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFFE040FB), Color(0xFFFF4081))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { activePaymentPlan = "Premium Plus" }
                            .padding(1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(15.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("💎 Premium Plus", color = Color(0xFF00E5FF), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFF4081), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("BEST VALUE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text("Full safety suite and all AI simulator powers", color = Color.Gray, fontSize = 11.sp)
                                }
                                Text("₹9/day", color = Color(0xFF00E5FF), fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("⚡ Unlocks ALL standard Premium privileges plus:", color = Color(0xFFE040FB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("✓ Device PIN startup protection lock", color = Color.White, fontSize = 12.sp)
                            Text("✓ Stealth Shield Incognito Mode visibility toggle", color = Color.White, fontSize = 12.sp)
                            Text("✓ Travel Teleport GPS companion to match globally", color = Color.White, fontSize = 12.sp)
                            Text("✓ Real-Time AI Fraud Prevention filter shield", color = Color.White, fontSize = 12.sp)
                            Text("✓ Unlimited access to AI Virtual Date Simulator", color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { activePaymentPlan = "Premium Plus" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (currentTier == "Premium Plus" || currentTier == "Premium Pro") "Active Premium Plus (Tap to Re-verify)" else "Sign Up for Premium Plus (₹9/day)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Section 2: Future Feature GPS Teleportation (Travel Companion)
                    Text(
                        text = "✈️ GPS Teleport (Travel Companion)",
                        color = TealAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyLight.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Change your location instantly to match with singles anywhere on earth! Currently matching in:",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(DarkBackground, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = "Pin", tint = TealAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(currentLocation, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (currentTier == "None") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "🔒 Unlocks with Premium membership. Subscribe to teleport now!",
                                        color = Color(0xFFFFD700),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Select a destination:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                val destinations = listOf(
                                    "Tokyo, Japan 🇯🇵",
                                    "London, UK 🇬🇧",
                                    "Paris, France 🇫🇷",
                                    "Sydney, Australia 🇦🇺",
                                    "New York, NY 🇺🇸"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    destinations.take(3).forEach { city ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (currentLocation == city) TealVibrant else DarkBackground, RoundedCornerShape(10.dp))
                                                .border(1.dp, if (currentLocation == city) TealAccent else Color.Transparent, RoundedCornerShape(10.dp))
                                                .clickable { viewModel.changeDatingLocation(city) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(city.substringBefore(" "), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    destinations.drop(3).forEach { city ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (currentLocation == city) TealVibrant else DarkBackground, RoundedCornerShape(10.dp))
                                                .border(1.dp, if (currentLocation == city) TealAccent else Color.Transparent, RoundedCornerShape(10.dp))
                                                .clickable { viewModel.changeDatingLocation(city) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(city.substringBefore(" "), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Real-Time GPS Local Radar (Find Nearby)
                    Text(
                        text = "📍 Real-Time GPS Local Radar",
                        color = TealAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    val gpsEnabled by viewModel.gpsEnabled.collectAsStateWithLifecycle()
                    val nearbyRadiusKm by viewModel.nearbyRadiusKm.collectAsStateWithLifecycle()
                    val isScanningNearby by viewModel.isScanningNearby.collectAsStateWithLifecycle()
                    val otherProfilesList by viewModel.otherProfiles.collectAsStateWithLifecycle()

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyLight.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Local Airspace Scan", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Search using high-precision GPS telemetry", color = Color.Gray, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = gpsEnabled,
                                    onCheckedChange = { viewModel.toggleGps(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TealAccent,
                                        checkedTrackColor = TealVibrant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!gpsEnabled) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = "GPS Inactive", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("GPS Satellites Disconnected. Enable GPS to scan nearby airspace.", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            } else {
                                // Radius Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Scan Radius", color = Color.LightGray, fontSize = 11.sp)
                                    Text("${nearbyRadiusKm.toInt()} km", color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = nearbyRadiusKm,
                                    onValueChange = { viewModel.setNearbyRadius(it) },
                                    valueRange = 5f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = TealAccent,
                                        activeTrackColor = TealVibrant,
                                        inactiveTrackColor = DarkBackground
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Radar Sonar Scan Animation
                                if (isScanningNearby) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
                                        val angle by infiniteTransition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(2000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "RadarAngle"
                                        )
                                        val pulseRadius by infiniteTransition.animateFloat(
                                            initialValue = 10f,
                                            targetValue = 200f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(2500, easing = EaseOutExpo),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "RadarPulse"
                                        )
                                        val pulseAlpha by infiniteTransition.animateFloat(
                                            initialValue = 0.8f,
                                            targetValue = 0f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(2500, easing = EaseOutExpo),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "RadarAlpha"
                                        )

                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val center = Offset(size.width / 2, size.height / 2)
                                            val maxRadius = Math.min(size.width, size.height) / 2

                                            // Draw concentric radar rings
                                            drawCircle(
                                                color = TealAccent.copy(alpha = 0.15f),
                                                radius = maxRadius * 0.33f,
                                                center = center,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                            )
                                            drawCircle(
                                                color = TealAccent.copy(alpha = 0.15f),
                                                radius = maxRadius * 0.66f,
                                                center = center,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                            )
                                            drawCircle(
                                                color = TealAccent.copy(alpha = 0.3f),
                                                radius = maxRadius,
                                                center = center,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                            )

                                            // Draw pulsing wave
                                            drawCircle(
                                                color = TealAccent.copy(alpha = pulseAlpha),
                                                radius = Math.min(pulseRadius, maxRadius),
                                                center = center
                                            )

                                            // Draw radar sweep line
                                            val rad = Math.toRadians(angle.toDouble())
                                            val sweepX = center.x + maxRadius * Math.cos(rad).toFloat()
                                            val sweepY = center.y + maxRadius * Math.sin(rad).toFloat()
                                            drawLine(
                                                color = TealAccent,
                                                start = center,
                                                end = Offset(sweepX, sweepY),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = TealAccent, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("📡 PINGING LOCAL GPS AIRSPACE...", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                                            Text("E2EE Telemetry Protocol active", color = Color.Gray, fontSize = 8.sp)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.triggerNearbyScan() },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Scan Local Area Nearby", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Filtered nearby profiles list
                                    val nearbyProfiles = otherProfilesList.filter { profile ->
                                        val distance = viewModel.getDistanceToProfile(profile)
                                        distance <= nearbyRadiusKm
                                    }.sortedBy { viewModel.getDistanceToProfile(it) }

                                    if (nearbyProfiles.isEmpty()) {
                                        Text(
                                            text = "No one found nearby within ${nearbyRadiusKm.toInt()} km. Expand scan radius or teleport to another city!",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "📡 Nearby Profiles Detected (${nearbyProfiles.size}):",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            nearbyProfiles.forEach { profile ->
                                                val dist = viewModel.getDistanceToProfile(profile)
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .clip(CircleShape)
                                                                    .background(AvatarGradients[profile.avatarGradientIndex]),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(profile.avatarEmoji, fontSize = 18.sp)
                                                            }

                                                            Spacer(modifier = Modifier.width(8.dp))

                                                            Column {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(profile.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                                    Text(", ${profile.age}", color = Color.LightGray, fontSize = 12.sp)
                                                                    if (profile.isVerified) {
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        VerifiedBadge(profile = profile, showText = false, iconSize = 12.dp)
                                                                    }
                                                                }
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(Icons.Default.Place, contentDescription = "Distance", tint = TealAccent, modifier = Modifier.size(10.dp))
                                                                    Spacer(modifier = Modifier.width(2.dp))
                                                                    Text(String.format("%.1f km away", dist), color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .background(TealVibrant.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    ) {
                                                                        Text("🛡️ ${profile.trustScore}% trust", color = TealAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        Button(
                                                            onClick = {
                                                                viewModel.startDirectChat(profile.id) { matchId ->
                                                                    onNavigateToChat(matchId)
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("💬 Chat Direct", color = NavyDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

                    // Section 4: AI Virtual Date Simulator
                    Text(
                        text = "🔮 AI Virtual Date Simulator",
                        color = TealAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyLight.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Practice dating by chatting with an AI virtual date partner in high-fidelity custom settings. Build your social safety confidence!",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )

                            if (currentTier != "Premium" && currentTier != "Premium Plus" && currentTier != "Premium Pro") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "🔒 Premium Exclusive. Upgrade to Premium or Premium Plus to chat with virtual dates!",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Configure Simulation Session:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Partner picker
                                Text("Partner Persona:", color = Color.Gray, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("Celeste 🤖", "Arthur 🤖", "Yuki 🤖", "Ryan 🤖").forEach { name ->
                                        val isSelected = virtualPartnerName == name
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSelected) TealVibrant else DarkBackground, RoundedCornerShape(10.dp))
                                                .clickable { viewModel.selectVirtualPartner(name, virtualScenario) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(name.substringBefore(" "), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Scenario picker
                                Text("Date Setting / Location Theme:", color = Color.Gray, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                val scenarios = listOf(
                                    "Charming Cafe in Paris ☕",
                                    "Cyberpunk Rooftop in Tokyo 👾",
                                    "Sunset Beach Malibu 🌅"
                                )
                                scenarios.forEach { setting ->
                                    val isSelected = virtualScenario == setting
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .background(if (isSelected) TealVibrant else DarkBackground, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.selectVirtualPartner(virtualPartnerName, setting) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(setting, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Chat Screen Simulator Container
                                Text(
                                    text = "Live Chat Simulation (${virtualPartnerName} in ${virtualScenario.substringBefore(" ")}):",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .border(1.dp, TealAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    if (virtualDateMessages.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(
                                                onClick = { viewModel.resetVirtualDate() },
                                                colors = ButtonDefaults.buttonColors(containerColor = TealVibrant)
                                            ) {
                                                Text("Start Simulated Session", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            // Message List Scroll
                                            Box(modifier = Modifier.weight(1f)) {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    reverseLayout = true,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(virtualDateMessages.reversed()) { msg ->
                                                        val isUser = msg.senderId == 1
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .widthIn(max = 180.dp)
                                                                    .background(
                                                                        if (isUser) TealVibrant else DarkBackground,
                                                                        RoundedCornerShape(
                                                                            topStart = 12.dp,
                                                                            topEnd = 12.dp,
                                                                            bottomStart = if (isUser) 12.dp else 0.dp,
                                                                            bottomEnd = if (isUser) 0.dp else 12.dp
                                                                        )
                                                                    )
                                                                    .padding(8.dp)
                                                            ) {
                                                                Text(msg.content, color = Color.White, fontSize = 11.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            if (isVirtualDateLoading) {
                                                Text(
                                                    text = "$virtualPartnerName is typing standard safety advice...",
                                                    color = TealAccent,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }

                                            // Input row
                                            var inputText by remember { mutableStateOf("") }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextField(
                                                    value = inputText,
                                                    onValueChange = { inputText = it },
                                                    placeholder = { Text("Type reply...", fontSize = 11.sp, color = Color.Gray) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(42.dp),
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = DarkSurface,
                                                        unfocusedContainerColor = DarkSurface,
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent
                                                    ),
                                                    shape = RoundedCornerShape(21.dp),
                                                    singleLine = true
                                                )

                                                Spacer(modifier = Modifier.width(6.dp))

                                                IconButton(
                                                    onClick = {
                                                        if (inputText.isNotBlank()) {
                                                            viewModel.sendVirtualDateMessage(inputText)
                                                            inputText = ""
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(TealVibrant, CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                                        contentDescription = "Send Message",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { viewModel.resetVirtualDate() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Reset Date Simulation", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (activePaymentPlan != null) {
        UPIPaymentDialog(
            planName = activePaymentPlan!!,
            viewModel = viewModel,
            onDismiss = { activePaymentPlan = null }
        )
    }
}

// --- Match Splash Animated Overlay ---

@Composable
fun MatchSplashOverlay(
    matchedProfile: Profile,
    userProfile: Profile?,
    onKeepSwiping: () -> Unit,
    onChatNow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "IT'S A MATCH! 🎉",
                color = TealAccent,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "You and ${matchedProfile.name} have liked each other. The right choice is correct!",
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Two overlapping/side-by-side matching avatars with a linking heart
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // User's own Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(AvatarGradients[userProfile?.avatarGradientIndex ?: 0])
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(userProfile?.avatarEmoji ?: "👩‍💻", fontSize = 48.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Connected",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Matched profile Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(AvatarGradients[matchedProfile.avatarGradientIndex])
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(matchedProfile.avatarEmoji, fontSize = 48.sp)
                }
            }

            Spacer(modifier = Modifier.height(54.dp))

            // Action options
            Button(
                onClick = onChatNow,
                colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("match_splash_chat_now"),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("Send Secure Message Now ✉️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onKeepSwiping,
                border = BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("match_splash_keep_swiping"),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("Keep Swiping", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

// ==========================================
// --- 10 NEW SECURITY & FUTURE UI OPTIONS ---
// ==========================================

// 1. PIN Lock Secure Screen Overlay
@Composable
fun PinLockOverlay(viewModel: DatingViewModel) {
    var pinEntered by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {}, // Block any background touch leaks
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SECURE PROTOCOL ACTIVE",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = "Enter your 4-digit security PIN to unlock.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Starry indicators for entered PIN
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                (1..4).forEach { i ->
                    val isFilled = pinEntered.length >= i
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) TealAccent else Color.DarkGray)
                            .border(1.5.dp, if (isFilled) TealVibrant else Color.Transparent, CircleShape)
                    )
                }
            }

            // Keypad Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        row.forEach { key ->
                            Button(
                                onClick = {
                                    if (key == "⌫") {
                                        if (pinEntered.isNotEmpty()) pinEntered = pinEntered.dropLast(1)
                                    } else if (key == "C") {
                                        pinEntered = ""
                                    } else {
                                        if (pinEntered.length < 4) {
                                            pinEntered += key
                                            if (pinEntered.length == 4) {
                                                viewModel.submitSecurityPin(pinEntered)
                                                pinEntered = ""
                                            }
                                        }
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (key == "C" || key == "⌫") NavyLight else DarkSurface
                                ),
                                border = BorderStroke(1.dp, TealVibrant.copy(alpha = 0.3f)),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (key == "C") Color.Red else if (key == "⌫") TealAccent else Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Demo hint: The default security code is '1234'",
                color = TealAccent.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// 2. Super Like Confetti Shower Spark
@Composable
fun ConfettiOverlay(triggerCount: Int) {
    if (triggerCount == 0) return

    var visible by remember(triggerCount) { mutableStateOf(true) }

    LaunchedEffect(triggerCount) {
        visible = true
        delay(3500) // particle duration
        visible = false
    }

    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {}
        ) {
            val colors = listOf(Color.Red, Color.Yellow, Color.Cyan, Color.Green, Color.Magenta, Color(0xFFFF9800), TealAccent)
            
            // Draw multiple simulated drift dots floating
            (1..24).forEach { i ->
                val startX = (i * 45) % 1080
                val speed = (1500..2800).random()
                val size = (10..24).random().dp
                val color = colors[i % colors.size]

                val infiniteTransition = rememberInfiniteTransition(label = "confetti")
                val floatY by infiniteTransition.animateFloat(
                    initialValue = -50f,
                    targetValue = 2000f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(speed, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "confettiY"
                )

                Box(
                    modifier = Modifier
                        .offset(x = startX.dp, y = floatY.dp)
                        .size(size)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

// 3. Biometric Photo Identity Verification Challenge
@Composable
fun BiometricVerificationPortal(viewModel: DatingViewModel) {
    val step by viewModel.verificationStep.collectAsStateWithLifecycle()
    if (step == 0) return

    val stepColors = listOf(TealAccent, Color.Yellow, Color.Cyan, Color(0xFFFF00CC), TealAccent)

    AlertDialog(
        onDismissRequest = { viewModel.resetBiometricChallenge() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Security Scanner", tint = TealAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Biometric Security Audit", color = Color.White)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                // Circular scanner graphic with animation
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(DarkBackground)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(listOf(DarkBackground, stepColors[step % stepColors.size], DarkBackground)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (step) {
                        1 -> Text("👁️👁️", fontSize = 48.sp)
                        2 -> Text("👤🔄", fontSize = 48.sp)
                        3 -> Text("😊✨", fontSize = 48.sp)
                        4 -> Icon(Icons.Default.Check, contentDescription = "Verified", tint = TealAccent, modifier = Modifier.size(64.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val challengeText = when (step) {
                    1 -> "CHALLENGE 1: Blink your eyes three times to verify liveness."
                    2 -> "CHALLENGE 2: Tilt your head left and then align with the screen."
                    3 -> "CHALLENGE 3: Smile and say \"FIND CORRECT\" out loud."
                    4 -> "DECRYPTING IDENTITY... Biometrics matched at 100% precision!"
                    else -> ""
                }

                Text(
                    text = challengeText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { step.toFloat() / 4f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = TealAccent,
                    trackColor = NavyLight
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step < 4) {
                        viewModel.advanceBiometricStep()
                    } else {
                        viewModel.resetBiometricChallenge()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealVibrant)
            ) {
                Text(if (step < 4) "Complete Step" else "Done")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.resetBiometricChallenge() }) {
                Text("Cancel Scan", color = Color.Gray)
            }
        },
        containerColor = DarkSurface
    )
}

// 4. On-demand AI Cryptographic Profile Integrity Scanner Report
@Composable
fun ProfileIntegrityScannerDialog(
    profile: Profile,
    viewModel: DatingViewModel,
    onDismiss: () -> Unit
) {
    val reportText by viewModel.onDemandIntegrityReport.collectAsStateWithLifecycle()
    val scanningId by viewModel.onDemandScanningId.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Shield Security", tint = TealAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Security Scan: ${profile.name}", color = Color.White)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (scanningId == profile.id) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    ) {
                        CircularProgressIndicator(color = TealAccent)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Conducting on-demand audit scan...", color = Color.White)
                    }
                } else if (reportText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = reportText!!,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TealVibrant)
            ) {
                Text("Dismiss Report")
            }
        },
        containerColor = DarkSurface
    )
}

// --- Verified Badge System ---
@Composable
fun VerifiedBadge(
    profile: Profile,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 14.dp,
    textSize: androidx.compose.ui.unit.TextUnit = 10.sp,
    showText: Boolean = true,
    interactive: Boolean = true
) {
    var showExplanationDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (profile.isVerified) TealAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (profile.isVerified) TealAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = interactive) {
                showExplanationDialog = true
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (profile.isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (profile.isVerified) "AI Verified Profile" else "Unverified Profile",
                tint = if (profile.isVerified) TealAccent else Color.LightGray,
                modifier = Modifier.size(iconSize)
            )
            if (showText) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (profile.isVerified) "AI VERIFIED" else "UNVERIFIED",
                    color = if (profile.isVerified) TealAccent else Color.LightGray,
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            icon = {
                Icon(
                    imageVector = if (profile.isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (profile.isVerified) TealAccent else Color.Red,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = if (profile.isVerified) "AI Safety Verified" else "Profile Unverified",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (profile.isVerified) {
                        Text(
                            text = "This profile has successfully passed our AI-powered safety check for authenticity. Our deep learning models analyzed facial biometrics, verified photo liveness, and audited natural conversation structures to guarantee genuine identity.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(TealAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Star, "Score", tint = TealAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Safety Integrity: ${profile.trustScore}%",
                                color = TealAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Text(
                            text = "This profile has not undergone complete AI authenticity verification yet. You can tap the 'AI Audit' shield on their card to perform an on-demand security scan.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExplanationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TealVibrant)
                ) {
                    Text("Got It")
                }
            },
            containerColor = DarkSurface
        )
    }
}

// 5. Secure Dating Educational Advisory Tip Guide Carousel
@Composable
fun SecuritySafetyGuideCarousel() {
    val guides = listOf(
        Pair("In-App Communications Only 💬", "Never jump to WhatsApp, Telegram, or Google Chat early. Fraudsters often isolate victims from in-app safety scans."),
        Pair("Scan Unverified Profiles 🔍", "Always click the AI Integrity Scan shield on any profile to examine their threat patterns and trust score."),
        Pair("Biometric Identification Check 📸", "Profiles with teal checkmarks are photo-verified. Make sure your matches are verified before sharing personal facts."),
        Pair("Meet in Highly Public Venues ☕", "Ensure your initial meeting is in a highly visible open cafe or park. Notify a buddy of your schedule.")
    )

    var currentGuide by remember { mutableStateOf(0) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🛡️ Digital Security Advisory", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = guides[currentGuide].first,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = guides[currentGuide].second,
                color = Color.LightGray,
                fontSize = 12.sp,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tip ${currentGuide + 1} of ${guides.size}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Row {
                    IconButton(
                        onClick = { currentGuide = if (currentGuide > 0) currentGuide - 1 else guides.size - 1 },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("<", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { currentGuide = (currentGuide + 1) % guides.size },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(">", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingSupportBubble(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .shadow(8.dp, CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(TealAccent, Color(0xFF007A87))
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("floating_support_bubble_btn"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = "Help Assistant",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun AISupportChatPanel(
    viewModel: DatingViewModel,
    onClose: () -> Unit
) {
    val supportChatHistory by viewModel.supportChatHistory.collectAsStateWithLifecycle()
    val isSupportLoading by viewModel.isSupportLoading.collectAsStateWithLifecycle()
    var inputMessage by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(supportChatHistory.size) {
        if (supportChatHistory.isNotEmpty()) {
            lazyListState.animateScrollToItem(supportChatHistory.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .pointerInput(Unit) {
                detectTapGestures { }
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- Support Chat Header ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(TealAccent, TealVibrant))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Support Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SafeCupid AI Support",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(TealAccent)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "24/7 Virtual Assistant",
                                    color = TealAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.clearSupportChat() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("🧹", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(32.dp)
                                .background(NavyDark, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Support Chat",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // --- Chat History Area ---
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }

                    items(supportChatHistory.size) { index ->
                        val (message, isUser) = supportChatHistory[index]
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                if (!isUser) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(NavyLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🛡️", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isUser) Brush.linearGradient(listOf(TealAccent, TealVibrant))
                                            else Brush.linearGradient(listOf(DarkSurface, DarkSurface)),
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = message,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            Text(
                                text = if (isUser) "You" else "SafeCupid AI Support",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(
                                    start = if (isUser) 0.dp else 34.dp,
                                    end = if (isUser) 8.dp else 0.dp,
                                    top = 2.dp
                                )
                            )
                        }
                    }

                    if (isSupportLoading) {
                        item {
                            Row(
                                modifier = Modifier.padding(start = 32.dp, top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = TealAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SafeCupid AI is drafting secure response...",
                                    color = TealAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                // --- Quick Suggestion Chips ---
                val suggestionChips = listOf(
                    "📡 Radar help",
                    "🔒 Encrypted calls",
                    "📸 Get Verified",
                    "👤 Stealth Shield",
                    "💖 Date Simulator"
                )
                
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestionChips.size) { index ->
                        val chipText = suggestionChips[index]
                        Box(
                            modifier = Modifier
                                .background(NavyLight, RoundedCornerShape(16.dp))
                                .border(1.dp, TealAccent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .clickable {
                                    val mappedQuery = when (chipText) {
                                        "📡 Radar help" -> "Tell me how the GPS Local Radar works and how I can find matches."
                                        "🔒 Encrypted calls" -> "How does Quantum End-to-End Encryption work for audio/video calls and what is the shred timer?"
                                        "📸 Get Verified" -> "Explain how the Biometric Selfie Verification works to get a blue checkmark and 100% trust score."
                                        "👤 Stealth Shield" -> "What is Stealth Shield and how does it make me invisible?"
                                        "💖 Date Simulator" -> "How do I start a Simulated Virtual Date to practice conversations?"
                                        else -> chipText
                                    }
                                    viewModel.sendSupportMessage(mappedQuery)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chipText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // --- Chat Input Bar ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("support_input_field"),
                            placeholder = { Text("Ask support anything...", color = Color.Gray, fontSize = 13.sp) },
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = TealAccent,
                                unfocusedBorderColor = NavyLight,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(20.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputMessage.isNotBlank()) {
                                    viewModel.sendSupportMessage(inputMessage)
                                    inputMessage = ""
                                }
                            })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputMessage.isNotBlank()) {
                                    viewModel.sendSupportMessage(inputMessage)
                                    inputMessage = ""
                                }
                            },
                            modifier = Modifier
                                .background(
                                    if (inputMessage.isNotBlank()) TealAccent else NavyDark,
                                    CircleShape
                                )
                                .size(40.dp)
                                .testTag("support_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send support question",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoUploadStudioDialog(
    slotNum: Int,
    currentUrl: String,
    onDismiss: () -> Unit,
    onPhotoSelected: (String) -> Unit,
    viewModel: DatingViewModel
) {
    var selectedUrl by remember { mutableStateOf(currentUrl) }
    var customUrlInput by remember { mutableStateOf(currentUrl) }
    var activeStudioTab by remember { mutableStateOf("Presets") } // "Presets", "Import", "GallerySim"
    var selectedCategory by remember { mutableStateOf("Masculine") } // "Masculine", "Feminine", "Lifestyle"
    
    // Gallery Simulation States
    var showGalleryProgress by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var progressPercentage by remember { mutableStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()

    val masculinePresets = listOf(
        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=800&q=80" to "Urban Traveler (HQ)",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800&q=80" to "Smiley Casual (HQ)",
        "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=800&q=80" to "Adventure Hiking (HQ)",
        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=800&q=80" to "Neon Portrait (HQ)",
        "https://images.unsplash.com/photo-1512485694743-9c9538b4e6e0?w=800&q=80" to "Coffee Shop Reading (HQ)"
    )

    val femininePresets = listOf(
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800&q=80" to "Sunkissed Smile (HQ)",
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=800&q=80" to "Fashion Portrait (HQ)",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&q=80" to "Creative Studio (HQ)",
        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&q=80" to "Scenic Outdoors (HQ)",
        "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=800&q=80" to "Aesthetic Interior (HQ)"
    )

    val lifestylePresets = listOf(
        "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=800&q=80" to "Cozy Coffee Brewing",
        "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80" to "Cyberpunk Battlestation",
        "https://images.unsplash.com/photo-1448375240586-882707db888b?w=800&q=80" to "Misty Forest Trail",
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80" to "Malibu Golden Sunset",
        "https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=800&q=80" to "Handmade Ceramic Pottery"
    )

    val simulatedGalleryFiles = listOf(
        Triple("https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=800&q=80", "DSC_8941_PORTRAIT_RAW.dng", "Sony α7R V • 85mm f/1.4 • 61MP • 14.8 MB"),
        Triple("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&q=80", "IMG_4922_RETOUCHED.tiff", "Fujifilm GFX 100S • 110mm f/2 • 102MP • 32.1 MB"),
        Triple("https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800&q=80", "CANDID_CAFE_PRO.jpeg", "Leica M11 • 50mm f/0.95 • 60MP • 8.4 MB"),
        Triple("https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=800&q=80", "OUTDOORSY_SUNSET.jpg", "Canon EOS R3 • 24-70mm f/2.8 • 24MP • 4.2 MB")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Premium Studio",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "👑 PREMIUM PHOTO STUDIO",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "High-Quality Lossless Upload & Preset Studio (Slot $slotNum)",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = NavyLight.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Studio Tabs Selection Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        "Presets" to "🌟 Studio Presets",
                        "Import" to "🔗 Custom URL",
                        "GallerySim" to "📱 Device Gallery"
                    )
                    tabs.forEach { (tabKey, label) ->
                        val isSelected = activeStudioTab == tabKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TealAccent.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) TealAccent.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { activeStudioTab = tabKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) TealAccent else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main body column
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (showGalleryProgress) {
                        // Simulated Upload Pipeline Loader
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = TealAccent,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = progressStatus,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(6.dp)
                                    .background(NavyDark, RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressPercentage)
                                        .height(6.dp)
                                        .background(
                                            Brush.horizontalGradient(listOf(TealAccent, Color(0xFFFFD700))),
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${(progressPercentage * 100).toInt()}% uploaded",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    } else {
                        when (activeStudioTab) {
                            "Presets" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Categories picker (Masculine / Feminine / Lifestyle)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Masculine" to "👨 Masculine", "Feminine" to "👩 Feminine", "Lifestyle" to "☕ Lifestyle").forEach { (catKey, label) ->
                                            val isSelected = selectedCategory == catKey
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) NavyLight else DarkBackground)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) TealAccent else Color.Transparent,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { selectedCategory = catKey }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, color = if (isSelected) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Presets Scrollable Grid
                                    val activePresets = when (selectedCategory) {
                                        "Masculine" -> masculinePresets
                                        "Feminine" -> femininePresets
                                        else -> lifestylePresets
                                    }

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(activePresets.size) { index ->
                                            val (url, title) = activePresets[index]
                                            val isSelected = selectedUrl == url
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) NavyLight else DarkBackground
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color(0xFFFFD700) else Color.Transparent
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedUrl = url }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AsyncImage(
                                                        model = url,
                                                        contentDescription = title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(50.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text("Ultra high-resolution photorealistic portrait", color = Color.Gray, fontSize = 10.sp)
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = Color(0xFFFFD700),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "Import" -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Paste or type any premium, high-resolution direct image URL (from Unsplash, Pexels, Imgur, or your custom domain):",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )

                                    OutlinedTextField(
                                        value = customUrlInput,
                                        onValueChange = {
                                            customUrlInput = it
                                            selectedUrl = it
                                        },
                                        label = { Text("High-Quality Photo URL", color = TealAccent) },
                                        placeholder = { Text("https://example.com/photo.jpg", color = Color.DarkGray) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = TealAccent,
                                            unfocusedBorderColor = Color.DarkGray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                customUrlInput = ""
                                                selectedUrl = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Clear", color = Color.Gray, fontSize = 10.sp)
                                        }

                                        Button(
                                            onClick = {
                                                selectedUrl = customUrlInput
                                                viewModel.showNotification("⚡ Preview refreshed for custom photo link!")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Refresh Photo Preview", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Quick help options
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = NavyDark.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("💡 Recommendation for Ultimate Match Visibility:", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Dating profiles using high-definition (HD, portrait ratio, clear face lighting) receive up to 10 times more high-quality direct chat requests. We support lossless JPG, PNG, WebP, and raw DNG formats.",
                                                color = Color.LightGray,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                            "GallerySim" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = "Simulating iOS / Android Native Photo Pipeline. Select a high-fidelity image file detected on this device storage:",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(simulatedGalleryFiles.size) { index ->
                                            val (url, filename, specs) = simulatedGalleryFiles[index]
                                            val isSelected = selectedUrl == url
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) NavyLight else DarkBackground
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isSelected) TealAccent else Color.Transparent
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        showGalleryProgress = true
                                                        progressStatus = "Verifying Image Integrity..."
                                                        progressPercentage = 0.1f
                                                        
                                                        coroutineScope.launch {
                                                            delay(500)
                                                            progressStatus = "Running EXIF Color Profile Scan (Rec. 2020)..."
                                                            progressPercentage = 0.4f
                                                            delay(600)
                                                            progressStatus = "Uploading lossless stream via E2EE Secure Sockets..."
                                                            progressPercentage = 0.8f
                                                            delay(700)
                                                            progressStatus = "Rendering HDR premium thumbnail..."
                                                            progressPercentage = 1.0f
                                                            delay(400)
                                                            selectedUrl = url
                                                            showGalleryProgress = false
                                                            viewModel.showNotification("🎉 File '$filename' uploaded successfully in ultra-high resolution!")
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AsyncImage(
                                                        model = url,
                                                        contentDescription = filename,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(filename, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        Text(specs, color = Color.Gray, fontSize = 9.sp)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("RAW", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Panel showing Active Selection Preview and Apply actions
                if (!showGalleryProgress) {
                    Divider(color = NavyLight.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NavyDark.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkBackground)
                                .border(1.dp, TealAccent, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedUrl.isBlank()) {
                                Icon(Icons.Default.Add, contentDescription = "Empty", tint = Color.Gray)
                            } else {
                                AsyncImage(
                                    model = selectedUrl,
                                    contentDescription = "Active Selection Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Active Studio Choice", color = Color.Gray, fontSize = 10.sp)
                            Text(
                                text = if (selectedUrl.isBlank()) "No image selected" else "High-Quality Lossless Render Selected",
                                color = if (selectedUrl.isBlank()) Color.Gray else TealAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            if (selectedUrl.isNotBlank()) {
                                Text(
                                    text = selectedUrl,
                                    color = Color.Gray,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (selectedUrl.isNotBlank()) {
                                    onPhotoSelected(selectedUrl)
                                } else {
                                    viewModel.showNotification("⚠️ Please pick or input an image URL first!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(10.dp),
                            enabled = selectedUrl.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

