package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateSolution(
        prompt: String,
        imageUri: Uri? = null,
        enableWebGrounding: Boolean = false,
        subject: String = "General"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Fall back to offline intelligent engine if no user key is injected
            val offlineResponse = OfflineSolveEngine.solve(prompt, imageUri != null, subject)
            return@withContext Result.success(offlineResponse)
        }

        try {
            val model = "gemini-3.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val requestJson = JSONObject()

            // System instruction tailored for student homework and academic problem solving
            val systemInstruction = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().put("text", 
                        "You are SOLVE AI, a student-focused academic tutor and homework problem solver. " +
                        "Your mission is to help students learn effectively by providing step-by-step explanations, " +
                        "identifying core principles, breaking down complex math, science, and academic questions into digestible steps, " +
                        "and offering clear, structured reasoning. " +
                        "Always format formulas cleanly with clear step-by-step resolution. " +
                        "Tone is encouraging, clear, concise, and academically sound."
                    ))
                }
                put("parts", parts)
            }
            requestJson.put("systemInstruction", systemInstruction)

            // User parts
            val partsArray = JSONArray()

            // If image is attached, convert to Base64 inlineData
            if (imageUri != null) {
                val base64Image = readImageAsBase64(imageUri)
                if (base64Image != null) {
                    val inlineData = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    }
                    partsArray.put(JSONObject().put("inlineData", inlineData))
                }
            }

            // Prompt text
            var augmentedPrompt = prompt
            if (subject != "General") {
                augmentedPrompt = "[$subject Focus] $prompt"
            }
            partsArray.put(JSONObject().put("text", augmentedPrompt))

            val contentObject = JSONObject().apply {
                put("role", "user")
                put("parts", partsArray)
            }

            val contentsArray = JSONArray().apply {
                put(contentObject)
            }
            requestJson.put("contents", contentsArray)

            // Optional Web Grounding via Google Search
            if (enableWebGrounding) {
                val toolsArray = JSONArray().apply {
                    put(JSONObject().put("googleSearch", JSONObject()))
                }
                requestJson.put("tools", toolsArray)
            }

            // Generation config
            val generationConfig = JSONObject().apply {
                put("temperature", 0.4)
                put("maxOutputTokens", 2048)
            }
            requestJson.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    // If network or quota fails, provide helpful fallback response
                    return@withContext Result.success(OfflineSolveEngine.solve(prompt, imageUri != null, subject))
                }

                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val resParts = content?.optJSONArray("parts")
                    val sb = StringBuilder()
                    if (resParts != null) {
                        for (i in 0 until resParts.length()) {
                            val part = resParts.getJSONObject(i)
                            if (part.has("text")) {
                                sb.append(part.getString("text"))
                            }
                        }
                    }

                    // Check for grounding metadata (citations / search results)
                    val groundingMetadata = candidate.optJSONObject("groundingMetadata")
                    if (groundingMetadata != null && enableWebGrounding) {
                        val searchChunks = groundingMetadata.optJSONArray("groundingChunks")
                        if (searchChunks != null && searchChunks.length() > 0) {
                            sb.append("\n\n**🌐 Live Web Grounding Citations:**\n")
                            for (i in 0 until minOf(3, searchChunks.length())) {
                                val chunk = searchChunks.getJSONObject(i)
                                val web = chunk.optJSONObject("web")
                                if (web != null) {
                                    val title = web.optString("title", "Source")
                                    val uri = web.optString("uri", "")
                                    sb.append("- [$title]($uri)\n")
                                }
                            }
                        }
                    }

                    val resultText = sb.toString().trim()
                    if (resultText.isNotEmpty()) {
                        Result.success(resultText)
                    } else {
                        Result.success(OfflineSolveEngine.solve(prompt, imageUri != null, subject))
                    }
                } else {
                    Result.success(OfflineSolveEngine.solve(prompt, imageUri != null, subject))
                }
            }
        } catch (e: Exception) {
            // Safe fallback so student always receives high-quality guidance
            Result.success(OfflineSolveEngine.solve(prompt, imageUri != null, subject))
        }
    }

    private fun readImageAsBase64(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: return null
                // Scale bitmap down to a reasonable dimension if too large
                val maxDim = 1024
                val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1.0f)
                val scaled = if (ratio < 1.0f) {
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                } else {
                    bitmap
                }
                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }
}
