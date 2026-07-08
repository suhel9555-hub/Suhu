# Product Requirements Document (PRD)

## Project Name: Find Correct Premium
**Document Version:** 1.0.0  
**Status:** Active  
**Author:** AI Coding Agent  

---

## 1. Executive Summary & Product Vision

### 1.1 Vision Statement
**Find Correct Premium** is a next-generation, high-fidelity social matchmaking and video-discovery platform that redefines digital dating. By blending modern interaction paradigms (short-form Reels) with highly detailed compatibility metrics and deep personality mapping, the application empowers users to discover genuine connections. It bridges the gap between shallow swiping and authentic communication through dynamic personality filtering, smart safety guards, real-time AI assistance, and high-impact immersive media.

### 1.2 Core Value Propositions
*   **Vibe-Driven Discovery:** Match based on active moods (e.g., *☕ Cafe Chat, 🍿 Movie Night, 🏔️ Outdoorsy*) and explicit dating goals (e.g., *💍 Serious Match, 🥂 Casual Fun*).
*   **Creative Shorts Integration:** A TikTok/Instagram-style "Reels Screen" where creators display their real personalities, complete with integrated equalizer presets, interactive stickers, Q&As, and custom background scoring.
*   **AI-Engineered Safety & Compatibility:** Automatic profile integrity scans, biometric verification simulations, and real-time AI profile analyzers powered by Google's Gemini API.
*   **Comprehensive Privacy:** Complete client-side security via custom Pin-Lock layers.

---

## 2. Target Audience & Personas

*   **Persona A: "The Intentional Dater" (Emily, 27)**  
    *   *Need:* Tired of endless mindless swiping and superficial profile bios. Wants to align matches with exact relationship standards and active weekly schedules.  
    *   *Usage:* Filters the main swipe deck by **Serious Match** or **Cafe Chat** and uses **Profile Analyzer** to evaluate compatibility.
*   **Persona B: "The Creative Creator" (Julian, 30)**  
    *   *Need:* Prefers communicating visually through short-form content and audio. Wants to share hobbies (photography, vinyl records) directly.  
    *   *Usage:* Interacts in the **Reels Creator Studio**, custom-configures sound overlays, adds polling stickers, and checks engagement via interactive Q&As.

---

## 3. Product Features & Detailed Functional Scope

The system is architected into five central modules.

### 3.1 Swipe Deck Screen (Home Matchmaking Portal)
The core visual landing page implements a highly responsive gestural card deck with deep filtering capabilities.

*   **Vibe Filters Row:** A horizontal scrolling `LazyRow` featuring distinct mood types:
    1.  *✨ All Vibes* (Default)
    2.  *☕ Cafe Chat*
    3.  *🍷 Wine Date*
    4.  *🍿 Movie Night*
    5.  *🏔️ Outdoorsy*
    6.  *🎮 Gaming Duo*
*   **Dating Goal Row:** A secondary horizontal `LazyRow` allowing goal alignment selection:
    1.  *🎯 All Goals*
    2.  *💍 Serious Match*
    3.  *🥂 Casual Fun*
    4.  *✨ Just Friends*
    5.  *🧩 Chat & See*
*   **Five-Button Decision Deck:** Fixed at the screen base, allowing complete gestural control:
    1.  **Undo/Rewind (Yellow ↺):** Recalls the previously bypassed or swiped profile, modifying indexes gracefully.
    2.  **Pass (Red ✕):** Rejects the current profile card and transitions seamlessly to the next card.
    3.  **Instant Boost Match (Purple Gradient ⚡):** A premium action that bypasses standard matching percentages, immediately writing a match record and triggering a welcoming, personalized icebreaker message in chats.
    4.  **SuperLike (Teal ⭐️):** Triggers custom celebratory confetti explosions and guarantees higher match priority.
    5.  **Like (Vibrant Green ♥):** Registers a positive standard swipe with responsive card-slide animations.

---

### 3.2 Reels Screen (Immersive Short-Video Ecosystem)
An immersive, full-screen vertical video feed allowing users to discover personalities via visual storytelling.

*   **Vertical Swipe Feed:** High-fidelity video panels supporting play/pause on-tap gestures.
*   **Immersive Media Overlay (TikTok/Instagram Design):** Includes username, glowing creator verified badges, customized dating goals, dynamic custom background cover graphics, and active song title/artist text banners.
*   **Sound Control Hub:**
    *   *Mute/Unmute:* Quick-action speaker toggles with distinct color cues.
    *   *Music Disc:* A continuously rotating vinyl avatar disc representing active track playing. Tapping launches the **Music Hub Dialog**.
    *   *Equalizer Presets:* Users can switch between acoustic profiles (e.g., *Bass Boost, Vocal Clarity, Studio Pure*).
*   **Interactive Engagement Overlays:**
    *   *Polling Stickers:* Real-time user votes with dynamic percentage bars and haptic confetti feedback.
    *   *Creator Q&A:* Allows direct question submittals, creating structured creator-to-user channels.
*   **Creator Studio Panel:** A custom pop-up drawer allowing creators to build custom reels:
    *   Select backgrounds, edit cover emojis, configure polling sticker questions, choose sound tracks, and instantly publish reels to the live database feed.

---

### 3.3 AI Recommendations & Deep Profile Analysis
Under the hood, server-side algorithms analyze personality matrices.

*   **Gemini AI Compatibility Engine:** Calculates customized compatibility scores (0–100%) on-the-fly and generates targeted conversation starters (icebreakers) based on mutual interests.
*   **AI Profile Analyzer Form:** Evaluates bio descriptions, warns against potential red flags, and recommends optimizing profile titles and images for high engagement.
*   **Comprehensive Profile Builder:** Multi-step wizard to edit location, professional field, core personality tags, bio data, and dating preferences.

---

### 3.4 Trust & Security Safeguards
Robust protocols designed to keep the platform respectful, private, and secure.

*   **Privacy PIN Lock:** An edge-to-edge secure screen demanding a 4-digit PIN to open the application, safeguarding personal messages.
*   **Biometric Verification Portal:** Simulated multi-layered biometric visual scans checking facial authenticity, awarding glowing "Verified" badges to accounts.
*   **Profile Integrity Scanner:** Scans user bios and chat history for compliance, identifying potential scammers or policy violations.
*   **Security & Safety Carousel:** Slides with essential tips regarding internet safety, secure payments, and physical meetup protocols.

---

### 3.5 System Admin & Support Infrastructure
*   **System Admin Portal:** Hidden panel with capabilities to seed default high-quality profiles, reset database tables, view cloud synchronization telemetry logs, and wipe local data.
*   **AI Support Chat Panel:** Instant customer assistance dialog with automated responses powered by conversational AI.

---

## 4. Technical Architecture & Tech Stack

### 4.1 Frameworks & Libraries
*   **Platform:** Native Android (Kotlin DSL).
*   **UI Framework:** Jetpack Compose (Material Design 3 Theme).
*   **Architecture Pattern:** Model-View-ViewModel (MVVM) utilizing `StateFlow` and `CoroutineScope`.
*   **Data Persistence Database:** Room DB (SQLite wrapper) for highly secure local client-side offline storage.
*   **AI Models Integration:** Google Gemini API integration for intelligent conversation recommendations, compatibility forecasts, and safety auditing.

### 4.2 Data Models Entity Schema
*   `Profile`: Stores user bio, age, verified status, mood badges, dating goals, custom avatars, and interests list.
*   `Match`: Links matching users with specific timestamps.
*   `Message`: Stores standard text and custom "Shared Reel" attachments to display rich media inside chat screens.
*   `CustomReel`: Holds video background metadata, stickers, song descriptions, and engagement states.

---

## 5. Security & Privacy Compliance

*   **Client Data Safety:** All message histories and profile settings are stored securely within the local SQLite Room DB database.
*   **Payment Operations:** All Mock premium subscriptions run through a strictly secure local sandboxed UPI Payment Flow validating payment steps and VPA handles safely.
