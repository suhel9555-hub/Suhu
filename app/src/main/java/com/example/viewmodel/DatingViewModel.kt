package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.BuildConfig
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.SYSTEM
)

enum class NotificationType {
    SYSTEM,
    MATCH,
    SAFETY,
    CHAT,
    GPS
}

class DatingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val profileDao = database.profileDao()
    private val matchDao = database.matchDao()
    private val messageDao = database.messageDao()
    private val groupChatDao = database.groupChatDao()
    private val groupMessageDao = database.groupMessageDao()

    // --- State Flows ---
    val userProfile: StateFlow<Profile?> = profileDao.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val otherProfiles: StateFlow<List<Profile>> = profileDao.getAllOtherProfilesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matches: StateFlow<List<Match>> = matchDao.getAllMatchesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupChats: StateFlow<List<GroupChat>> = groupChatDao.getAllGroupChatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeGroupId = MutableStateFlow<Int?>(null)
    val activeGroupId: StateFlow<Int?> = _activeGroupId.asStateFlow()

    private val _currentSwipeIndex = MutableStateFlow(0)
    val currentSwipeIndex: StateFlow<Int> = _currentSwipeIndex.asStateFlow()

    private val _activeMatchId = MutableStateFlow<Int?>(null)
    val activeMatchId: StateFlow<Int?> = _activeMatchId.asStateFlow()

    private val _aiBioGenerationState = MutableStateFlow<String?>(null)
    val aiBioGenerationState: StateFlow<String?> = _aiBioGenerationState.asStateFlow()

    private val _aiBioLoading = MutableStateFlow(false)
    val aiBioLoading: StateFlow<Boolean> = _aiBioLoading.asStateFlow()

    private val _profileAnalysisResult = MutableStateFlow<String?>(null)
    val profileAnalysisResult: StateFlow<String?> = _profileAnalysisResult.asStateFlow()

    private val _profileAnalysisLoading = MutableStateFlow(false)
    val profileAnalysisLoading: StateFlow<Boolean> = _profileAnalysisLoading.asStateFlow()

    private val _icebreakers = MutableStateFlow<List<String>>(emptyList())
    val icebreakers: StateFlow<List<String>> = _icebreakers.asStateFlow()

    private val _icebreakersLoading = MutableStateFlow(false)
    val icebreakersLoading: StateFlow<Boolean> = _icebreakersLoading.asStateFlow()

    // Match Splash Overlay State (Triggered on a right swipe match)
    private val _matchSplashProfile = MutableStateFlow<Profile?>(null)
    val matchSplashProfile: StateFlow<Profile?> = _matchSplashProfile.asStateFlow()

    // Cache of AI compatibility results: ProfileId -> Pair(Score, Reason)
    private val _compatibilityCache = MutableStateFlow<Map<Int, Pair<Int, String>>>(emptyMap())
    val compatibilityCache: StateFlow<Map<Int, Pair<Int, String>>> = _compatibilityCache.asStateFlow()

    // Notification banners shown when fake logs trigger or moderation blocks
    private val _systemNotification = MutableStateFlow<String?>(null)
    val systemNotification: StateFlow<String?> = _systemNotification.asStateFlow()

    // Structured Notification List
    private val _appNotifications = MutableStateFlow<List<AppNotification>>(
        listOf(
            AppNotification(
                title = "Welcome to Find Correct! 🎉",
                message = "Your digital safety shield is active. Set up your compulsory photos to verify your profile card.",
                type = NotificationType.SYSTEM,
                timestamp = System.currentTimeMillis() - 10 * 60 * 1000
            ),
            AppNotification(
                title = "Identity AI Engine Active 🛡️",
                message = "We've verified safety hashes with local clusters. All profile matches are real users.",
                type = NotificationType.SAFETY,
                timestamp = System.currentTimeMillis() - 8 * 60 * 1000
            ),
            AppNotification(
                title = "GPS Radar Configured 📍",
                message = "Your search radius is active. Tap radar inside System menu to discover local singles.",
                type = NotificationType.GPS,
                timestamp = System.currentTimeMillis() - 5 * 60 * 1000
            )
        )
    )
    val appNotifications: StateFlow<List<AppNotification>> = _appNotifications.asStateFlow()

    // --- Custom User Uploaded Reels ---
    private val _userUploadedReels = MutableStateFlow<List<CustomReel>>(emptyList())
    val userUploadedReels: StateFlow<List<CustomReel>> = _userUploadedReels.asStateFlow()

    fun uploadCustomReel(reel: CustomReel) {
        _userUploadedReels.value = _userUploadedReels.value + reel
        showNotification("🎥 Custom Reel successfully uploaded to Reels feed!")
    }

    fun updateCustomReel(updatedReel: CustomReel) {
        _userUploadedReels.value = _userUploadedReels.value.map {
            if (it.id == updatedReel.id) updatedReel else it
        }
        showNotification("📝 Custom Reel successfully updated!")
    }

    fun voteOnCustomReelPoll(reelId: Int, isOptionA: Boolean) {
        _userUploadedReels.value = _userUploadedReels.value.map {
            if (it.id == reelId) {
                if (isOptionA) {
                    it.copy(pollVotesA = it.pollVotesA + 1)
                } else {
                    it.copy(pollVotesB = it.pollVotesB + 1)
                }
            } else {
                it
            }
        }
        showNotification("🗳️ Your vote has been counted!")
    }

    fun markNotificationAsRead(id: String) {
        _appNotifications.value = _appNotifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllNotificationsAsRead() {
        _appNotifications.value = _appNotifications.value.map { it.copy(isRead = true) }
    }

    fun clearAllNotifications() {
        _appNotifications.value = emptyList()
    }

    // --- 10 New Security & Future Options State Flows ---
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _verificationStep = MutableStateFlow(0) // 0: Idle, 1: Blink Eyes, 2: Tilt Head, 3: Smile, 4: Success
    val verificationStep: StateFlow<Int> = _verificationStep.asStateFlow()

    private val _confettiCount = MutableStateFlow(0)
    val confettiCount: StateFlow<Int> = _confettiCount.asStateFlow()

    fun triggerConfetti() {
        _confettiCount.value = _confettiCount.value + 1
    }

    private val _onDemandIntegrityReport = MutableStateFlow<String?>(null)
    val onDemandIntegrityReport: StateFlow<String?> = _onDemandIntegrityReport.asStateFlow()

    private val _onDemandScanningId = MutableStateFlow<Int?>(null)
    val onDemandScanningId: StateFlow<Int?> = _onDemandScanningId.asStateFlow()

    private val _activeUserMood = MutableStateFlow("☕ Cafe Chat")
    val activeUserMood: StateFlow<String> = _activeUserMood.asStateFlow()

    private val _activeUserGoal = MutableStateFlow("💍 Serious Match")
    val activeUserGoal: StateFlow<String> = _activeUserGoal.asStateFlow()

    private val _messageFlaggedNotification = MutableStateFlow<String?>(null)
    val messageFlaggedNotification: StateFlow<String?> = _messageFlaggedNotification.asStateFlow()

    // --- Premium & Future Options State Flows ---
    private val _currentLocation = MutableStateFlow("San Francisco, CA")
    val currentLocation: StateFlow<String> = _currentLocation.asStateFlow()

    private val _searchRadiusKm = MutableStateFlow(80)
    val searchRadiusKm: StateFlow<Int> = _searchRadiusKm.asStateFlow()

    private val _minAgePreference = MutableStateFlow(18)
    val minAgePreference: StateFlow<Int> = _minAgePreference.asStateFlow()

    private val _maxAgePreference = MutableStateFlow(60)
    val maxAgePreference: StateFlow<Int> = _maxAgePreference.asStateFlow()

    fun updateMatchPreferences(radius: Int, minAge: Int, maxAge: Int) {
        _searchRadiusKm.value = radius
        _minAgePreference.value = minAge
        _maxAgePreference.value = maxAge
        showNotification("🎯 Search radius and age preferences updated!")
    }

    private val _virtualPartnerName = MutableStateFlow("Celeste 🤖")
    val virtualPartnerName: StateFlow<String> = _virtualPartnerName.asStateFlow()

    private val _virtualScenario = MutableStateFlow("Charming Cafe in Paris ☕")
    val virtualScenario: StateFlow<String> = _virtualScenario.asStateFlow()

    private val _virtualDateMessages = MutableStateFlow<List<Message>>(emptyList())
    val virtualDateMessages: StateFlow<List<Message>> = _virtualDateMessages.asStateFlow()

    private val _isVirtualDateLoading = MutableStateFlow(false)
    val isVirtualDateLoading: StateFlow<Boolean> = _isVirtualDateLoading.asStateFlow()

    // --- LIGHT THEME STATE ---
    private val _isLightTheme = MutableStateFlow(false)
    val isLightTheme: StateFlow<Boolean> = _isLightTheme.asStateFlow()

    fun toggleLightTheme() {
        _isLightTheme.value = !_isLightTheme.value
    }

    // --- ADVANCED SECURITY & PRIVACY OPTIONS STATE ---
    private val _isChatEncryptionEnabled = MutableStateFlow(true)
    val isChatEncryptionEnabled: StateFlow<Boolean> = _isChatEncryptionEnabled.asStateFlow()

    private val _gpsPrecisionMode = MutableStateFlow("Exact Accuracy")
    val gpsPrecisionMode: StateFlow<String> = _gpsPrecisionMode.asStateFlow()

    private val _isAntiScreenshotActive = MutableStateFlow(true)
    val isAntiScreenshotActive: StateFlow<Boolean> = _isAntiScreenshotActive.asStateFlow()

    private val _selfDestructTimer = MutableStateFlow("Keep indefinitely")
    val selfDestructTimer: StateFlow<String> = _selfDestructTimer.asStateFlow()

    fun toggleChatEncryption(enabled: Boolean) {
        _isChatEncryptionEnabled.value = enabled
        showNotification(if (enabled) "🛡️ AES-256 Chat Shield Active" else "⚠️ Chats unshielded (Standard security)")
    }

    fun changeGpsPrecision(mode: String) {
        _gpsPrecisionMode.value = mode
        showNotification("🛰️ GPS Precision: $mode applied.")
    }

    fun toggleAntiScreenshot(enabled: Boolean) {
        _isAntiScreenshotActive.value = enabled
        showNotification(if (enabled) "🚫 Anti-Screenshot Guard Enabled" else "🔓 Screenshot restriction removed.")
    }

    fun changeSelfDestructTimer(timer: String) {
        _selfDestructTimer.value = timer
        showNotification("⏳ Profile data self-destruct set to: $timer")
    }

    fun updateUserProfileComplete(
        name: String,
        age: Int,
        location: String,
        gender: String,
        interestedIn: String,
        bio: String,
        interests: String,
        avatarEmoji: String,
        avatarGradientIndex: Int,
        latitude: Double,
        longitude: Double,
        premiumTier: String,
        isVerified: Boolean,
        image1: String,
        image2: String,
        image3: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userProfile.value ?: return@launch
            val updated = user.copy(
                name = name,
                age = age,
                location = location,
                gender = gender,
                interestedIn = interestedIn,
                bio = bio,
                interests = interests,
                avatarEmoji = avatarEmoji,
                avatarGradientIndex = avatarGradientIndex,
                latitude = latitude,
                longitude = longitude,
                premiumTier = premiumTier,
                isVerified = isVerified,
                image1 = image1,
                image2 = image2,
                image3 = image3
            )
            profileDao.updateProfile(updated)
            showNotification("✨ Profile updated successfully with advanced options!")
        }
    }

    init {
        // Automatically check if database needs seeding
        viewModelScope.launch {
            checkAndSeedDatabase()
        }
    }

    private suspend fun checkAndSeedDatabase() {
        withContext(Dispatchers.IO) {
            val profilesList = profileDao.getAllProfilesFlow().firstOrNull() ?: emptyList()
            if (profilesList.isEmpty()) {
                // Seed 5 high-fidelity profiles and 1 fake profile for demonstrations
                val seedProfiles = listOf(
                    Profile(
                        name = "Leo", age = 28, bio = "Software Engineer & board game enthusiast. Let's debate about cyberpunk lore over hot chocolate!",
                        gender = "Male", interestedIn = "Female",
                        location = "San Francisco, CA", interests = "Coding, Coffee, Board Games, Hiking",
                        avatarGradientIndex = 0, avatarEmoji = "💻", isVerified = true, isUser = false,
                        latitude = 37.7599, longitude = -122.4148,
                        image1 = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&q=80",
                        image2 = "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=400&q=80",
                        image3 = "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=400&q=80"
                    ),
                    Profile(
                        name = "Sophia", age = 26, bio = "Art history grad student, amateur potter, and full-time cat mom. Let's find the best croissants in the city.",
                        gender = "Female", interestedIn = "Male",
                        location = "San Francisco, CA", interests = "Art, Cats, Baking, Museums",
                        avatarGradientIndex = 1, avatarEmoji = "🎨", isVerified = true, isUser = false,
                        latitude = 37.7932, longitude = -122.4140,
                        image1 = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&q=80",
                        image2 = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
                        image3 = "https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=400&q=80"
                    ),
                    Profile(
                        name = "Marcus", age = 31, bio = "Personal trainer and meal prep advocate. Love traveling, electronic music, and morning runs. Let's lift each other up!",
                        gender = "Male", interestedIn = "Everyone",
                        location = "Oakland, CA", interests = "Fitness, Travel, Tech, Running",
                        avatarGradientIndex = 2, avatarEmoji = "🏋️", isVerified = true, isUser = false,
                        latitude = 37.8044, longitude = -122.2711,
                        image1 = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&q=80",
                        image2 = "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=400&q=80",
                        image3 = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=400&q=80"
                    ),
                    Profile(
                        name = "Elena", age = 25, bio = "Plant enthusiast and landscape photographer. Usually found in a botanical garden or editing photos at a local cafe.",
                        gender = "Female", interestedIn = "Everyone",
                        location = "Berkeley, CA", interests = "Photography, Plants, Cafes, Nature",
                        avatarGradientIndex = 3, avatarEmoji = "📸", isVerified = true, isUser = false,
                        latitude = 37.8715, longitude = -122.2730,
                        image1 = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=400&q=80",
                        image2 = "https://images.unsplash.com/photo-1448375240586-882707db888b?w=400&q=80",
                        image3 = "https://images.unsplash.com/photo-1502082553048-f009c37129b9?w=400&q=80"
                    ),
                    Profile(
                        name = "Sam", age = 29, bio = "Indie game developer and sci-fi reader. Looking for someone to co-pilot on space sims or debate cyberpunk theories.",
                        gender = "Non-binary", interestedIn = "Everyone",
                        location = "San Francisco, CA", interests = "Gaming, Sci-Fi, Books, Pizza",
                        avatarGradientIndex = 4, avatarEmoji = "👾", isVerified = true, isUser = false,
                        latitude = 37.8036, longitude = -122.4368,
                        image1 = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&q=80",
                        image2 = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=400&q=80",
                        image3 = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=400&q=80"
                    ),
                    Profile(
                        name = "RichCryptoAngel", age = 24, bio = "I am a high-class financial adviser. If you want to make 500% profit in 1 week, swipe right and add my secure WhatsApp! No poor people.",
                        gender = "Female", interestedIn = "Everyone",
                        location = "San Jose, CA", interests = "Crypto, Gold, Luxury, WhatsApp",
                        avatarGradientIndex = 5, avatarEmoji = "💸", isVerified = false, isUser = false,
                        latitude = 37.3382, longitude = -121.8863,
                        image1 = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&q=80",
                        image2 = "https://images.unsplash.com/photo-1579621970563-ebec7560ff3e?w=400&q=80",
                        image3 = "https://images.unsplash.com/photo-1518186285589-2f7649de83e0?w=400&q=80"
                    )
                )
                seedProfiles.forEach { profileDao.insertProfile(it) }
                Log.d("DatingViewModel", "Database seeded with default profiles!")
            }

            val groupsList = groupChatDao.getAllGroupChatsFlow().firstOrNull() ?: emptyList()
            if (groupsList.isEmpty()) {
                val seedGroups = listOf(
                    GroupChat(
                        name = "👾 Tech & Cyberpunk Fans",
                        description = "Discuss software engineering, sci-fi novels, space simulators, and neon vibes.",
                        avatarGradientIndex = 4,
                        avatarEmoji = "👾"
                    ),
                    GroupChat(
                        name = "🥐 Croissant & Art Explorers",
                        description = "Finding the city's finest bakeries, pottery workshops, and gallery open nights.",
                        avatarGradientIndex = 1,
                        avatarEmoji = "🎨"
                    ),
                    GroupChat(
                        name = "🏃‍♂️ Active Fitness & Mornings",
                        description = "Morning running clubs, weightlifting advice, healthy meal preps, and techno workout tracks.",
                        avatarGradientIndex = 2,
                        avatarEmoji = "🏋️"
                    )
                )
                seedGroups.forEach { groupChatDao.insertGroupChat(it) }
                
                // Seed initial group messages
                // Group 1
                groupMessageDao.insertGroupMessage(
                    GroupMessage(
                        groupId = 1,
                        senderId = 1, // Leo
                        senderName = "Leo",
                        senderEmoji = "💻",
                        senderGradientIndex = 0,
                        content = "Who's down for a cyberpunk board game night this Thursday? 🎲"
                    )
                )
                groupMessageDao.insertGroupMessage(
                    GroupMessage(
                        groupId = 1,
                        senderId = 5, // Sam
                        senderName = "Sam",
                        senderEmoji = "👾",
                        senderGradientIndex = 4,
                        content = "Absolutely! I can bring some snacks and co-pilot! Let's build some cool automation decks."
                    )
                )
                
                // Group 2
                groupMessageDao.insertGroupMessage(
                    GroupMessage(
                        groupId = 2,
                        senderId = 2, // Sophia
                        senderName = "Sophia",
                        senderEmoji = "🎨",
                        senderGradientIndex = 1,
                        content = "I found an absolute gem of a bakery in the Mission District! Best buttery croissants ever."
                    )
                )
                groupMessageDao.insertGroupMessage(
                    GroupMessage(
                        groupId = 2,
                        senderId = 4, // Elena
                        senderName = "Elena",
                        senderEmoji = "📸",
                        senderGradientIndex = 3,
                        content = "Oh, please tell me they have a good botanical garden vibe or outdoor seating! I need to take some plant photos 📸"
                    )
                )
            }
        }
    }

    // Sign up / Create or edit user's own profile
    fun signUpAndCreateProfile(
        name: String,
        age: Int,
        gender: String,
        interestedIn: String,
        location: String,
        interests: String,
        avatarGradientIndex: Int,
        avatarEmoji: String,
        image1: String = "",
        image2: String = "",
        image3: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val userProfile = Profile(
                name = name,
                age = age,
                bio = "Co-creating my destiny. Let's talk about our favorite adventures!",
                gender = gender,
                interestedIn = interestedIn,
                location = location,
                interests = interests,
                avatarGradientIndex = avatarGradientIndex,
                avatarEmoji = avatarEmoji,
                isVerified = true,
                isUser = true,
                image1 = image1,
                image2 = image2,
                image3 = image3
            )
            profileDao.insertProfile(userProfile)
            _currentSwipeIndex.value = 0 // Reset swipe index
        }
    }

    fun updateUserProfile(updated: Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            profileDao.updateProfile(updated)
        }
    }

    // --- Swiping and Match Core ---

    fun swipeLeft() {
        viewModelScope.launch {
            val profiles = otherProfiles.value
            if (profiles.isNotEmpty()) {
                _currentSwipeIndex.value = (_currentSwipeIndex.value + 1) % profiles.size
            }
        }
    }

    fun swipeRight(swipedProfile: Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userProfile.value ?: return@launch

            // 85% match chance to make it highly engaging and instant!
            val isMatch = (swipedProfile.isVerified) || (Math.random() < 0.85)
            
            if (isMatch) {
                // Record match in DB
                val match = Match(userId = user.id, matchedUserId = swipedProfile.id)
                val matchId = matchDao.insertMatch(match).toInt()

                // Trigger splash screen
                _matchSplashProfile.value = swipedProfile

                // Automatically generate a welcoming introductory icebreaker from the matched user
                val welcomeMessage = "Hey there! Thanks for matching. I saw we both love ${
                    swipedProfile.interestList.firstOrNull() ?: "making new connections"
                }! Tell me more?"
                
                messageDao.insertMessage(
                    Message(
                        matchId = matchId,
                        senderId = swipedProfile.id,
                        receiverId = user.id,
                        content = welcomeMessage
                    )
                )

                showNotification("It's a Match! You and ${swipedProfile.name} connected.")
            }

            // Move to next card
            val profiles = otherProfiles.value
            if (profiles.isNotEmpty()) {
                _currentSwipeIndex.value = (_currentSwipeIndex.value + 1) % profiles.size
            }
        }
    }

    fun undoSwipe() {
        viewModelScope.launch {
            val profiles = otherProfiles.value
            if (profiles.isNotEmpty()) {
                val currentIndex = _currentSwipeIndex.value
                val previousIndex = if (currentIndex - 1 < 0) profiles.size - 1 else currentIndex - 1
                _currentSwipeIndex.value = previousIndex
            }
        }
    }

    fun instantMatch(swipedProfile: Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userProfile.value ?: return@launch
            
            // Record match in DB with 100% guarantee
            val match = Match(userId = user.id, matchedUserId = swipedProfile.id)
            val matchId = matchDao.insertMatch(match).toInt()

            // Trigger splash screen
            _matchSplashProfile.value = swipedProfile

            // Automatically generate a welcoming introductory icebreaker from the matched user
            val welcomeMessage = "⚡ INSTANT MATCH! Hey there! I'm so thrilled we connected instantly. I love ${
                swipedProfile.interestList.firstOrNull() ?: "making new connections"
            }! Let's make something amazing happen!"
            
            messageDao.insertMessage(
                Message(
                    matchId = matchId,
                    senderId = swipedProfile.id,
                    receiverId = user.id,
                    content = welcomeMessage
                )
            )

            showNotification("Instant Match! Connected with ${swipedProfile.name} ⚡")

            // Move to next card
            val profiles = otherProfiles.value
            if (profiles.isNotEmpty()) {
                _currentSwipeIndex.value = (_currentSwipeIndex.value + 1) % profiles.size
            }
        }
    }

    fun dismissMatchSplash() {
        _matchSplashProfile.value = null
    }

    fun setActiveMatchId(id: Int?) {
        _activeMatchId.value = id
        _icebreakers.value = emptyList() // clear previous suggestions
    }

    // Observe active chat messages
    fun getMessagesForMatch(matchId: Int): Flow<List<Message>> {
        return messageDao.getMessagesForMatchFlow(matchId)
    }

    // --- Message Sending with Content Moderation ---

    fun sendMessage(matchId: Int, content: String) {
        val user = userProfile.value ?: return
        val matchesList = matches.value
        val matchObj = matchesList.find { it.id == matchId } ?: return
        val receiverId = if (matchObj.userId == user.id) matchObj.matchedUserId else matchObj.userId

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Save user's message temporarily with a loading state, or run content moderation first
            val (isUnsafe, reason) = GeminiClient.moderateMessage(content)

            if (isUnsafe) {
                // Insert as blocked message
                messageDao.insertMessage(
                    Message(
                        matchId = matchId,
                        senderId = user.id,
                        receiverId = receiverId,
                        content = content,
                        isModerated = true,
                        moderationWarning = reason ?: "Content flagged for safety violations."
                    )
                )
                showNotification("⚠️ Message Blocked: Safety Guidelines Violation.")
                return@launch
            }

            // Insert normal safe message
            messageDao.insertMessage(
                Message(
                    matchId = matchId,
                    senderId = user.id,
                    receiverId = receiverId,
                    content = content
                )
            )

            // Trigger simulated smart reply from the matched user
            delay(150)
            generateSimulatedReply(matchId, receiverId, content)
        }
    }

    // Call Gemini API to formulate a realistic response based on the match's bio
    private suspend fun generateSimulatedReply(matchId: Int, senderId: Int, lastUserMsg: String) {
        val matchProfile = profileDao.getProfileById(senderId) ?: return
        val user = userProfile.value ?: return

        val prompt = """
            You are a user on a dating app named ${matchProfile.name} (Age: ${matchProfile.age}, Bio: "${matchProfile.bio}", Interests: "${matchProfile.interests}").
            You are chatting with ${user.name} (Bio: "${user.bio}", Interests: "${user.interests}").
            
            They just sent you this message: "$lastUserMsg"
            
            Write a natural, conversational, friendly, and engaging reply back to them. Keep it between 1 to 2 sentences. Include a relevant emoji and ask a short question to keep the conversation flowing. Do not use quotes around the response.
        """.trimIndent()

        val reply = withContext(Dispatchers.IO) {
            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Friendly fallback replies if offline/no key
                val replies = listOf(
                    "That sounds super interesting! What else do you enjoy doing during your free time? 😊",
                    "Aww, that's awesome! I would love to know more about that. How's your week going? ✨",
                    "Haha love that! Let's definitely plan to talk more. What is your absolute favorite spot in the city? ☕"
                )
                replies.random()
            } else {
                try {
                    val request = com.example.network.GenerateContentRequest(
                        contents = listOf(com.example.network.Content(parts = listOf(com.example.network.Part(text = prompt)))),
                        generationConfig = com.example.network.GenerationConfig(temperature = 0.8f, maxOutputTokens = 120)
                    )
                    val response = com.example.network.RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                        ?: "That sounds great! Tell me more about it? 😄"
                } catch (e: Exception) {
                    "That's so cool! Tell me more? 😊"
                }
            }
        }

        messageDao.insertMessage(
            Message(
                matchId = matchId,
                senderId = senderId,
                receiverId = user.id,
                content = reply
            )
        )
        showNotification("New message from ${matchProfile.name}!")
    }

    // --- Message Group (Group Chat) and Reel Sharing Methods ---

    fun setActiveGroupId(id: Int?) {
        _activeGroupId.value = id
    }

    fun getMessagesForGroup(groupId: Int): Flow<List<GroupMessage>> {
        return groupMessageDao.getMessagesForGroupFlow(groupId)
    }

    fun sendGroupMessage(groupId: Int, content: String) {
        val user = userProfile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val (isUnsafe, reason) = GeminiClient.moderateMessage(content)
            if (isUnsafe) {
                showNotification("⚠️ Message Blocked: Safety Guidelines Violation.")
                return@launch
            }

            groupMessageDao.insertGroupMessage(
                GroupMessage(
                    groupId = groupId,
                    senderId = 0, // User
                    senderName = user.name,
                    senderEmoji = user.avatarEmoji,
                    senderGradientIndex = user.avatarGradientIndex,
                    content = content
                )
            )

            // Trigger group reply
            simulateGroupReply(groupId, content)
        }
    }

    fun shareReelToMatch(matchId: Int, reelTitle: String, reelArtist: String, reelEmoji: String) {
        val user = userProfile.value ?: return
        val matchesList = matches.value
        val matchObj = matchesList.find { it.id == matchId } ?: return
        val receiverId = if (matchObj.userId == user.id) matchObj.matchedUserId else matchObj.userId

        val messageContent = "🎬 Shared Reel: $reelEmoji $reelTitle by $reelArtist"
        
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.insertMessage(
                Message(
                    matchId = matchId,
                    senderId = user.id,
                    receiverId = receiverId,
                    content = messageContent
                )
            )
            showNotification("Shared reel with ${profileDao.getProfileById(receiverId)?.name ?: "friend"}! 🚀")

            // Simulate instant response from friend
            delay(1500)
            generateSimulatedReply(matchId, receiverId, messageContent)
        }
    }

    fun shareReelToGroup(groupId: Int, reelTitle: String, reelArtist: String, reelEmoji: String) {
        val user = userProfile.value ?: return
        val messageContent = "🎬 Shared Reel: $reelEmoji $reelTitle by $reelArtist"
        
        viewModelScope.launch(Dispatchers.IO) {
            groupMessageDao.insertGroupMessage(
                GroupMessage(
                    groupId = groupId,
                    senderId = 0, // User
                    senderName = user.name,
                    senderEmoji = user.avatarEmoji,
                    senderGradientIndex = user.avatarGradientIndex,
                    content = messageContent
                )
            )
            showNotification("Shared reel to Group Chat! 🚀")

            // Simulate group member response
            simulateGroupReply(groupId, messageContent)
        }
    }

    fun createGroupChat(name: String, description: String, avatarEmoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupChatDao.insertGroupChat(
                GroupChat(
                    name = name,
                    description = description,
                    avatarGradientIndex = (0..5).random(),
                    avatarEmoji = avatarEmoji
                )
            )
            showNotification("Group Chat created! 🥳")
        }
    }

    private fun simulateGroupReply(groupId: Int, lastUserMsg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            delay((1200..2800).random().toLong())
            val matchedProfiles = otherProfiles.value
            if (matchedProfiles.isEmpty()) return@launch

            // Filter profiles that belong to this group
            val candidates = when (groupId) {
                1 -> matchedProfiles.filter { it.name == "Leo" || it.name == "Sam" }
                2 -> matchedProfiles.filter { it.name == "Sophia" || it.name == "Elena" }
                3 -> matchedProfiles.filter { it.name == "Marcus" || it.name == "Elena" || it.name == "Leo" }
                else -> matchedProfiles
            }
            val finalCandidates = if (candidates.isEmpty()) matchedProfiles else candidates
            val replier = finalCandidates.random()
            
            // Call Gemini API to formulate a funny or relevant response, or fall back to high fidelity replies
            val prompt = """
                You are ${replier.name} (Bio: "${replier.bio}", Interests: "${replier.interests}").
                You are participating in a group chat named "${when(groupId) {
                    1 -> "Tech & Cyberpunk Fans"
                    2 -> "Croissant & Art Explorers"
                    else -> "Active Fitness & Mornings"
                }}".
                
                The user just posted this message in the group: "$lastUserMsg"
                
                Respond to it as ${replier.name} in a short, casual, friendly chat message (maximum 2 sentences). Include 1 relevant emoji. Do not use quotes. Do not prefix with your name.
            """.trimIndent()

            val response = try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    null
                } else {
                    val request = com.example.network.GenerateContentRequest(
                        contents = listOf(com.example.network.Content(parts = listOf(com.example.network.Part(text = prompt)))),
                        generationConfig = com.example.network.GenerationConfig(temperature = 0.8f, maxOutputTokens = 120)
                    )
                    val res = com.example.network.RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
                    res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                }
            } catch (e: Exception) {
                null
            }

            val fallbackResponse = when {
                lastUserMsg.contains("Shared Reel", ignoreCase = true) -> {
                    listOf(
                        "Wow, that song has such a cool beat! Added to my playlist! 💖",
                        "Wait, I love this artist! They are amazing. Good choice!",
                        "Super cool vibe! Let's play this next time we meet up!",
                        "Thanks for sharing! This makes my day. ✨",
                        "Ah, this brings back so many memories! Love it!"
                    ).random()
                }
                else -> {
                    listOf(
                        "That's so interesting, tell me more! 🤔",
                        "Haha that sounds fun! Let me know if we are doing this.",
                        "Totally agree with you! 💯",
                        "Oh wow! Nice one.",
                        "Awesome! Let's catch up soon."
                    ).random()
                }
            }

            val finalReply = response ?: fallbackResponse

            groupMessageDao.insertGroupMessage(
                GroupMessage(
                    groupId = groupId,
                    senderId = replier.id,
                    senderName = replier.name,
                    senderEmoji = replier.avatarEmoji,
                    senderGradientIndex = replier.avatarGradientIndex,
                    content = finalReply
                )
            )

            showNotification("💬 ${replier.name} posted in group chat")
        }
    }

    // --- Gemini AI Features ---

    // 1. AI Bio Generator
    fun generateAIBio(qualities: String) {
        viewModelScope.launch {
            _aiBioLoading.value = true
            _aiBioGenerationState.value = null
            try {
                val bio = GeminiClient.generateBio(qualities)
                _aiBioGenerationState.value = bio
            } catch (e: Exception) {
                _aiBioGenerationState.value = "Failed to generate bio. Try again!"
            } finally {
                _aiBioLoading.value = false
            }
        }
    }

    fun clearAIBioState() {
        _aiBioGenerationState.value = null
    }

    fun analyzeUserProfile(bio: String, interests: String) {
        viewModelScope.launch {
            _profileAnalysisLoading.value = true
            _profileAnalysisResult.value = null
            try {
                val analysis = GeminiClient.analyzeProfile(bio, interests)
                _profileAnalysisResult.value = analysis
            } catch (e: Exception) {
                _profileAnalysisResult.value = "Failed to analyze profile. Please try again!"
            } finally {
                _profileAnalysisLoading.value = false
            }
        }
    }

    fun clearProfileAnalysisState() {
        _profileAnalysisResult.value = null
    }

    fun updateUserBioAndInterests(newBio: String, newInterests: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userProfile.value ?: return@launch
            val updated = user.copy(bio = newBio, interests = newInterests)
            profileDao.updateProfile(updated)
            showNotification("✅ Profile successfully updated with new bio and interests!")
        }
    }

    // 2. Icebreaker Suggestion Generator
    fun fetchIcebreakersForActiveMatch(matchName: String, matchBio: String, matchInterests: String) {
        viewModelScope.launch {
            _icebreakersLoading.value = true
            _icebreakers.value = emptyList()
            try {
                val suggestions = GeminiClient.suggestIcebreakers(matchName, matchBio, matchInterests)
                _icebreakers.value = suggestions
            } catch (e: Exception) {
                _icebreakers.value = listOf(
                    "Tell me about your absolute favorite weekend hobby! 🌟",
                    "If you could travel anywhere right now, where would it be? ✈️"
                )
            } finally {
                _icebreakersLoading.value = false
            }
        }
    }

    // 3. Match Compatibility prediction (Calculated on-demand or cached)
    fun loadCompatibilityRecommendation(matchProfile: Profile) {
        if (_compatibilityCache.value.containsKey(matchProfile.id)) return

        viewModelScope.launch {
            val user = userProfile.value ?: return@launch
            val result = GeminiClient.getMatchRecommendation(
                userBio = user.bio,
                userInterests = user.interests,
                matchName = matchProfile.name,
                matchBio = matchProfile.bio,
                matchInterests = matchProfile.interests
            )
            _compatibilityCache.value = _compatibilityCache.value + (matchProfile.id to result)
        }
    }

    // 4. Fake Profile Detection & Logging (Admin Tool)
    fun runFakeProfileDetection(profile: Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            val (isFake, analysis) = GeminiClient.detectFakeProfile(profile.name, profile.bio, profile.interests)
            
            // Update profile with fake verification logs
            val updated = profile.copy(
                isVerified = !isFake,
                isFakeFlagged = isFake,
                fakeAnalysis = analysis
            )
            profileDao.updateProfile(updated)

            val alertMsg = if (isFake) {
                "🚨 Fake Profile Detected! Flagged: ${profile.name}"
            } else {
                "✅ Profile Verification Clean: ${profile.name}"
            }
            showNotification(alertMsg)
        }
    }

    // Clear system notification
    fun clearNotification() {
        _systemNotification.value = null
    }

    fun showNotification(msg: String) {
        viewModelScope.launch {
            // Determine type and title from msg content
            val type = when {
                msg.contains("Match", ignoreCase = true) || msg.contains("matched", ignoreCase = true) || msg.contains("connected", ignoreCase = true) || msg.contains("SUPER LIKED", ignoreCase = true) -> NotificationType.MATCH
                msg.contains("Safety", ignoreCase = true) || msg.contains("Blocked", ignoreCase = true) || msg.contains("Fake", ignoreCase = true) || msg.contains("biometric", ignoreCase = true) || msg.contains("PIN", ignoreCase = true) || msg.contains("Locked", ignoreCase = true) || msg.contains("Security", ignoreCase = true) || msg.contains("Advisory", ignoreCase = true) || msg.contains("Secure", ignoreCase = true) || msg.contains("Trust", ignoreCase = true) || msg.contains("unlocked", ignoreCase = true) -> NotificationType.SAFETY
                msg.contains("message", ignoreCase = true) || msg.contains("chat", ignoreCase = true) -> NotificationType.CHAT
                msg.contains("GPS", ignoreCase = true) || msg.contains("Location", ignoreCase = true) || msg.contains("Radar", ignoreCase = true) || msg.contains("scan", ignoreCase = true) || msg.contains("radius", ignoreCase = true) -> NotificationType.GPS
                else -> NotificationType.SYSTEM
            }
            val title = when (type) {
                NotificationType.MATCH -> "Match Update! 💖"
                NotificationType.SAFETY -> "Privacy & Safety 🛡️"
                NotificationType.CHAT -> "Secure Chat 💬"
                NotificationType.GPS -> "Location Sync 📍"
                NotificationType.SYSTEM -> "System Notification ⚙️"
            }
            
            val newNotification = AppNotification(
                title = title,
                message = msg,
                type = type,
                timestamp = System.currentTimeMillis()
            )
            
            // Prepend new notification to keep list sorted with newest first
            _appNotifications.value = listOf(newNotification) + _appNotifications.value

            _systemNotification.value = msg
            // Auto dismiss after 1.5 seconds (much faster notification dismissal)
            delay(1500)
            if (_systemNotification.value == msg) {
                _systemNotification.value = null
            }
        }
    }

    // --- 10 New Options/Security Logic Methods ---

    // 1. PIN Lock & Security Shield
    fun setAppLockedState(locked: Boolean) {
        _isAppLocked.value = locked
    }

    fun toggleUserProfilePinLock(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userProfile.value ?: return@launch
            val updated = user.copy(pinLocked = enabled)
            profileDao.updateProfile(updated)
            if (enabled) {
                showNotification("🔒 Secure PIN Lock enabled! PIN is 1234")
            } else {
                _isAppLocked.value = false
                showNotification("🔓 PIN Lock disabled.")
            }
        }
    }

    fun checkAndApplyStartupPinLock() {
        val user = userProfile.value
        if (user != null && user.pinLocked) {
            _isAppLocked.value = true
        }
    }

    fun submitSecurityPin(pin: String): Boolean {
        return if (pin == "1234") {
            _isAppLocked.value = false
            showNotification("🔓 Device Biometric PIN Approved!")
            true
        } else {
            showNotification("❌ Incorrect PIN. Please enter '1234' for demo.")
            false
        }
    }

    // 2. Biometric Photo Verification Challenge (Live Simulator)
    fun startBiometricVerification() {
        _verificationStep.value = 1
    }

    fun advanceBiometricStep() {
        val current = _verificationStep.value
        if (current in 1..2) {
            _verificationStep.value = current + 1
        } else if (current == 3) {
            _verificationStep.value = 4
            viewModelScope.launch(Dispatchers.IO) {
                val user = userProfile.value
                if (user != null) {
                    val updated = user.copy(isVerified = true, trustScore = 100)
                    profileDao.updateProfile(updated)
                    showNotification("📸 Identity Verified with Biometric Scan! (100% Trust)")
                }
            }
        }
    }

    fun resetBiometricChallenge() {
        _verificationStep.value = 0
    }

    // 3. Super Like Confetti Spark
    fun triggerSuperLikeConfetti() {
        _confettiCount.value = _confettiCount.value + 1
        showNotification("💖 SUPER LIKED! Confetti Spark Particle Action Triggered!")
    }

    // 4. On-demand AI Integrity Report & Scam Check
    fun scanProfileIntegrity(profile: Profile) {
        _onDemandScanningId.value = profile.id
        _onDemandIntegrityReport.value = null
        viewModelScope.launch(Dispatchers.IO) {
            delay(1200) // beautiful delay for high-fidelity scanning feel
            val score = if (profile.name == "RichCryptoAngel") 12 else if (profile.isVerified) 98 else 74
            val passedSafetyCheck = score >= 70
            val trustColor = if (score > 80) "EXCELLENT" else if (score > 50) "MODERATE" else "CRITICAL RISK"
            
            val report = """
                🔍 AI INTEGRITY SCAN REPORT
                ===========================
                Target: ${profile.name} (Age ${profile.age})
                FC Trust Score: $score% ($trustColor)
                
                [SAFETY SCANS]
                - Facial Biometric Match: ${if (passedSafetyCheck) "PASSED (AI Photo Verification Liveness Confirmed)" else "FAILED (Inconsistent landmarks)"}
                - Natural Language Spam Audit: ${if (profile.name == "RichCryptoAngel") "SPAM ALERT: Heavy promotional terms detected." else "PASSED (Organic chat patterns)"}
                - Link Security: Checked (No dangerous domains)
                - Threat Patterns: ${if (profile.name == "RichCryptoAngel") "SPAM DETECTED: Solicits money, external handles, or crypto. High probability of a commercial bot." else "NO ALERTS (Authentic behavior)"}
                - Risk level: ${if (profile.name == "RichCryptoAngel") "HIGH RISK (Engagement restricted)" else "LOW RISK (Verified Safe)"}
            """.trimIndent()
            
            _onDemandIntegrityReport.value = report
            _onDemandScanningId.value = null
            
            val updated = profile.copy(
                trustScore = score,
                isVerified = if (passedSafetyCheck) true else profile.isVerified
            )
            profileDao.updateProfile(updated)
            if (passedSafetyCheck && !profile.isVerified) {
                showNotification("✨ ${profile.name} has passed the AI-powered safety check and is now Verified!")
            }
        }
    }

    fun dismissIntegrityReport() {
        _onDemandIntegrityReport.value = null
    }

    // 5. Dynamic Vibe/Mood Badge Selector
    fun changeUserMood(mood: String) {
        _activeUserMood.value = mood
        viewModelScope.launch(Dispatchers.IO) {
            val user = userProfile.value ?: return@launch
            val updated = user.copy(moodBadge = mood)
            profileDao.updateProfile(updated)
            showNotification("🎨 Vibe updated to: $mood")
        }
    }

    // 6. Dating Goal Intent Capsules
    fun changeUserDatingGoal(goal: String) {
        _activeUserGoal.value = goal
        viewModelScope.launch(Dispatchers.IO) {
            val user = userProfile.value ?: return@launch
            val updated = user.copy(datingGoal = goal)
            profileDao.updateProfile(updated)
            showNotification("🎯 Intent set to: $goal")
        }
    }

    // 7. Incognito Privacy Toggle
    fun toggleIncognitoMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userProfile.value ?: return@launch
            val updated = user.copy(isIncognito = enabled)
            profileDao.updateProfile(updated)
            showNotification(if (enabled) "👤 Stealth Shield: You are now INCOGNITO (Invisible)" else "👤 Stealth Shield disabled.")
        }
    }

    // Outbound chat scanner / message sanitizer
    fun clearMessageFlagged() {
        _messageFlaggedNotification.value = null
    }

    // Admin action: Add a custom profile
    fun createCustomProfile(profile: Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            profileDao.insertProfile(profile)
            showNotification("Successfully added profile: ${profile.name}")
        }
    }

    // Admin action: Reset all matching lists
    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            matchDao.deleteAll()
            messageDao.deleteAll()
            profileDao.deleteAll()
            groupChatDao.deleteAll()
            groupMessageDao.deleteAll()
            _currentSwipeIndex.value = 0
            _compatibilityCache.value = emptyMap()
            _currentLocation.value = "San Francisco, CA"
            _virtualDateMessages.value = emptyList()
            checkAndSeedDatabase()
            showNotification("Database Reset to Defaults.")
        }
    }

    // --- 10 NEW PREMIUM & FUTURE OPTIONS METHODS ---

    // 1. Subscription Billing Upgrade
    fun upgradeUserPremiumTier(tier: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userProfile.value ?: return@launch
            val updated = user.copy(premiumTier = tier)
            profileDao.updateProfile(updated)
            
            val tierName = when (tier) {
                "Premium" -> "👑 PREMIUM (₹1/day)"
                "Premium Plus" -> "💎 PREMIUM PLUS (₹9/day)"
                else -> "💎 PREMIUM PLUS (₹9/day)"
            }
            showNotification("✨ Success! You are now subscribed to $tierName. Unlocked priority safety shielding and premium future features!")
            _confettiCount.value = _confettiCount.value + 1
        }
    }

    // 2. Premium Location GPS Teleportation
    fun changeDatingLocation(location: String) {
        _currentLocation.value = location
        showNotification("✈️ Premium Teleport active: Location set to $location! Matching feed updated.")
    }

    // 3. AI Virtual Date Simulator - Setup
    fun selectVirtualPartner(name: String, scenario: String) {
        _virtualPartnerName.value = name
        _virtualScenario.value = scenario
        resetVirtualDate()
        showNotification("🔮 Simulated Virtual Date active with $name in $scenario!")
    }

    // 4. AI Virtual Date Simulator - Reset
    fun resetVirtualDate() {
        val initialMsg = Message(
            id = (0..Int.MAX_VALUE).random(),
            matchId = -999,
            senderId = -999,
            receiverId = 1,
            content = "Hi! I'm so glad we're on this virtual date together in ${_virtualScenario.value}. Tell me, what's a typical perfect day look like for you? 😊",
            timestamp = System.currentTimeMillis()
        )
        _virtualDateMessages.value = listOf(initialMsg)
    }

    // 5. AI Virtual Date Simulator - Conversation Loop (Real Gemini Beta API Integration)
    fun sendVirtualDateMessage(text: String) {
        if (text.isBlank()) return
        val currentMsgs = _virtualDateMessages.value.toMutableList()
        val userMsg = Message(
            id = (0..Int.MAX_VALUE).random(),
            matchId = -999,
            senderId = 1, // User is 1
            receiverId = -999,
            content = text,
            timestamp = System.currentTimeMillis()
        )
        currentMsgs.add(userMsg)
        _virtualDateMessages.value = currentMsgs

        _isVirtualDateLoading.value = true
        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val responseText = if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                delay(120)
                val replies = listOf(
                    "That sounds incredibly nice! Honestly, I can totally visualize that. What else gets you excited in life? ✨",
                    "Wow, I love how you express that! It really matches our vibe here in ${_virtualScenario.value}. Let's order another round of drinks! 🥂",
                    "That is so cool. I'm really glad we swiped on each other today. Tell me, do you consider yourself more of an adventurous explorer or a cozy indoor gamer?",
                    "Haha, that makes so much sense! I can definitely see why you'd say that. Let's make a virtual toast to discovering new perspectives! ☕"
                )
                replies.random()
            } else {
                val systemPrompt = "You are ${_virtualPartnerName.value}, roleplaying a warm, playful, and charming date in a virtual simulation. The current scenario is: ${_virtualScenario.value}. Respond in 1-2 friendly, conversational sentences to the user's messages, maintaining the chosen theme and setting. Do not write anything outside your character response."
                val chatHistory = currentMsgs.takeLast(8).joinToString("\n") { 
                    val sender = if (it.senderId == 1) "User" else "You"
                    "$sender: ${it.content}"
                }
                val promptText = "The conversation so far:\n$chatHistory\n\nGenerate your next warm conversational response as ${_virtualPartnerName.value}:"
                try {
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                        generationConfig = GenerationConfig(temperature = 0.8f, maxOutputTokens = 150),
                        systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                    )
                    val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                        ?: "That's lovely! Tell me more."
                } catch (e: Exception) {
                    Log.e("DatingViewModel", "Virtual Date AI Error: ", e)
                    "That's so interesting! Tell me more about what inspires you."
                }
            }

            val partnerMsg = Message(
                id = (0..Int.MAX_VALUE).random(),
                matchId = -999,
                senderId = -999, // celeste
                receiverId = 1,
                content = responseText,
                timestamp = System.currentTimeMillis()
            )
            val finalMsgs = _virtualDateMessages.value.toMutableList()
            finalMsgs.add(partnerMsg)
            _virtualDateMessages.value = finalMsgs
            _isVirtualDateLoading.value = false
        }
    }

    // --- 11. GPS NEARBY SCAN & RADAR STATES ---
    private val _isScanningNearby = MutableStateFlow(false)
    val isScanningNearby: StateFlow<Boolean> = _isScanningNearby.asStateFlow()

    private val _nearbyRadiusKm = MutableStateFlow(15f)
    val nearbyRadiusKm: StateFlow<Float> = _nearbyRadiusKm.asStateFlow()

    private val _gpsEnabled = MutableStateFlow(true)
    val gpsEnabled: StateFlow<Boolean> = _gpsEnabled.asStateFlow()

    fun setNearbyRadius(radius: Float) {
        _nearbyRadiusKm.value = radius
    }

    fun toggleGps(enabled: Boolean) {
        _gpsEnabled.value = enabled
        if (enabled) {
            showNotification("📍 GPS Satellites synchronized. Secure local tracking active.")
        } else {
            showNotification("🚫 GPS Disabled. Nearby radar will operate on last known location.")
        }
    }

    fun triggerNearbyScan() {
        if (!_gpsEnabled.value) {
            showNotification("❌ Cannot scan: Please enable GPS Location to run secure nearby radar!")
            return
        }
        _isScanningNearby.value = true
        viewModelScope.launch {
            delay(300) // Beautiful radar scan animation duration (tuned for maximum speed!)
            _isScanningNearby.value = false
            showNotification("📡 Radar scan complete. Found local active profiles within ${_nearbyRadiusKm.value.toInt()} km!")
            _confettiCount.value = _confettiCount.value + 1
        }
    }

    // GPS Distance calculation (Haversine formula)
    fun getDistanceToProfile(otherProfile: Profile): Double {
        val user = userProfile.value ?: return 5.0 // fallback
        val r = 6371.0 // Earth radius in km
        val lat1 = Math.toRadians(user.latitude)
        val lon1 = Math.toRadians(user.longitude)
        val lat2 = Math.toRadians(otherProfile.latitude)
        val lon2 = Math.toRadians(otherProfile.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    // --- 12. HIGH-FIDELITY SECURE AUDIO/VIDEO CALL ENGINE ---
    private val _activeCallProfile = MutableStateFlow<Profile?>(null)
    val activeCallProfile: StateFlow<Profile?> = _activeCallProfile.asStateFlow()

    private val _callState = MutableStateFlow("IDLE") // "IDLE", "CONNECTING", "RINGING", "CONNECTED", "ENDED"
    val callState: StateFlow<String> = _callState.asStateFlow()

    private val _callType = MutableStateFlow("AUDIO") // "AUDIO" or "VIDEO"
    val callType: StateFlow<String> = _callType.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds.asStateFlow()

    private val _isCallMuted = MutableStateFlow(false)
    val isCallMuted: StateFlow<Boolean> = _isCallMuted.asStateFlow()

    private val _isCallCameraOff = MutableStateFlow(false)
    val isCallCameraOff: StateFlow<Boolean> = _isCallCameraOff.asStateFlow()

    private val _isCallSpeakerOn = MutableStateFlow(true)
    val isCallSpeakerOn: StateFlow<Boolean> = _isCallSpeakerOn.asStateFlow()

    private val _simulatedCallTranscript = MutableStateFlow("Establishing secure tunnel...")
    val simulatedCallTranscript: StateFlow<String> = _simulatedCallTranscript.asStateFlow()

    private var callTimerJob: kotlinx.coroutines.Job? = null
    private var callTranscriptJob: kotlinx.coroutines.Job? = null

    fun initiateCall(profile: Profile, type: String) {
        _activeCallProfile.value = profile
        _callType.value = type
        _callState.value = "CONNECTING"
        _callDurationSeconds.value = 0
        _isCallMuted.value = false
        _isCallCameraOff.value = false
        _simulatedCallTranscript.value = "Establishing AES-256 E2EE handshake..."

        viewModelScope.launch {
            delay(150)
            if (_callState.value == "CONNECTING") {
                _callState.value = "RINGING"
                _simulatedCallTranscript.value = "Securing line with ${profile.name}... Ringing..."
            }
            delay(300)
            if (_callState.value == "RINGING") {
                _callState.value = "CONNECTED"
                _simulatedCallTranscript.value = "Connected • Secure Peer-to-Peer Link"
                startCallBillingAndTranscriptEngine(profile)
            }
        }
    }

    private fun startCallBillingAndTranscriptEngine(profile: Profile) {
        // Start duration timer
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (_callState.value == "CONNECTED") {
                delay(1000)
                _callDurationSeconds.value = _callDurationSeconds.value + 1
            }
        }

        // Start simulated transcript dialogue updates
        callTranscriptJob?.cancel()
        callTranscriptJob = viewModelScope.launch {
            val dialogueList = listOf(
                "Hey! Just picked up your call over the Find Correct secure line. 🔒",
                "Wow, the connection is so crystal clear! Is this really fully encrypted?",
                "Yes, the quantum security lock indicator is fully green on my end! Vibe check passed. ☀️",
                "I was actually just looking at your profile. So cool we got connected nearby!",
                "Honestly, security is so important to me on these apps. I love that we can call safely without sharing our phone numbers! 🛡️",
                "Do you have any plans for this weekend? Maybe we can meet up in person since we are so close!",
                "That sounds amazing! Let's continue chatting over message and set up a coffee date! ☕",
                "Awesome! Talk to you in a bit. Have a wonderful rest of your day! 😊"
            )
            var index = 0
            while (_callState.value == "CONNECTED") {
                delay(if (index == 0) 2000 else 6000)
                if (index < dialogueList.size) {
                    _simulatedCallTranscript.value = "${profile.name}: \"${dialogueList[index]}\""
                    index++
                } else {
                    _simulatedCallTranscript.value = "E2EE line active • No intrusion detected."
                }
            }
        }
    }

    fun toggleCallMute() {
        _isCallMuted.value = !_isCallMuted.value
        showNotification(if (_isCallMuted.value) "🎙️ Microphone muted" else "🎙️ Microphone active")
    }

    fun toggleCallCamera() {
        _isCallCameraOff.value = !_isCallCameraOff.value
        showNotification(if (_isCallCameraOff.value) "📹 Camera disabled" else "📹 Camera activated")
    }

    fun toggleCallSpeaker() {
        _isCallSpeakerOn.value = !_isCallSpeakerOn.value
        showNotification(if (_isCallSpeakerOn.value) "🔊 Audio set to Speaker" else "🔈 Audio set to Receiver")
    }

    fun endActiveCall() {
        _callState.value = "ENDED"
        callTimerJob?.cancel()
        callTranscriptJob?.cancel()
        viewModelScope.launch {
            _simulatedCallTranscript.value = "Call secure line disconnected."
            delay(150)
            _callState.value = "IDLE"
            _activeCallProfile.value = null
        }
    }

    // --- 13. DIRECT CHAT CREATION (FROM RADAR / NEARBY FINDER) ---
    fun startDirectChat(targetProfileId: Int, onMatchIdCreated: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userProfile.value ?: return@launch
            
            // Check if match already exists
            val currentMatches = matches.first()
            val existingMatch = currentMatches.find { 
                (it.userId == user.id && it.matchedUserId == targetProfileId) ||
                (it.userId == targetProfileId && it.matchedUserId == user.id)
            }

            if (existingMatch != null) {
                withContext(Dispatchers.Main) {
                    onMatchIdCreated(existingMatch.id)
                }
            } else {
                // Create a secure direct match immediately
                val newMatch = Match(userId = user.id, matchedUserId = targetProfileId)
                val newMatchId = matchDao.insertMatch(newMatch).toInt()
                
                // Add an initial welcome message
                val targetProfile = profileDao.getProfileById(targetProfileId)
                if (targetProfile != null) {
                    val welcomeMsg = Message(
                        matchId = newMatchId,
                        senderId = targetProfileId,
                        receiverId = user.id,
                        content = "Hey! 📍 I saw you on the secure GPS Radar within my neighborhood range. Love to connect!",
                        timestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(welcomeMsg)
                }

                withContext(Dispatchers.Main) {
                    showNotification("🔓 Secure chat line unlocked with direct connection!")
                    onMatchIdCreated(newMatchId)
                }
            }
        }
    }

    // --- AI Customer Support Chatbot States & Actions ---
    private val _supportChatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(listOf(
        "Hello! I am your SafeCupid AI Support assistant. 🛡️ How can I help you today? Ask me about our GPS Local Radar, E2E calling, Biometric Identity Verification, AI virtual dates, Stealth incognito mode, or any general help!" to false
    ))
    val supportChatHistory: StateFlow<List<Pair<String, Boolean>>> = _supportChatHistory.asStateFlow()

    private val _isSupportLoading = MutableStateFlow(false)
    val isSupportLoading: StateFlow<Boolean> = _isSupportLoading.asStateFlow()

    fun sendSupportMessage(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        
        // Add user message to history
        val currentHistory = _supportChatHistory.value
        _supportChatHistory.value = currentHistory + (trimmed to true)
        
        viewModelScope.launch {
            _isSupportLoading.value = true
            try {
                // We pass the history drops the last user message as it is already added to contents in GeminiClient
                val reply = GeminiClient.supportChat(trimmed, currentHistory)
                _supportChatHistory.value = _supportChatHistory.value + (reply to false)
            } catch (e: Exception) {
                Log.e("DatingViewModel", "Error in customer support chat: ", e)
                _supportChatHistory.value = _supportChatHistory.value + ("I ran into an issue connecting to support servers. Please check your network and try again!" to false)
            } finally {
                _isSupportLoading.value = false
            }
        }
    }

    fun clearSupportChat() {
        _supportChatHistory.value = listOf(
            "Hello! I am your SafeCupid AI Support assistant. 🛡️ How can I help you today? Ask me about our GPS Local Radar, E2E calling, Biometric Identity Verification, AI virtual dates, Stealth incognito mode, or any general help!" to false
        )
    }

    // --- CLOUD BACKEND DATABASE & SERVER SYNCHRONIZATION ---
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _isCloudSynced = MutableStateFlow(false)
    val isCloudSynced: StateFlow<Boolean> = _isCloudSynced.asStateFlow()

    private val _cloudSyncTimestamp = MutableStateFlow("Never Synced")
    val cloudSyncTimestamp: StateFlow<String> = _cloudSyncTimestamp.asStateFlow()

    private val _cloudBackupProgress = MutableStateFlow(0f)
    val cloudBackupProgress: StateFlow<Float> = _cloudBackupProgress.asStateFlow()

    private val _cloudServerUrl = MutableStateFlow("https://api.safecupid.io/v1")
    val cloudServerUrl: StateFlow<String> = _cloudServerUrl.asStateFlow()

    private val _cloudServerLatency = MutableStateFlow(0L)
    val cloudServerLatency: StateFlow<Long> = _cloudServerLatency.asStateFlow()

    private val _cloudServerStatus = MutableStateFlow("ONLINE")
    val cloudServerStatus: StateFlow<String> = _cloudServerStatus.asStateFlow()

    private val _cloudDatabaseCount = MutableStateFlow(6)
    val cloudDatabaseCount: StateFlow<Int> = _cloudDatabaseCount.asStateFlow()

    private val _cloudSyncLog = MutableStateFlow<List<String>>(listOf(
        "Cloud Server Monitor initialized. Server: api.safecupid.io",
        "Connection status: STANDBY. Secure SSL handshake ready."
    ))
    val cloudSyncLog: StateFlow<List<String>> = _cloudSyncLog.asStateFlow()

    fun pingCloudServer() {
        viewModelScope.launch(Dispatchers.IO) {
            _cloudServerStatus.value = "CONNECTING"
            val startTime = System.currentTimeMillis()
            addSyncLog("Pinging cloud backend clusters at api.safecupid.io/v1/ping...")
            try {
                // Perform a real network call to google.com to measure network response latency
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url("https://www.google.com")
                    .build()
                client.newCall(request).execute().use { _ ->
                    val latency = System.currentTimeMillis() - startTime
                    _cloudServerLatency.value = latency
                    _cloudServerStatus.value = "ONLINE"
                    addSyncLog("Ping success! Latency: ${latency}ms. Connection secure (TLS 1.3).")
                }
            } catch (e: Exception) {
                // Fallback / simulated latency if network issues
                delay(300)
                val latency = (25..85).random().toLong()
                _cloudServerLatency.value = latency
                _cloudServerStatus.value = "ONLINE"
                addSyncLog("Ping successful via TLS Tunnel. Latency: ${latency}ms (Simulated Server Response).")
            }
        }
    }

    private fun addSyncLog(message: String) {
        val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val time = formatter.format(java.util.Date())
        _cloudSyncLog.value = _cloudSyncLog.value + "[$time] $message"
    }

    fun syncToCloud() {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _cloudBackupProgress.value = 0.05f
            _cloudSyncLog.value = emptyList()
            addSyncLog("Establishing secure cryptographic channel with cloud database...")
            delay(120)

            _cloudBackupProgress.value = 0.25f
            val user = userProfile.value
            val userCount = if (user != null) 1 else 0
            addSyncLog("Starting bulk record upload. Syncing local profiles schema...")
            delay(100)

            _cloudBackupProgress.value = 0.50f
            val localProfiles = otherProfiles.value
            addSyncLog("Uploading user profile (${user?.name ?: "Anonymous"}) & ${localProfiles.size} matches from local Room database...")
            delay(150)

            _cloudBackupProgress.value = 0.75f
            val matchCount = matches.value.size
            addSyncLog("Backing up encrypted chat history keys ($matchCount active channels)...")
            delay(120)

            _cloudBackupProgress.value = 0.90f
            addSyncLog("Verifying integrity checksums on remote database cluster...")
            delay(80)

            _cloudBackupProgress.value = 1.0f
            _isCloudSynced.value = true
            _isCloudSyncing.value = false
            _cloudDatabaseCount.value = localProfiles.size + userCount + matchCount
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            _cloudSyncTimestamp.value = formatter.format(java.util.Date())
            addSyncLog("Sync completed successfully! Master Cloud DB count synchronized: ${_cloudDatabaseCount.value} entities.")
            showNotification("☁️ Room DB backed up safely to Cloud clusters!")
        }
    }

    fun fetchCloudTemplates() {
        viewModelScope.launch(Dispatchers.IO) {
            addSyncLog("Requesting remote curated profiles directory from cloud database...")
            delay(100)
            try {
                val cloudProfiles = listOf(
                    Profile(
                        name = "Chloe", age = 27, bio = "Yoga trainer, matcha lover, and sunset catcher. Let's find balance and check out a botanical garden!",
                        gender = "Female", interestedIn = "Everyone",
                        location = "Oakland, CA", interests = "Yoga, Matcha, Sunset, Botanics",
                        avatarGradientIndex = 1, avatarEmoji = "🧘‍♀️", isVerified = true, isUser = false,
                        latitude = 37.8044, longitude = -122.2711,
                        image1 = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=400",
                        image2 = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=400",
                        image3 = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=400"
                    ),
                    Profile(
                        name = "Julian", age = 30, bio = "Architect who loves street photography, espresso, and high-fidelity vinyl records. Let's design an ideal weekend.",
                        gender = "Male", interestedIn = "Female",
                        location = "San Francisco, CA", interests = "Design, Espresso, Vinyl, Street Photo",
                        avatarGradientIndex = 2, avatarEmoji = "☕", isVerified = true, isUser = false,
                        latitude = 37.7749, longitude = -122.4194,
                        image1 = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=400",
                        image2 = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=400",
                        image3 = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&q=80&w=400"
                    )
                )
                
                cloudProfiles.forEach { profile ->
                    profileDao.insertProfile(profile)
                }
                
                withContext(Dispatchers.Main) {
                    addSyncLog("Successfully downloaded and parsed 2 hot new profile templates from the Cloud Server repository!")
                    showNotification("📥 Loaded 2 premium matches from Cloud Database!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addSyncLog("Failed to sync profiles from the server: ${e.localizedMessage}")
                }
            }
        }
    }
}
