package com.example.keyboard

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class CerebrasApiService {
    private val client = OkHttpClient()

    suspend fun rewriteText(text: String, apiKey: String, mood: Int): String? {
        if (apiKey.isBlank()) return "Error: API Key missing. Please config in Settings."
        
        return withContext(Dispatchers.IO) {
            val moodString = when (mood) {
                1 -> "Professional"
                2 -> "Casual"
                3 -> "Friendly"
                else -> "Normal, clear, and grammatically correct"
            }

            val systemPrompt = """
                You are a keyboard AI typing assistant. The user will provide a text message. 
                Your task is to fix any grammar errors and rewrite the text to match a '$moodString' tone.
                IMPORTANT: Ensure the sentiment or factual meaning remains intact.
                Do NOT answer questions or converse with the user. Output ONLY the rewritten text, nothing else, no quotes, no explanations.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("model", "llama3.1-8b") // Llama 3.1 8B is generally fast and available
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", text)
                    })
                })
                put("temperature", 0.7)
            }

            val request = Request.Builder()
                .url("https://api.cerebras.ai/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        return@withContext message.getString("content").trim()
                    }
                } else {
                    Log.e("CerebrasApi", "Error: ${response.code} - $responseBody")
                    return@withContext "Error: Could not connect to AI. Status ${response.code}"
                }
            } catch (e: Exception) {
                Log.e("CerebrasApi", "Exception during AI call", e)
                return@withContext "Error: Failed to reach AI service."
            }
            null
        }
    }
}
