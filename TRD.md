# Technical Requirements Document (TRD)

**Project Name:** Find Correct Premium (Android Matchmaking & Reels Platform)  
**Document Version:** 1.0.0  
**Status:** Approved  
**Author:** AI Coding Agent  

---

## 1. System Architecture & Component Diagram

The application implements a clean **MVVM (Model-View-ViewModel)** architectural pattern. It is structured entirely within a single-module, single-activity Android codebase using **Jetpack Compose** for a modern, hardware-accelerated declarative UI. Local offline persistence is managed securely by **Room Database (SQLite)**, and remote AI intelligence is provided by Google's **Gemini REST API Client**.

```
+-----------------------------------------------------------------------------------+
|                                     UI LAYER                                      |
|    DatingApp.kt (MainHub, SwipeDeckScreen, ChatsScreen, AIRecommendationsScreen)  |
|    ReelsScreen.kt (ReelsScreen, CreatorStudio, MusicHub, StickerPolls)            |
|    UPIPaymentDialog.kt (Sandboxed Mock UPI Transactions)                          |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
|                                  VIEWMODEL LAYER                                  |
|                             DatingViewModel.kt                                    |
|   - Manages UI StateFlows (Profiles, Messages, CustomReels, Notifications)         |
|   - Coordinates database reads/writes via CoroutineScopes & IO Dispatcher        |
|   - Integrates Gemini API request-response pipelines                              |
+-----------------------------------------------------------------------------------+
                                   │           │
                     ┌─────────────┘           └─────────────┐
                     ▼                                       ▼
+-----------------------------------------+ +---------------------------------------+
|               DATA LAYER                | |             NETWORK LAYER             |
|   Database.kt (RoomDatabase, DAOs)      | |   GeminiClient.kt (REST Pipeline)     |
|   Profile.kt (Profile, Match, Message,  | |   - GenerateContentRequest/Response   |
|               GroupChat, CustomReel)    | |   - API Key Injection via BuildConfig |
+-----------------------------------------+ +---------------------------------------+
```

---

## 2. Database Schema (Room Persistence)

All application data is securely persisted inside a local SQLite database using Jetpack Room. The schema contains five primary entities, with strict transactional constraints.

### 2.1 Entity: `Profile`
Stores comprehensive information representing both local and matched users.
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `name`: `String`
*   `age`: `Int`
*   `bio`: `String`
*   `location`: `String`
*   `interests`: `String` (Comma-separated tagging)
*   `isVerified`: `Boolean` (Glowing blue check verification badge)
*   `isUser`: `Boolean` (Flags the current local client account)
*   `trustScore`: `Int` (Calculated safety score, 0 to 100)
*   `moodBadge`: `String` (Active vibe: *☕ Cafe Chat, 🍷 Wine Date, Movie Night, etc.*)
*   `datingGoal`: `String` (Relationship alignment capsule: *Serious Match, Casual, etc.*)
*   `image1`: `String` (Primary avatar URL or background path)
*   `image2`: `String` (Secondary image path)
*   `avatarEmoji`: `String` (Emoji representing user visual placeholder)
*   `avatarGradientIndex`: `Int` (Selected color gradient identifier)

### 2.2 Entity: `Match`
Represents established bilateral relationships.
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `userId`: `Int` (Foreign Key pointing to standard user)
*   `matchedUserId`: `Int` (Foreign Key pointing to matched creator)
*   `timestamp`: `Long` (Defaulting to system epoch timestamp)

### 2.3 Entity: `Message`
Maintains individual message records. Supports normal text communication and rich visual attachment blocks.
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `matchId`: `Int` (References parent Match entry)
*   `senderId`: `Int`
*   `receiverId`: `Int`
*   `content`: `String` (Text or media identifier prefix e.g. `🎬 Shared Reel:`)
*   `timestamp`: `Long`
*   `isRead`: `Boolean` (Tracks reading telemetry status)

### 2.4 Entity: `CustomReel`
Contains dynamic user-published media metadata representing active video content.
*   `id`: `Int` (Primary Key, AutoGenerate)
*   `creatorId`: `Int` (References publisher's Profile)
*   `backgroundUrl`: `String` (Static visual backing vector)
*   `stickerQuestion`: `String` (Polling text)
*   `stickerOptionA`: `String` (Poll choice A)
*   `stickerOptionB`: `String` (Poll choice B)
*   `soundTitle`: `String` (Active track)
*   `soundArtist`: `String` (Composer text)
*   `soundEmoji`: `String` (Audio theme icon)
*   `votesA`: `Int` (Poll results counting)
*   `votesB`: `Int` (Poll results counting)
*   `hasVoted`: `Boolean` (Saves polling state of user)

---

## 3. UI and Screen Composition Layer

The application utilizes declarative compose components nested inside an edge-to-edge configured `MainActivity`.

### 3.1 Primary Composable Hierarchy
*   `DatingApp(viewModel: DatingViewModel)`: Root composition containing the primary app Scaffold, top dynamic app bar, bottom navigation rails/bars, floating assistive customer support bubbles, and overlay modal screens.
*   `MainHub`: Controls screen-swapping states via a simple observable String parameter (`deck`, `reels`, `recommendations`, `chats`, `settings`, `premium`).

### 3.2 Immersive Reels Layout Layout Mechanics (`ReelsScreen.kt`)
The short-form feed mimics highly responsive horizontal-vertical content containers:
1.  **Vertical Swipe Container:** Accommodates stacked video segments with on-scroll page-snapping.
2.  **Right-Side Utility Stack:** Styled vertically, with custom spacing configured tightly (`Arrangement.spacedBy(6.dp)`) to avoid clutter and maximize screen space.
3.  **Vinyl Rotation Algorithm:** Updates continuous layout transformations of the musical disc icon.
    *   ```kotlin
        val discRotation by animateFloatAsState(
            targetValue = if (isPaused) 0f else (System.currentTimeMillis() % 3600 / 10).toFloat(),
            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
        )
        ```

---

## 4. Algorithmic and Decision Logic

### 4.1 gestural Deck Filtering & Sorting
Instead of standard sequential indexes, the main match deck processes profiles in real-time based on active filters selected in the visual horizontal chips.

```kotlin
val filteredProfiles = remember(profiles, selectedMoodFilter, selectedGoalFilter) {
    profiles.filter { profile ->
        val matchMood = selectedMoodFilter == "All" || profile.moodBadge == selectedMoodFilter
        val matchGoal = selectedGoalFilter == "All" || profile.datingGoal == selectedGoalFilter
        matchMood && matchGoal
    }
}
```

### 4.2 Instant Boost Match (Algorithm)
Selecting the Purple Spark (⚡) button executes a transaction that guarantees connectivity:
1.  Bypasses the random percentage generator (`Math.random() < 0.85`).
2.  Writes a new `Match` entity record to the local SQLite database.
3.  Injects a specialized introductory greeting utilizing the target profile's first interest.
4.  Pushes an overlay match alert accompanied by localized confetti showers.

---

## 5. Security Protocols & Networking

### 5.1 Biometric Authentication Simulation
Uses high-precision Compose canvas drawing loops to mimic modern visual scanning:
*   Generates a glowing sweep vector moving vertically through sinusoidal mathematical curves.
*   Calculates a synthetic digital signature match to verify the user account profile.

### 5.2 Gemini REST Integration
Network calls bypass bulky SDK dependencies by communicating via native JSON payloads built on asynchronous Kotlin Coroutine `Ktor` / `HttpURLConnection` integrations.
*   **Security Principle:** API keys are dynamically populated in compiling environments via the gradle configuration (`BuildConfig.GEMINI_API_KEY`), ensuring zero sensitive tokens are committed to source files.
