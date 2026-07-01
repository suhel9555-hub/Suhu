package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Profile
import com.example.data.Match
import com.example.data.CustomReel
import com.example.viewmodel.DatingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.abs

// Song Data Structure
data class ReelSong(
    val title: String,
    val artist: String,
    val emoji: String,
    val duration: String,
    val lyrics: List<String>
)

// Helper to retrieve songs for all profiles
fun getSongForProfile(profileId: Int): ReelSong {
    val songs = listOf(
        ReelSong(
            title = "Love Me Like You Do",
            artist = "Ellie Goulding",
            emoji = "💖",
            duration = "3:58",
            lyrics = listOf(
                "You're the light, you're the night...",
                "You're the color of my blood...",
                "You're the cure, you're the pain...",
                "Only you can set my heart on fire! 🔥",
                "Yeah, I let you set the pace...",
                "Love me like you do, love-love-love me like you do..."
            )
        ),
        ReelSong(
            title = "Perfect Vibes",
            artist = "Ed Sheeran (Acoustic)",
            emoji = "🎸",
            duration = "4:23",
            lyrics = listOf(
                "I found a love for me...",
                "Darling, just dive right in and follow my lead...",
                "I found a girl, beautiful and sweet...",
                "I never knew you were the one waiting for me... 💫",
                "We were just kids when we fell in love...",
                "Baby, I'm dancing in the dark with you in my arms..."
            )
        ),
        ReelSong(
            title = "Blinding Lights",
            artist = "The Weeknd",
            emoji = "🌌",
            duration = "3:20",
            lyrics = listOf(
                "Yeah, I've been on my own for long enough...",
                "Maybe you can show me how to love, maybe...",
                "I'm going through withdrawals...",
                "You don't even have to do too much... ✨",
                "I can't sleep until I feel your touch...",
                "I said, ooh, I'm blinded by the lights!"
            )
        ),
        ReelSong(
            title = "Stay With Me",
            artist = "Justin Bieber & Kid LAROI",
            emoji = "🔥",
            duration = "2:21",
            lyrics = listOf(
                "I do the same thing I told you that I never would...",
                "I told you I'd change, even when I knew I never could...",
                "I know that I can't find nobody else as good as you...",
                "I need you to stay, need you to stay, yeah... 🥺",
                "Oh-oh-oh, stay with me...",
                "You're the only reason I believe in love again..."
            )
        ),
        ReelSong(
            title = "Flowers & Sunshine",
            artist = "Miley Cyrus (Chill Pop)",
            emoji = "🌸",
            duration = "3:11",
            lyrics = listOf(
                "We were good, we were gold...",
                "Kinda dream that can't be sold...",
                "We were right 'til we weren't...",
                "Built a home and watched it burn... 🔥",
                "I can buy myself flowers...",
                "Write my name in the sand, talk to myself for hours..."
            )
        ),
        ReelSong(
            title = "Late Night Melodies",
            artist = "Lofi Girl (Study Beats)",
            emoji = "☕",
            duration = "2:45",
            lyrics = listOf(
                "[Ambient soft Rain sounds] 🌧️",
                "[Chill Jazz Guitar intro] 🎸",
                "[Sipping warm coffee sound] ☕",
                "[Lofi beat drops softly] 🎧",
                "[Deep relaxing frequency wave] 🌌",
                "[Relax and let the vibe match your mood] ✨"
            )
        ),
        ReelSong(
            title = "Cruel Summer Romance",
            artist = "Taylor Swift (Dream Pop)",
            emoji = "💘",
            duration = "3:31",
            lyrics = listOf(
                "Fever dream high in the quiet of the night...",
                "You know that I caught it...",
                "Bad, bad boy, shiny toy with a price...",
                "You know that I bought it... 💎",
                "It's blue, the feeling I've got...",
                "And it's a cruel summer with you!"
            )
        ),
        ReelSong(
            title = "Stay Chill & Sweet",
            artist = "Sabrina Carpenter",
            emoji = "🍨",
            duration = "3:02",
            lyrics = listOf(
                "I'm espresso, sweet and bold...",
                "Walked in and everyone was staring at you...",
                "Now he's thinkin' 'bout me every night, oh...",
                "Is it that sweet? I guess so! ☕",
                "Say you can't sleep, baby, I know...",
                "That's that me espresso!"
            )
        )
    )
    return songs[abs(profileId) % songs.size]
}

@Composable
fun MusicNoteIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Draw music note
        // Oval base
        drawOval(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.55f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.3f)
        )
        // Vertical stem
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.7f),
            end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.2f),
            strokeWidth = 3f
        )
        // Flag
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.2f)
            quadraticTo(w * 0.7f, h * 0.2f, w * 0.85f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.45f)
            quadraticTo(w * 0.7f, h * 0.35f, w * 0.5f, h * 0.35f)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

@Composable
fun SpeakerIcon(isMuted: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Draw speaker body
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.25f, h * 0.35f)
            lineTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.7f, h * 0.15f)
            lineTo(w * 0.7f, h * 0.85f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.25f, h * 0.65f)
            close()
        }
        drawPath(path = path, color = tint)
        
        if (isMuted) {
            // Draw cross X
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.38f),
                end = androidx.compose.ui.geometry.Offset(w * 0.94f, h * 0.62f),
                strokeWidth = 2.5f
            )
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(w * 0.94f, h * 0.38f),
                end = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.62f),
                strokeWidth = 2.5f
            )
        } else {
            // Draw sound wave arcs
            drawArc(
                color = tint,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.25f),
                size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.5f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )
        }
    }
}

@Composable
fun ReelsScreen(viewModel: DatingViewModel, onNavigateToChat: (Int) -> Unit = {}) {
    val profiles by viewModel.otherProfiles.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val customUploadedReels by viewModel.userUploadedReels.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current

    if (profiles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = TealVibrant)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading video reels...", color = appColors.text, fontSize = 14.sp)
            }
        }
        return
    }

    var selectedInterest by remember { mutableStateOf("All") }
    var showCreatorStudio by remember { mutableStateOf(false) }
    var reelToCustomize by remember { mutableStateOf<CustomReel?>(null) }

    // Map profiles to unified CustomReel items
    val convertedReels = remember(profiles) {
        profiles.map { profile ->
            val song = getSongForProfile(profile.id)
            CustomReel(
                id = profile.id,
                authorName = profile.name,
                authorEmoji = profile.avatarEmoji,
                authorGradientIndex = profile.avatarGradientIndex,
                backgroundUrl = profile.image1,
                caption = profile.bio,
                hashtags = "#match #dating #findcorrect",
                songTitle = song.title,
                songArtist = song.artist,
                songEmoji = song.emoji,
                effectFilter = if (profile.id % 4 == 1) "Vintage Glow 🎞️" else if (profile.id % 4 == 2) "Neon Cyberpunk 🌌" else "None",
                stickerType = if (profile.id % 3 == 0) "Poll" else if (profile.id % 4 == 0) "QA" else "None",
                stickerQuestion = if (profile.id % 3 == 0) "Coffee first date? ☕" else if (profile.id % 4 == 0) "Ask me anything! ✨" else "",
                stickerOptionA = "Yes!",
                stickerOptionB = "Skip",
                isLikedByMe = false,
                likesCount = Random.nextInt(450, 2300),
                profileId = profile.id
            )
        }
    }

    // Combine standard converted reels and custom uploaded reels
    val allReels = remember(convertedReels, customUploadedReels) {
        customUploadedReels + convertedReels
    }

    // Dynamically extract all unique interest tags across all profiles
    val allInterests = remember(profiles) {
        listOf("All") + profiles
            .flatMap { it.interestList }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(6)
    }

    // Filter combined reels by the selected interest
    val filteredReels = remember(allReels, selectedInterest, profiles) {
        if (selectedInterest == "All") {
            allReels
        } else {
            allReels.filter { reel ->
                val originalProfile = profiles.find { it.id == reel.profileId }
                val matchesOriginal = originalProfile?.interestList?.any { it.equals(selectedInterest, ignoreCase = true) } == true
                val matchesHashtags = reel.hashtags.contains(selectedInterest, ignoreCase = true) || reel.caption.contains(selectedInterest, ignoreCase = true)
                matchesOriginal || matchesHashtags
            }
        }
    }

    var activeIndex by remember(selectedInterest) { mutableStateOf(1000) }

    // Swipe Guide tutorial overlay at startup
    var showSwipeGuide by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(4500)
        showSwipeGuide = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (filteredReels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "No reels matched",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No reels found for '$selectedInterest'",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try picking another interest category from the bar above.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            AnimatedContent(
                targetState = activeIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        // Swiped UP -> Enters from Bottom, exits to Top
                        slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { height -> height } + fadeIn(animationSpec = tween(400)) togetherWith
                                slideOutVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { height -> -height } + fadeOut(animationSpec = tween(400))
                    } else {
                        // Swiped DOWN -> Enters from Top, exits to Bottom
                        slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { height -> -height } + fadeIn(animationSpec = tween(400)) togetherWith
                                slideOutVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { height -> height } + fadeOut(animationSpec = tween(400))
                    }
                },
                modifier = Modifier.fillMaxSize(),
                label = "ReelTransition"
            ) { indexValue ->
                val index = (indexValue % filteredReels.size).let { if (it < 0) it + filteredReels.size else it }
                val reel = filteredReels[index]

                ReelItem(
                    reel = reel,
                    viewModel = viewModel,
                    matches = matches,
                    profiles = profiles,
                    onNext = {
                        activeIndex++
                    },
                    onPrev = {
                        activeIndex--
                    },
                    onNavigateToChat = onNavigateToChat,
                    onEditClicked = { selected ->
                        reelToCustomize = selected
                    }
                )
            }
        }

        // --- Top Overlay Scrollable Filter Chips Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Creator Studio glowing button!
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(TealVibrant, Color(0xFF8E24AA))))
                    .border(width = 1.dp, color = Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                    .clickable { showCreatorStudio = true }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Upload Reel",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Creator Studio 🎥",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            allInterests.forEach { interest ->
                val isSelected = selectedInterest == interest
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) TealVibrant.copy(alpha = 0.9f)
                            else Color.Black.copy(alpha = 0.6f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) TealAccent else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedInterest = interest }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (interest == "All") "🎯 All Reels" else "✨ $interest",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // --- Animated Swipe Guide Tutorial Overlay ---
        AnimatedVisibility(
            visible = showSwipeGuide && filteredReels.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.2.dp, TealAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "ArrowAnim")
                val arrowYOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "YOffset"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(220.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Swipe up gesture",
                        tint = TealAccent,
                        modifier = Modifier
                            .size(36.dp)
                            .offset(y = arrowYOffset.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "VERTICAL REELS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "👆 Swipe UP for Next video\n👇 Swipe DOWN for Previous\n\nDouble Tap to Like & Spark! ✨",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showSwipeGuide = false },
                        colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Start Swiping", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Active creator dialogues ---
        if (showCreatorStudio) {
            ReelsCreatorStudioDialog(
                viewModel = viewModel,
                onDismiss = { showCreatorStudio = false }
            )
        }

        if (reelToCustomize != null) {
            ReelCustomizerDialog(
                reel = reelToCustomize!!,
                onSave = { updated ->
                    if (updated.id < 0) {
                        viewModel.updateCustomReel(updated)
                    } else {
                        viewModel.showNotification("📝 Dynamic Reel options custom configured!")
                    }
                    reelToCustomize = null
                },
                onDismiss = { reelToCustomize = null }
            )
        }
    }
}

@Composable
fun ReelItem(
    reel: CustomReel,
    viewModel: DatingViewModel,
    matches: List<Match>,
    profiles: List<Profile>,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onNavigateToChat: (Int) -> Unit,
    onEditClicked: (CustomReel) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val groupChats by viewModel.groupChats.collectAsStateWithLifecycle()

    // Find or construct proxy profile fallback
    val profile = remember(profiles, reel.profileId) {
        profiles.find { it.id == reel.profileId } ?: Profile(
            id = reel.id,
            name = reel.authorName,
            age = 22,
            bio = reel.caption,
            gender = "Everyone",
            interestedIn = "Everyone",
            location = "Local Feed",
            interests = "Dating, Reels, Creator",
            avatarGradientIndex = reel.authorGradientIndex,
            avatarEmoji = reel.authorEmoji,
            isVerified = true,
            isUser = false,
            trustScore = 95,
            moodBadge = "⚡ Creator Feed",
            datingGoal = "✨ Custom Reel",
            image1 = reel.backgroundUrl,
            image2 = "",
            image3 = ""
        )
    }

    // Find any existing matches database row for this profile
    val existingMatch = remember(matches, profile.id) {
        matches.find { it.matchedUserId == profile.id || it.userId == profile.id }
    }

    // Dynamic curated playlist
    val curatedPlaylist = remember {
        List(8) { getSongForProfile(it) }
    }

    // Selected customized track for this profile video
    var selectedSong by remember(reel.id) {
        mutableStateOf(
            ReelSong(
                title = reel.songTitle,
                artist = reel.songArtist,
                emoji = reel.songEmoji,
                duration = "3:10",
                lyrics = listOf(
                    "This is the music, feel the tempo drop...",
                    "Spark connections, let the passion rock... ⚡",
                    "We're on SafeCupid, top of the tier...",
                    "Swipe and connect, love is in the air! 💖",
                    "Yeah, we're sharing dynamic lifestyle vibes!",
                    "This custom reel matches your mood tonight!"
                )
            )
        )
    }

    var isMuted by remember { mutableStateOf(false) }
    var playSpeed by remember(reel.id) { mutableStateOf(reel.playSpeed) }
    
    // Audio progress bar simulation
    var playProgress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    // Equalizer preset setting
    var equalizerPreset by remember { mutableStateOf("BASS") }

    // Heart pop pop animation on double tap
    var showHeartPop by remember { mutableStateOf(false) }

    // Comments bottom drawer state
    var showCommentsSheet by remember { mutableStateOf(false) }

    // Music & Song Hub state
    var showMusicHub by remember { mutableStateOf(false) }

    // Custom Share Floating HUD toast
    var showShareToast by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }

    // Likes count (simulated active counting)
    var localLikesCount by remember(reel.id) { mutableStateOf(reel.likesCount) }
    var isLikedByMe by remember(reel.id) { mutableStateOf(reel.isLikedByMe) }

    val filterOverlayModifier = remember(reel.effectFilter) {
        when (reel.effectFilter) {
            "Vintage Glow 🎞️" -> Modifier.background(
                Brush.radialGradient(
                    colors = listOf(Color(0x2AFFEBCD), Color(0x7F8B5A2B))
                )
            )
            "Neon Cyberpunk 🌌" -> Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0x4DFF007F), Color(0x4D00F6FF))
                )
            )
            "Warm Sunset 🌅" -> Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(Color(0x5DFF7F00), Color(0x5D7F00FF))
                )
            )
            "Lofi B&W 🖤" -> Modifier.background(
                Color(0x6D1A1A1A)
            )
            "Rainbow Vibe 🌈" -> Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0x4DFF0000), Color(0x4D00FF00), Color(0x4D0000FF))
                )
            )
            else -> Modifier
        }
    }

    // Sound Equalizer simulation
    val soundBars = remember { mutableStateListOf(0.3f, 0.5f, 0.2f, 0.6f, 0.4f, 0.8f, 0.5f, 0.3f, 0.7f, 0.2f) }

    // Dynamic double tap heartbeat animations
    val heartScale by animateFloatAsState(
        targetValue = if (showHeartPop) 1.7f else 0.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "HeartScale"
    )
    val heartYOffset by animateFloatAsState(
        targetValue = if (showHeartPop) -120f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "HeartOffset"
    )
    val heartAlpha by animateFloatAsState(
        targetValue = if (showHeartPop) 1f else 0f,
        animationSpec = tween(500),
        label = "HeartAlpha"
    )

    // Progress handler with Speed control
    LaunchedEffect(reel.id, isPaused, playSpeed) {
        if (!isPaused) {
            while (playProgress < 1.0f) {
                delay(100)
                // Advance based on speed multiplication
                playProgress += 0.008f * playSpeed
                
                // Update equalizer spectrum bars dynamically based on preset
                val baselineFactor = when (equalizerPreset) {
                    "BASS" -> 0.45f
                    "VOCAL" -> 0.2f
                    "3D" -> 0.6f
                    else -> 0.3f
                }
                for (i in soundBars.indices) {
                    if (isMuted) {
                        soundBars[i] = 0.05f
                    } else {
                        soundBars[i] = Random.nextFloat() * (1.0f - baselineFactor) + baselineFactor
                    }
                }
            }
            // Auto scroll to next video once finished!
            playProgress = 0f
            onNext()
        }
    }

    // Auto dismiss Share Toast
    LaunchedEffect(showShareToast) {
        if (showShareToast) {
            delay(2500)
            showShareToast = false
        }
    }

    // Ken burns zoom effect
    val infiniteTransition = rememberInfiniteTransition(label = "BreatheZoom")
    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KenBurns"
    )

    // Spinning disc rotation
    val rotationTransition = rememberInfiniteTransition(label = "MusicDisc")
    val discRotation by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(reel.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isLikedByMe) {
                            isLikedByMe = true
                            localLikesCount++
                            viewModel.triggerConfetti()
                            viewModel.showNotification("💖 Liked ${profile.name}'s video reel!")
                        }
                        showHeartPop = true
                        coroutineScope.launch {
                            delay(500)
                            showHeartPop = false
                        }
                    },
                    onTap = {
                        isPaused = !isPaused
                    }
                )
            }
            .pointerInput(reel.id) {
                var dragAmountY = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        dragAmountY = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAmountY += dragAmount
                    },
                    onDragEnd = {
                        if (dragAmountY < -100f) {
                            // Swiped UP -> Next
                            onNext()
                        } else if (dragAmountY > 100f) {
                            // Swiped DOWN -> Prev
                            onPrev()
                        }
                    }
                )
            }
    ) {
        // --- Full Screen Ken Burns Live Visual Canvas ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(if (isPaused) 1.0f else kenBurnsScale)
        ) {
            if (!profile.image1.isNullOrBlank()) {
                AsyncImage(
                    model = profile.image1,
                    contentDescription = "Profile Reel ${profile.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Artistic Gradient fallbacks
                val gradient = AvatarGradients[profile.avatarGradientIndex % AvatarGradients.size]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.avatarEmoji,
                                fontSize = 80.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = profile.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }

        // --- Visual Effect Filter Overlay ---
        if (reel.effectFilter != "None") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(filterOverlayModifier)
            )
        }

        // --- Dark Vignette Overlays for Perfect Legibility ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // --- Desktop/Web Navigation Control Arrows (Overlay Sidebar-styled) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onPrev() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Previous Reel",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp).rotate(270f) // points UP!
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Next Reel",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp).rotate(90f) // points DOWN!
                )
            }
        }

        // --- Live Synced Scrolling Lyrics Box Overlay ---
        val currentLyricLineIndex = (playProgress * selectedSong.lyrics.size).toInt().coerceIn(0, selectedSong.lyrics.size - 1)
        val currentLyric = selectedSong.lyrics[currentLyricLineIndex]

        AnimatedVisibility(
            visible = !isPaused && !isMuted,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { 20 }),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 185.dp, end = 90.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(0.8.dp, TealAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { showMusicHub = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MusicNoteIcon(
                        tint = TealAccent,
                        modifier = Modifier
                            .size(12.dp)
                            .rotate(if (playProgress * 100 % 2 == 0f) 15f else -15f)
                    )
                    Text(
                        text = currentLyric,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }

        // --- Left Overlaid Metadata Overlay (TikTok / Instagram reels design) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp, end = 90.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${profile.name}, ${profile.age}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (profile.isVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "SafeCupid Verified",
                        tint = TealAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TealVibrant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Trust: ${profile.trustScore}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(profile.moodBadge, color = Color.White, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(profile.datingGoal, color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = profile.bio,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Voice audio track / sound simulation (Interactive with selector launcher!)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showMusicHub = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                SpeakerIcon(
                    isMuted = isMuted,
                    tint = if (isMuted) Color.LightGray else TealAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isMuted) "Audio Muted (Tap to unmute)" else "${selectedSong.emoji} ${selectedSong.title} - ${selectedSong.artist}",
                    color = if (isMuted) Color.LightGray else TealAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Equalizer simulation
                Row(
                    modifier = Modifier.height(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    soundBars.forEach { height ->
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight(height)
                                .background(if (isMuted) Color.Gray else TealAccent)
                        )
                    }
                }
            }
        }

        // --- Right Sidebar Action Bar (Liking, Matching, Swiping, and Controls) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Like Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            isLikedByMe = !isLikedByMe
                            localLikesCount = if (isLikedByMe) localLikesCount + 1 else localLikesCount - 1
                            if (isLikedByMe) {
                                viewModel.triggerConfetti()
                                viewModel.showNotification("💖 Liked ${profile.name}'s video reel!")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like Reel",
                        tint = if (isLikedByMe) Color.Red else Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = localLikesCount.toString(),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Comments Overlay Drawer Trigger
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { showCommentsSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Comments",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "84",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3. Match / Swipe Right (Direct connect!)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val hasMatch = existingMatch != null
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (hasMatch) Color(0xFF00C853) else TealVibrant)
                        .clickable {
                            if (hasMatch) {
                                onNavigateToChat(existingMatch.id)
                            } else {
                                viewModel.swipeRight(profile)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasMatch) Icons.Default.Send else Icons.Default.Star,
                        contentDescription = if (hasMatch) "Chat Now" else "Match Now",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (hasMatch) "Chat" else "Match",
                    color = if (hasMatch) Color(0xFF00E676) else TealAccent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4. Mute / Unmute Option
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { isMuted = !isMuted },
                    contentAlignment = Alignment.Center
                ) {
                    SpeakerIcon(
                        isMuted = isMuted,
                        tint = if (isMuted) Color.LightGray else TealAccent,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isMuted) "Mute" else "Sound",
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // 5. Soundtrack Selector (Launches beautiful Vibe & Music Selector dialog!)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            showMusicHub = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Music Hub",
                        tint = TealAccent,
                        modifier = Modifier.size(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Track",
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // 6. Playback Speed Selector (Cycles: 1.0x -> 1.5x -> 2.0x)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            playSpeed = when (playSpeed) {
                                1.0f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${playSpeed}x",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Speed",
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // 7. Share Option
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { showShareMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Profile Link",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Share",
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Quick Edit / Customize Options button!
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { onEditClicked(reel) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Reel Options",
                        tint = TealAccent,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Edit 📝",
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 8. Spinning music disc (Tapping triggers active music manager hub!)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .rotate(if (isPaused) 0f else discRotation)
                    .clickable { showMusicHub = true },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(AvatarGradients[profile.avatarGradientIndex % AvatarGradients.size]),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.avatarEmoji,
                        fontSize = 8.sp
                    )
                }
            }
        }

        // --- Translucent Center Play / Pause Feedback overlay ---
        if (isPaused) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // --- Double Tap Heart Pop Feedback ---
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(heartScale)
                .offset(y = heartYOffset.dp)
                .alpha(heartAlpha)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Double Tap Heart",
                tint = Color.Red,
                modifier = Modifier.size(100.dp)
            )
        }

        // --- Floating Interactive Sticker Overlay ---
        if (reel.stickerType != "None") {
            var votedOptionA by remember(reel.id) { mutableStateOf(false) }
            var votedOptionB by remember(reel.id) { mutableStateOf(false) }
            var localVotesA by remember(reel.id) { mutableStateOf(reel.pollVotesA + Random.nextInt(12, 45)) }
            var localVotesB by remember(reel.id) { mutableStateOf(reel.pollVotesB + Random.nextInt(8, 30)) }
            
            var qaReplyText by remember(reel.id) { mutableStateOf("") }
            var qaSubmitted by remember(reel.id) { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 74.dp, bottom = 40.dp) // fits perfectly next to right sidebar!
                    .width(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.2.dp, TealAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sticker Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (reel.stickerType == "Poll") "📊 INTERACTIVE POLL" else "💬 ASK A QUESTION",
                            color = TealAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = if (reel.stickerQuestion.isBlank()) "What do you think?" else reel.stickerQuestion,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (reel.stickerType == "Poll") {
                        val totalVotes = (localVotesA + localVotesB).coerceAtLeast(1)
                        val pctA = (localVotesA * 100 / totalVotes)
                        val pctB = 100 - pctA

                        // Option A Button
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (votedOptionA) TealVibrant.copy(alpha = 0.3f)
                                        else Color.White.copy(alpha = 0.1f)
                                    )
                                    .border(
                                        1.dp,
                                        if (votedOptionA) TealAccent else Color.White.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (!votedOptionA && !votedOptionB) {
                                            votedOptionA = true
                                            localVotesA++
                                            viewModel.triggerConfetti()
                                            viewModel.showNotification("🗳️ Voted: ${reel.stickerOptionA}!")
                                        }
                                    }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(reel.stickerOptionA, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    if (votedOptionA || votedOptionB) {
                                        Text("$pctA%", color = TealAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Option B Button
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (votedOptionB) TealVibrant.copy(alpha = 0.3f)
                                        else Color.White.copy(alpha = 0.1f)
                                    )
                                    .border(
                                        1.dp,
                                        if (votedOptionB) TealAccent else Color.White.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (!votedOptionA && !votedOptionB) {
                                            votedOptionB = true
                                            localVotesB++
                                            viewModel.triggerConfetti()
                                            viewModel.showNotification("🗳️ Voted: ${reel.stickerOptionB}!")
                                        }
                                    }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(reel.stickerOptionB, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    if (votedOptionA || votedOptionB) {
                                        Text("$pctB%", color = TealAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Q&A
                        if (qaSubmitted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(TealVibrant.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Reply Sent Safely! 🛡️", color = TealAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = qaReplyText,
                                    onValueChange = { qaReplyText = it },
                                    placeholder = { Text("Reply...", color = Color.Gray, fontSize = 10.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealAccent,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        if (qaReplyText.isNotBlank()) {
                                            qaSubmitted = true
                                            viewModel.showNotification("🛡️ Answer successfully submitted to Creator!")
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(TealVibrant, CircleShape)
                                ) {
                                    Icon(Icons.Default.Send, "Send", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Timeline audio/video progress bar ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.2f))
                .align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(playProgress)
                    .fillMaxHeight()
                    .background(TealAccent)
            )
        }

        // --- Interactive Safety Comments Sliding Sheet ---
        if (showCommentsSheet) {
            CommentsDialog(
                profile = profile,
                onDismiss = { showCommentsSheet = false }
            )
        }

        // --- Interactive Song Selector & Audio Hub Dialog ---
        if (showMusicHub) {
            MusicHubDialog(
                currentSong = selectedSong,
                allSongs = curatedPlaylist,
                onSongSelected = { song -> selectedSong = song },
                activeProgress = playProgress,
                onProgressChanged = { newProgress -> playProgress = newProgress },
                isMuted = isMuted,
                onMuteToggle = { isMuted = !isMuted },
                playSpeed = playSpeed,
                onSpeedToggle = {
                    playSpeed = when (playSpeed) {
                        1.0f -> 1.5f
                        1.5f -> 2.0f
                        else -> 1.0f
                    }
                },
                equalizerPreset = equalizerPreset,
                onEqualizerPresetChanged = { preset -> equalizerPreset = preset },
                onDismiss = { showMusicHub = false }
            )
        }

        // --- High Fidelity Top Share toast overlay notification ---
        AnimatedVisibility(
            visible = showShareToast,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 80.dp, start = 24.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDF09141A))
                    .border(1.dp, TealAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Copied",
                        tint = TealAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Secure link copied for ${profile.name}! 🛡️",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Share Reel to Match or Group Dialog ---
        if (showShareMenu) {
            AlertDialog(
                onDismissRequest = { showShareMenu = false },
                title = {
                    Text(
                        text = "Share Reel to Friends & Groups",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Current Reel Info card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavyDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(TealAccent.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(selectedSong.emoji, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedSong.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = selectedSong.artist,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Section 1: Matches / DMs
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, "DMs", tint = TealAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Direct Matches", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            if (matches.isEmpty()) {
                                Text("No connections yet. Go swipe!", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(matches) { match ->
                                        val friend = profiles.find { it.id == match.matchedUserId || it.id == match.userId }
                                        if (friend != null) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .clickable {
                                                        viewModel.shareReelToMatch(
                                                            matchId = match.id,
                                                            reelTitle = selectedSong.title,
                                                            reelArtist = selectedSong.artist,
                                                            reelEmoji = selectedSong.emoji
                                                        )
                                                        showShareMenu = false
                                                        viewModel.showNotification("🎬 Shared Reel with ${friend.name}!")
                                                    }
                                                    .width(64.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(AvatarGradients[friend.avatarGradientIndex % AvatarGradients.size]),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(friend.avatarEmoji, fontSize = 22.sp)
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
                            }
                        }

                        // Section 2: Group Chats
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, "Groups", tint = TealAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Message Groups", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            if (groupChats.isEmpty()) {
                                Text("No message groups found.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(groupChats) { group ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.shareReelToGroup(
                                                        groupId = group.id,
                                                        reelTitle = selectedSong.title,
                                                        reelArtist = selectedSong.artist,
                                                        reelEmoji = selectedSong.emoji
                                                    )
                                                    showShareMenu = false
                                                    viewModel.showNotification("🎬 Shared Reel in ${group.name}!")
                                                }
                                                .width(64.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(AvatarGradients[group.avatarGradientIndex % AvatarGradients.size]),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(group.avatarEmoji, fontSize = 22.sp)
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
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showShareMenu = false }) {
                        Text("Close", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// Utility extension function to get a fallback icon
fun Int.getRotatedIcon(): Int = this

@Composable
fun CommentsDialog(profile: Profile, onDismiss: () -> Unit) {
    val commentsList = remember(profile.id) {
        mutableStateListOf(
            Triple("Aria_Dating_Coach", "This voice intro sounds so incredibly genuine! Big green flag! 💚", "👩‍💼"),
            Triple("Security_Checkbot", "SafeCupid cryptographic checksum validated. Profile has 100% integrity score. 🛡️", "🤖"),
            Triple("CupidArrow", "Your vibe matches perfectly! You both listed '${profile.interestList.firstOrNull() ?: "Hiking"}' as top priority! ✨", "💘"),
            Triple("LocalExplorer", "Highly recommend connecting! Super responsive in chat. 🗺️", "🌟"),
            Triple("MatchMaker_AI", "Highly recommended compatible alignment match! Send a friendly prompt! 🚀", "🔮")
        )
    }

    var userCommentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comments & Safety Insights",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(commentsList) { (author, text, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyDark.copy(alpha = 0.3f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(icon, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = author,
                                    fontWeight = FontWeight.Bold,
                                    color = TealAccent,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = text,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Custom Active Comments Poster
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = userCommentText,
                        onValueChange = { userCommentText = it },
                        placeholder = { Text("Add safety insight / comment...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                            focusedContainerColor = NavyDark.copy(alpha = 0.4f),
                            unfocusedContainerColor = NavyDark.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (userCommentText.isNotBlank()) {
                                commentsList.add(Triple("You (Verified User)", userCommentText, "👤"))
                                userCommentText = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(TealVibrant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Post Comment",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun MusicHubDialog(
    currentSong: ReelSong,
    allSongs: List<ReelSong>,
    onSongSelected: (ReelSong) -> Unit,
    activeProgress: Float,
    onProgressChanged: (Float) -> Unit,
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    playSpeed: Float,
    onSpeedToggle: () -> Unit,
    equalizerPreset: String,
    onEqualizerPresetChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎵", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vibe & Music Center",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active Song Showcase
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyDark.copy(alpha = 0.5f))
                        .border(1.dp, TealAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Big emoji vinyl disc art
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(currentSong.emoji, fontSize = 24.sp)
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSong.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong.artist,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Simulated audio spectrum lines
                        Row(
                            modifier = Modifier.height(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            repeat(6) { index ->
                                val waveHeight = remember { mutableStateOf(0.2f) }
                                LaunchedEffect(key1 = activeProgress) {
                                    waveHeight.value = if (isMuted) 0.05f else (0.2f + Random.nextFloat() * 0.8f)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .fillMaxHeight(waveHeight.value)
                                        .background(TealAccent)
                                )
                            }
                        }
                    }
                }

                // Interactive Scrubbing Control
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simulated Audio Timeline", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(currentSong.duration, color = Color.Gray, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = activeProgress,
                        onValueChange = onProgressChanged,
                        colors = SliderDefaults.colors(
                            thumbColor = TealAccent,
                            activeTrackColor = TealAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick Soundtrack Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Toggle Mute
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMuted) Color.Red.copy(alpha = 0.15f) else NavyDark.copy(alpha = 0.3f))
                            .border(
                                1.dp,
                                if (isMuted) Color.Red.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onMuteToggle() }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SpeakerIcon(isMuted = isMuted, tint = if (isMuted) Color.Red else TealAccent, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isMuted) "Unmute Audio" else "Mute Sound",
                                color = if (isMuted) Color.Red else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Toggle Playback Speed
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavyDark.copy(alpha = 0.3f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .clickable { onSpeedToggle() }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Speed", tint = TealAccent, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Speed: ${playSpeed}x",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Equalizer Sound FX Presets
                Column {
                    Text("Sound Profile Presets", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Bass Boosted 🔥" to "BASS",
                            "Vocal Clarity 🎙️" to "VOCAL",
                            "3D Concert Hall 🎧" to "3D",
                            "Lofi Acoustic ☕" to "LOFI"
                        ).forEach { (label, preset) ->
                            val isSelected = equalizerPreset == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) TealVibrant.copy(alpha = 0.9f)
                                        else NavyDark.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) TealAccent else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onEqualizerPresetChanged(preset) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Directory of All Available Songs
                Column {
                    Text("Select Background Music Soundtrack", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allSongs.forEach { song ->
                            val isCurrent = song.title == currentSong.title
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isCurrent) TealVibrant.copy(alpha = 0.15f)
                                        else NavyDark.copy(alpha = 0.2f)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = if (isCurrent) TealAccent else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSongSelected(song) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(song.emoji, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrent) TealAccent else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = song.artist,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Playing",
                                        tint = TealAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ReelsCreatorStudioDialog(
    viewModel: DatingViewModel,
    onDismiss: () -> Unit
) {
    val customUploadedReels by viewModel.userUploadedReels.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) } // 0: Publish, 1: My Uploads
    
    // Publish states
    var captionText by remember { mutableStateOf("") }
    var hashtagText by remember { mutableStateOf("#vibe #dating #lifestyle") }
    var backgroundPresetIndex by remember { mutableStateOf(0) }
    var customBackgroundUrl by remember { mutableStateOf("") }
    
    val bgPresets = listOf(
        "☕ Cozy Cafe Vibe" to "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?q=80&w=600&auto=format&fit=crop",
        "🌅 Warm Sunset Beach" to "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=600&auto=format&fit=crop",
        "🎸 Indie Concert Stage" to "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=600&auto=format&fit=crop",
        "🏋️‍♀️ Gym & Fitness Motivation" to "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?q=80&w=600&auto=format&fit=crop",
        "🌌 Cyberpunk Tokyo Drive" to "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?q=80&w=600&auto=format&fit=crop"
    )

    val allSongs = remember { List(8) { getSongForProfile(it) } }
    var selectedSongIndex by remember { mutableStateOf(0) }
    var playSpeed by remember { mutableStateOf(1.0f) }
    var selectedFilter by remember { mutableStateOf("None") }
    
    val filterOptions = listOf("None", "Vintage Glow 🎞️", "Neon Cyberpunk 🌌", "Warm Sunset 🌅", "Lofi B&W 🖤", "Rainbow Vibe 🌈")
    
    // Sticker states
    var stickerType by remember { mutableStateOf("None") } // "None", "Poll", "QA"
    var stickerQuestion by remember { mutableStateOf("") }
    var pollOptionA by remember { mutableStateOf("Yes! 👍") }
    var pollOptionB by remember { mutableStateOf("No 👎") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎥 Reels Creator Studio", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = TealAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = TealAccent
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Publish Reel 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) TealAccent else Color.Gray) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("My Uploads (${customUploadedReels.size}) 🎬", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) TealAccent else Color.Gray) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (selectedTab == 0) {
                    // Publish tab
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Background Preset Picker
                        Column {
                            Text("1. Background Video/Vibe", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                bgPresets.forEachIndexed { idx, (label, _) ->
                                    val isSel = backgroundPresetIndex == idx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) TealVibrant else NavyDark.copy(alpha = 0.5f))
                                            .border(1.dp, if (isSel) TealAccent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .clickable { backgroundPresetIndex = idx }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Background url overrides
                        OutlinedTextField(
                            value = customBackgroundUrl,
                            onValueChange = { customBackgroundUrl = it },
                            label = { Text("Or paste custom Image/Video URL", color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealAccent,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                focusedContainerColor = NavyDark.copy(alpha = 0.2f)
                            )
                        )

                        // Soundtrack picker
                        Column {
                            Text("2. Choose Background Music Soundtrack", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allSongs.forEachIndexed { idx, song ->
                                    val isSel = selectedSongIndex == idx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) TealVibrant else NavyDark.copy(alpha = 0.5f))
                                            .border(1.dp, if (isSel) TealAccent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .clickable { selectedSongIndex = idx }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("${song.emoji} ${song.title}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Playback Speed
                        Column {
                            Text("3. Default Playback Speed", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(1.0f, 1.5f, 2.0f).forEach { speed ->
                                    val isSel = playSpeed == speed
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) TealVibrant else NavyDark.copy(alpha = 0.4f))
                                            .clickable { playSpeed = speed }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${speed}x", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Filter Option selector
                        Column {
                            Text("4. Visual FX Filter Overlay", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filterOptions.forEach { filter ->
                                    val isSel = selectedFilter == filter
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) TealVibrant else NavyDark.copy(alpha = 0.5f))
                                            .border(1.dp, if (isSel) TealAccent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .clickable { selectedFilter = filter }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(filter, color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Sticker Setting
                        Column {
                            Text("5. Interactive Engagement Sticker", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("None", "Poll", "QA").forEach { type ->
                                    val isSel = stickerType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) TealVibrant else NavyDark.copy(alpha = 0.4f))
                                            .clickable { stickerType = type }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(type, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            if (stickerType != "None") {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = stickerQuestion,
                                    onValueChange = { stickerQuestion = it },
                                    label = { Text("Sticker Question / Prompt", color = Color.Gray, fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealAccent,
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                        focusedContainerColor = NavyDark.copy(alpha = 0.2f)
                                    )
                                )
                                
                                if (stickerType == "Poll") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = pollOptionA,
                                            onValueChange = { pollOptionA = it },
                                            label = { Text("Option A", color = Color.Gray, fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f),
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp)
                                        )
                                        OutlinedTextField(
                                            value = pollOptionB,
                                            onValueChange = { pollOptionB = it },
                                            label = { Text("Option B", color = Color.Gray, fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f),
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp)
                                        )
                                    }
                                }
                            }
                        }

                        // Caption & Hashtags
                        OutlinedTextField(
                            value = captionText,
                            onValueChange = { captionText = it },
                            label = { Text("Caption (e.g., Chill Sunday afternoons... 🍃)", color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                        )

                        OutlinedTextField(
                            value = hashtagText,
                            onValueChange = { hashtagText = it },
                            label = { Text("Hashtags (e.g., #dating #music #fun)", color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val bg = if (customBackgroundUrl.isNotBlank()) customBackgroundUrl else bgPresets[backgroundPresetIndex].second
                                val song = allSongs[selectedSongIndex]
                                val newReel = CustomReel(
                                    id = -Random.nextInt(1000, 99999), // unique negative ID for custom uploads
                                    authorName = "You (Verified User)",
                                    authorEmoji = "👤",
                                    authorGradientIndex = 4,
                                    backgroundUrl = bg,
                                    caption = if (captionText.isBlank()) "Enjoying the vibe!" else captionText,
                                    hashtags = hashtagText,
                                    songTitle = song.title,
                                    songArtist = song.artist,
                                    songEmoji = song.emoji,
                                    effectFilter = selectedFilter,
                                    playSpeed = playSpeed,
                                    stickerType = stickerType,
                                    stickerQuestion = stickerQuestion,
                                    stickerOptionA = pollOptionA,
                                    stickerOptionB = pollOptionB,
                                    likesCount = 0
                                )
                                viewModel.uploadCustomReel(newReel)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealVibrant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🚀 Publish Live Reel Feed", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // My Uploads list
                    if (customUploadedReels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PlayArrow, "No uploads", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No custom uploads yet.", color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Go to the Publish tab to upload your first reel!", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(customUploadedReels) { reel ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = NavyDark.copy(alpha = 0.5f)),
                                    border = BorderStroke(0.5.dp, TealAccent.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Preview / Song Emoji
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(TealAccent.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(reel.songEmoji, fontSize = 20.sp)
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(reel.caption, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Track: ${reel.songTitle} • FX: ${reel.effectFilter}", color = Color.Gray, fontSize = 10.sp)
                                        }
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        // Edit Button
                                        Button(
                                            onClick = {
                                                // Prepopulate editor inputs with current reel values
                                                captionText = reel.caption
                                                hashtagText = reel.hashtags
                                                customBackgroundUrl = reel.backgroundUrl
                                                selectedFilter = reel.effectFilter
                                                playSpeed = reel.playSpeed
                                                stickerType = reel.stickerType
                                                stickerQuestion = reel.stickerQuestion
                                                pollOptionA = reel.stickerOptionA
                                                pollOptionB = reel.stickerOptionB
                                                selectedTab = 0
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent.copy(alpha = 0.2f)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Edit 📝", color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ReelCustomizerDialog(
    reel: CustomReel,
    onSave: (CustomReel) -> Unit,
    onDismiss: () -> Unit
) {
    var captionText by remember { mutableStateOf(reel.caption) }
    var hashtagText by remember { mutableStateOf(reel.hashtags) }
    var selectedFilter by remember { mutableStateOf(reel.effectFilter) }
    var playSpeed by remember { mutableStateOf(reel.playSpeed) }
    var stickerType by remember { mutableStateOf(reel.stickerType) }
    var stickerQuestion by remember { mutableStateOf(reel.stickerQuestion) }
    var pollOptionA by remember { mutableStateOf(reel.stickerOptionA) }
    var pollOptionB by remember { mutableStateOf(reel.stickerOptionB) }
    
    val filterOptions = listOf("None", "Vintage Glow 🎞️", "Neon Cyberpunk 🌌", "Warm Sunset 🌅", "Lofi B&W 🖤", "Rainbow Vibe 🌈")
    val allSongs = remember { List(8) { getSongForProfile(it) } }
    var selectedSongTitle by remember { mutableStateOf(reel.songTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📝 Edit Reel Options", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Customize the metadata, visual filters, engagement widgets, and audio timeline for this active reel feed.", color = Color.Gray, fontSize = 11.sp)

                // Caption & Hashtags
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("Reel Caption / Bio", color = Color.Gray, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = hashtagText,
                    onValueChange = { hashtagText = it },
                    label = { Text("Hashtags", color = Color.Gray, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )

                // Visual filter
                Column {
                    Text("Applied Visual FX Filter", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterOptions.forEach { filter ->
                            val isSel = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) TealVibrant else NavyDark.copy(alpha = 0.5f))
                                    .border(1.dp, if (isSel) TealAccent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(filter, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Soundtrack Picker
                Column {
                    Text("Select Background Music", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allSongs.forEach { song ->
                            val isSel = selectedSongTitle == song.title
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) TealVibrant else NavyDark.copy(alpha = 0.5f))
                                    .border(1.dp, if (isSel) TealAccent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { selectedSongTitle = song.title }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("${song.emoji} ${song.title}", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Playback Speed
                Column {
                    Text("Playback Speed", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(1.0f, 1.5f, 2.0f).forEach { speed ->
                            val isSel = playSpeed == speed
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) TealVibrant else NavyDark.copy(alpha = 0.4f))
                                    .clickable { playSpeed = speed }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${speed}x", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Engagement Sticker
                Column {
                    Text("Interactive Engagement Sticker", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("None", "Poll", "QA").forEach { type ->
                            val isSel = stickerType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) TealVibrant else NavyDark.copy(alpha = 0.4f))
                                    .clickable { stickerType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    if (stickerType != "None") {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = stickerQuestion,
                            onValueChange = { stickerQuestion = it },
                            label = { Text("Question / Prompt Text", color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                        )
                        
                        if (stickerType == "Poll") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = pollOptionA,
                                    onValueChange = { pollOptionA = it },
                                    label = { Text("Option A", color = Color.Gray, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp)
                                )
                                OutlinedTextField(
                                    value = pollOptionB,
                                    onValueChange = { pollOptionB = it },
                                    label = { Text("Option B", color = Color.Gray, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val matchingSong = allSongs.find { it.title == selectedSongTitle } ?: allSongs.first()
                    onSave(
                        reel.copy(
                            caption = captionText,
                            hashtags = hashtagText,
                            effectFilter = selectedFilter,
                            playSpeed = playSpeed,
                            songTitle = matchingSong.title,
                            songArtist = matchingSong.artist,
                            songEmoji = matchingSong.emoji,
                            stickerType = stickerType,
                            stickerQuestion = stickerQuestion,
                            stickerOptionA = pollOptionA,
                            stickerOptionB = pollOptionB
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealVibrant)
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
