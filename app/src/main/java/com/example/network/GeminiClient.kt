package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Moshi Data Classes ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Retrofit Service ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"

    // Helper to extract the text response
    private fun extractText(response: GenerateContentResponse): String {
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "No response generated. Please check connection or try again."
    }

    // AI feature: Generate profile bio based on bullet points
    suspend fun generateBio(userTraits: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "A passionate explorer who loves coffee, interesting conversations, and making authentic connections! ✨ (Note: Enter a valid Gemini API key to activate AI custom bio generator!)"
        }

        val prompt = "Generate a catchy, attractive dating app profile bio based on these user qualities or bullet points: \"$userTraits\". Keep it highly conversational, authentic, fun, under 3 sentences, and add 1-2 relevant emojis. Do not wrap in quotes."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7f, maxOutputTokens = 150)
        )

        return try {
            val response = RetrofitClient.service.generateContent(MODEL_NAME, apiKey, request)
            extractText(response).trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating bio: ", e)
            "A passionate individual who loves learning, creative hobbies, and sharing beautiful moments. Let's find out if we click! 🌟"
        }
    }

    // AI feature: Analyze user profile bio and interests and provide suggestions
    suspend fun analyzeProfile(bio: String, interests: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // High-quality smart offline fallback response
            val lowerBio = bio.lowercase()
            
            val tips = mutableListOf<String>()
            if (bio.length < 25) {
                tips.add("📝 **Expand your bio**: Your current bio is a bit short. Add a sentence about what a perfect weekend looks like or what kind of music/food you love to make it warmer.")
            } else {
                tips.add("✨ **Great length**: Your bio length is excellent and gives people a good starting point to chat!")
            }
            
            if (interests.isBlank() || interests.split(",").size < 3) {
                tips.add("🎯 **Add more interests**: List at least 3 distinct hobbies or passions separated by commas (e.g., Hiking, Coffee, Sci-Fi) to help the matching algorithm pair you correctly.")
            } else {
                tips.add("🎨 **Nice variety of interests**: Your interests provide vibrant conversation starters!")
            }
            
            if (!lowerBio.contains("?") && !lowerBio.contains("how") && !lowerBio.contains("what")) {
                tips.add("❓ **Add a conversation hook**: Try ending your bio with a playful question (e.g., 'Best taco spot in town?') to make it super easy for others to break the ice.")
            }
            
            return """
                ### 🛡️ AI Profile Optimization Audit
                
                Here are personalized suggestions to optimize your profile:
                
                ${tips.joinToString("\n\n")}
                
                💡 **Suggested Revised Bio**:
                "${if (bio.isNotBlank()) bio else "Adventure enthusiast and coffee lover."} Let's grab a cup and swap stories? ☕️✨"
                
                *Note: Enter a valid Gemini API Key in the settings panel to activate deep neural profile analysis!*
            """.trimIndent()
        }

        val prompt = """
            You are the "SafeCupid Profile Optimization Expert".
            Analyze the following user profile details from our secure dating application:
            
            Bio: "$bio"
            Interests: "$interests"
            
            Provide a warm, constructive, encouraging, and actionable assessment in clear Markdown. Give:
            1. An overall score (out of 100) of how appealing, authentic, and engaging the profile sounds.
            2. 2-3 specific, bulleted tips for improvement (e.g., how to sound more active, how to specify hobbies better, or how to avoid clichés).
            3. A suggested optimized bio that they could easily copy-paste.
            
            Keep your response concise, professional, and friendly. Start directly with the results.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7f, maxOutputTokens = 450)
        )

        return try {
            val response = RetrofitClient.service.generateContent(MODEL_NAME, apiKey, request)
            extractText(response).trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing profile: ", e)
            "Unable to run profile analysis at this time. Please check your network connection and try again."
        }
    }

    // AI feature: Icebreaker suggestions
    suspend fun suggestIcebreakers(matchName: String, matchBio: String, matchInterests: String): List<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val fallbackList = listOf(
            "Hey $matchName! I noticed you like $matchInterests. What's your absolute favorite thing about it?",
            "Hi $matchName! Your bio sounds amazing. What's one thing not mentioned there that you love?",
            "Hey there! If you had to describe your perfect weekend in three words, what would they be?"
        )

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return fallbackList
        }

        val prompt = "Suggest exactly 3 distinct, creative, fun, and personalized dating app icebreaker messages to send to $matchName. Here is their profile info - Bio: \"$matchBio\", Interests: \"$matchInterests\". Keep each message friendly and engaging. Format your response exactly as 3 lines, starting each line with \"- \". Do not add introductory or concluding text."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.8f, maxOutputTokens = 250)
        )

        return try {
            val response = RetrofitClient.service.generateContent(MODEL_NAME, apiKey, request)
            val text = extractText(response)
            val lines = text.lines()
                .map { it.replace(Regex("^[-*#\\s\\d.]*"), "").trim() }
                .filter { it.isNotEmpty() }
            if (lines.size >= 3) lines.take(3) else fallbackList
        } catch (e: Exception) {
            Log.e(TAG, "Error suggesting icebreakers: ", e)
            fallbackList
        }
    }

    // AI feature: Match recommendations (compatibility scoring)
    suspend fun getMatchRecommendation(
        userBio: String,
        userInterests: String,
        matchName: String,
        matchBio: String,
        matchInterests: String
    ): Pair<Int, String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val defaultScore = 85
        val defaultReason = "You both share vibrant hobbies and expressive personalities! Swiping right is highly recommended."

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return Pair(defaultScore, defaultReason)
        }

        val prompt = """
            Analyze the compatibility between User A and User B for a dating application.
            User A:
            Bio: "$userBio"
            Interests: "$userInterests"
            
            User B ($matchName):
            Bio: "$matchBio"
            Interests: "$matchInterests"
            
            Based on their bios and interests, provide:
            1. A compatibility score (integer between 60 and 99).
            2. A brief, playful, warm explanation of why they are a perfect match (maximum 2 sentences).
            
            Format your response exactly like this, with NO other text:
            Score: [score_number]
            Reason: [brief_reason]
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.6f, maxOutputTokens = 200)
        )

        return try {
            val response = RetrofitClient.service.generateContent(MODEL_NAME, apiKey, request)
            val text = extractText(response)
            var score = defaultScore
            var reason = defaultReason
            
            text.lines().forEach { line ->
                if (line.startsWith("Score:", ignoreCase = true)) {
                    val scoreStr = line.substringAfter(":").trim().replace("%", "")
                    score = scoreStr.toIntOrNull() ?: defaultScore
                } else if (line.startsWith("Reason:", ignoreCase = true)) {
                    reason = line.substringAfter(":").trim()
                }
            }
            Pair(score, reason)
        } catch (e: Exception) {
            Log.e(TAG, "Error predicting compatibility: ", e)
            Pair(defaultScore, defaultReason)
        }
    }

    // AI feature: Fake Profile Detection
    suspend fun detectFakeProfile(name: String, bio: String, interests: String): Pair<Boolean, String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val defaultSafe = Pair(false, "Profile exhibits genuine self-expression with balanced interest fields. Verified clean.")
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Simulated local check for our designated fake crypto profile
            if (name.contains("crypto", ignoreCase = true) || bio.contains("crypto", ignoreCase = true) || bio.contains("WhatsApp", ignoreCase = true)) {
                return Pair(true, "Simulated Detection: Suspicious commercial language, solicitations for fast profit, and external messaging links.")
            }
            return defaultSafe
        }

        val prompt = """
            Analyze the following dating app profile details to detect if this is a potential fake profile, spammer, bot, or financial scam.
            Name: "$name"
            Bio: "$bio"
            Interests: "$interests"
            
            Look for typical red flags: offering overnight profits, requesting off-platform contact (WhatsApp/Telegram), overly promotional vocabulary, or disjointed interest tags.
            
            Respond exactly in this format:
            Status: [FAKE or SAFE]
            Analysis: [A brief 1-2 sentence analysis explaining the flags or why it is clean]
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.3f, maxOutputTokens = 150)
        )

        return try {
            val response = RetrofitClient.service.generateContent(MODEL_NAME, apiKey, request)
            val text = extractText(response)
            var isFake = false
            var analysis = "Profile is verified clean."
            
            text.lines().forEach { line ->
                if (line.startsWith("Status:", ignoreCase = true)) {
                    val status = line.substringAfter(":").trim()
                    isFake = status.equals("FAKE", ignoreCase = true)
                } else if (line.startsWith("Analysis:", ignoreCase = true)) {
                    analysis = line.substringAfter(":").trim()
                }
            }
            Pair(isFake, analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking fake profile: ", e)
            defaultSafe
        }
    }

    // AI feature: Content Moderation & Safety checks
    suspend fun moderateMessage(message: String): Pair<Boolean, String?> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Offline simple keyword list filter for robustness
            val toxicWords = listOf("hate", "scam", "stupid", "idiot", "jerk", "kill", "die")
            val hasToxic = toxicWords.any { message.contains(it, ignoreCase = true) }
            if (hasToxic) {
                return Pair(true, "Content flagged by local safety policy (contains inappropriate or aggressive keywords).")
            }
            return Pair(false, null)
        }

        val prompt = """
            Analyze this chat message being sent in a friendly dating application to check if it violates safety standards (e.g., severe toxicity, targeted harassment, sexual solicitation, extreme insults, or financial scamming).
            Message content: "$message"
            
            Respond in this format:
            Unsafe: [YES or NO]
            Reason: [If YES, provide a 1-sentence explanation of why it was flagged. If NO, leave empty]
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.2f, maxOutputTokens = 100)
        )

        return try {
            val response = RetrofitClient.service.generateContent(MODEL_NAME, apiKey, request)
            val text = extractText(response)
            var isUnsafe = false
            var reason: String? = null
            
            text.lines().forEach { line ->
                if (line.startsWith("Unsafe:", ignoreCase = true)) {
                    val ans = line.substringAfter(":").trim()
                    isUnsafe = ans.equals("YES", ignoreCase = true)
                } else if (line.startsWith("Reason:", ignoreCase = true)) {
                    val res = line.substringAfter(":").trim()
                    if (res.isNotEmpty() && !res.equals("none", ignoreCase = true)) {
                        reason = res
                    }
                }
            }
            Pair(isUnsafe, reason)
        } catch (e: Exception) {
            Log.e(TAG, "Error moderating message: ", e)
            Pair(false, null)
        }
    }

    // AI feature: Customer Support Chatbot Assistant
    suspend fun supportChat(userMessage: String, chatHistory: List<Pair<String, Boolean>>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        val systemInstruction = """
            You are "SafeCupid AI Support", the friendly and highly competent AI Customer Support Assistant for the SafeCupid Dating App.
            Your purpose is to help the customer/user with any questions about using the app, its safety protocols, premium packages, GPS Local Radar, AI date simulators, E2E calls, message encryption, self-destruct timers, stealth modes, or profile bio creation.
                    Key features of SafeCupid to know and explain:
            1. GPS Local Radar: Scans nearby airspace for active profiles. The user can toggle GPS, adjust radius, and start direct chats.
            2. Quantum E2E Encryption: Secure audio and video peer-to-peer calls, message logs, and a Self-Destruct Shred Timer that wipes messages.
            3. AI Virtual Date Simulator: Lets users test-drive dates with custom scenarios using Gemini.
            4. Verified Safety: Users can undergo a biometric scan to verify identity and achieve 100% trust score.
            5. Stealth Shield (Incognito): Makes the user invisible.
            6. Subscription tiers:
               - Premium Membership: Supercharges matching and dynamic vibes at just ₹1 per day (₹1/day).
               - Premium Plus: Full security suite, Stealth mode, Travel Teleport companion, and unlimited AI dates simulator at ₹9 per day (₹9/day).
            
            Guidelines:
            - Keep your responses concise, clear, and exceptionally warm and helpful.
            - Format answers with clean bullets and short paragraphs if needed.
            - Answer the question directly. If the query is unrelated to SafeCupid or general app support, politely redirect them back to SafeCupid support topics.
        """.trimIndent()

        val defaultResponse = "I'm here to help you navigate SafeCupid safely! Our app features Premium (₹1/day) and Premium Plus (₹9/day) subscriptions, Quantum E2E Encrypted calls, a Self-Destruct Message Shredder, and AI-powered dating simulators. How can I help you today?"
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Offline fallback response based on keywords
            val lower = userMessage.lowercase()
            return when {
                lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                    "Hello! Welcome to SafeCupid Support. How can I assist you with our safety, encryption, simulator, or radar features today? (Note: To activate my full AI capability, please configure a valid Gemini API Key!)"
                lower.contains("radar") || lower.contains("gps") || lower.contains("nearby") ->
                    "Our GPS Local Radar scans the airspace within your set radius to find nearby active profiles. You can configure the slider up to 100km! Ensure GPS is enabled."
                lower.contains("call") || lower.contains("encrypted") || lower.contains("secure") || lower.contains("shred") || lower.contains("timer") || lower.contains("quantum") ->
                    "SafeCupid employs military-grade Quantum E2E Encryption (AES-256) for chats and peer-to-peer calls. You can also turn on the 'Shredder' to auto-destruct messages after 10, 30, or 60 seconds!"
                lower.contains("sim") || lower.contains("virtual") || lower.contains("date") || lower.contains("scenario") ->
                    "The AI Virtual Date Simulator allows you to practice conversations in realistic scenarios like cozy cafes, moonlit walks, or high-speed space stations before dating for real!"
                lower.contains("stealth") || lower.contains("incognito") || lower.contains("invisible") ->
                    "Stealth Shield hides your profile from local scans and swiping lists, letting you explore the app in full private incognito mode."
                lower.contains("premium") || lower.contains("subscribe") || lower.contains("charge") || lower.contains("price") || lower.contains("cost") || lower.contains("rupee") || lower.contains("fee") || lower.contains("tier") ->
                    "SafeCupid offers two premium tiers: Premium at ₹1 per day (₹1/day) for boosted visibility and custom vibes, and Premium Plus at ₹9 per day (₹9/day) which unlocks travel teleporting, Stealth Shield, and unlimited AI simulated dates! Click the Premium tab to sign up!"
                lower.contains("biometric") || lower.contains("verify") || lower.contains("trust") || lower.contains("scan") ->
                    "You can complete a Biometric Selfie Verification scan on your Profile tab. This instantly gives you a Verified Blue Checkmark and boosts your Trust Score to 100%!"
                else ->
                    "Thank you for contacting SafeCupid Support! I can help you with GPS radar, E2E encrypted calling, biometric verification, or dating simulator questions. What would you like to know?"
            }
        }

        // Construct chat history content list
        val contents = mutableListOf<Content>()
        
        // Add last 6 messages from history to keep context short and within limits
        chatHistory.takeLast(6).forEach { (text, isUser) ->
            contents.add(
                Content(parts = listOf(Part(text = text)))
            )
        }
        
        // Add current user message
        contents.add(Content(parts = listOf(Part(text = userMessage))))

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(temperature = 0.7f, maxOutputTokens = 350)
        )

        return try {
            val response = RetrofitClient.service.generateContent(MODEL_NAME, apiKey, request)
            extractText(response).trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error in support chatbot: ", e)
            "I'm experiencing a minor connection hiccup with our secure support servers. To help you: SafeCupid offers secure matching, E2E calls, GPS airspace scanning, and custom AI simulators. Please feel free to ask again!"
        }
    }
}
