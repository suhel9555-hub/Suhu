package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: Int,
    val bio: String,
    val gender: String, // "Male", "Female", "Non-binary"
    val interestedIn: String, // "Male", "Female", "Everyone"
    val location: String,
    val interests: String, // Comma separated, e.g. "Hiking, Coffee, Tech"
    val avatarGradientIndex: Int, // 0 to 5 for unique beautiful gradients
    val avatarEmoji: String, // Icon representation
    val isVerified: Boolean = true,
    val isUser: Boolean = false,
    val isFakeFlagged: Boolean = false,
    val fakeAnalysis: String? = null,
    val trustScore: Int = 85, // Correctness/Trust Score (0-100)
    val moodBadge: String = "☕ Cafe Chat", // Vibrant dynamic Mood
    val datingGoal: String = "💍 Serious Match", // Dating intent goal
    val isIncognito: Boolean = false, // Security: Private stealth mode
    val pinLocked: Boolean = false, // Security: Account lock active
    val premiumTier: String = "None", // "None", "Premium" ($9), "Premium Pro" ($199)
    val latitude: Double = 37.7749, // GPS Lat
    val longitude: Double = -122.4194, // GPS Long
    val image1: String = "", // Profile Image Slot 1 (Required)
    val image2: String = "", // Profile Image Slot 2 (Required)
    val image3: String = ""  // Profile Image Slot 3 (Required)
) : Serializable {
    val interestList: List<String>
        get() = if (interests.isBlank()) emptyList() else interests.split(",").map { it.trim() }
}

@Entity(tableName = "matches")
data class Match(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val matchedUserId: Int,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isModerated: Boolean = false,
    val moderationWarning: String? = null
) : Serializable

@Entity(tableName = "group_chats")
data class GroupChat(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val avatarGradientIndex: Int,
    val avatarEmoji: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "group_messages")
data class GroupMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val senderId: Int, // 0 for the user, otherwise profile ID
    val senderName: String,
    val senderEmoji: String,
    val senderGradientIndex: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

data class CustomReel(
    val id: Int,
    val authorName: String,
    val authorEmoji: String,
    val authorGradientIndex: Int,
    val backgroundUrl: String,
    val caption: String,
    val hashtags: String,
    val songTitle: String,
    val songArtist: String,
    val songEmoji: String,
    val effectFilter: String = "None", // "None", "Vintage Glow 🎞️", "Neon Cyberpunk 🌌", "Warm Sunset 🌅", "Lofi B&W 🖤", "Rainbow Vibe 🌈"
    val playSpeed: Float = 1.0f,
    val stickerType: String = "None", // "None", "Poll", "QA"
    val stickerQuestion: String = "",
    val stickerOptionA: String = "",
    val stickerOptionB: String = "",
    val pollVotesA: Int = 0,
    val pollVotesB: Int = 0,
    val isLikedByMe: Boolean = false,
    val likesCount: Int = 100,
    val profileId: Int? = null
) : Serializable


