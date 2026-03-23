package com.example.kali_ai.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class OnlineApiManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"
    private val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
    
    companion object {
        private const val TAG = "OnlineAPI"
    }

    // DeepSeek API Call
    suspend fun getDeepSeekAnswer(question: String, apiKey: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isEmpty() || apiKey == "") {
                    Log.e(TAG, "DeepSeek API Key empty")
                    return@withContext null
                }
                
                val jsonBody = JSONObject().apply {
                    put("model", "deepseek-chat")
                    put("messages", JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", question)
                        }
                    ))
                    put("temperature", 0.7)
                    put("max_tokens", 1000)
                }

                val mediaType = MediaType.parse("application/json")
                val body = RequestBody.create(mediaType, jsonBody.toString())

                val request = Request.Builder()
                    .url(DEEPSEEK_URL)                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()

                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val respJson = JSONObject(response.body?.string() ?: "")
                    val choices = respJson.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val answer = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        Log.d(TAG, "✅ DeepSeek Response: ${answer.length} chars")
                        return@withContext answer
                    }
                } else {
                    Log.e(TAG, "❌ DeepSeek Error: ${response.code}")
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "❌ DeepSeek Exception: ${e.message}")
                null
            }
        }
    }

    // Gemini API Call (Backup)
    suspend fun getGeminiAnswer(question: String, apiKey: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isEmpty() || apiKey == "") {
                    Log.e(TAG, "Gemini API Key empty")
                    return@withContext null
                }
                
                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().apply {
                            put("parts", JSONArray().put(
                                JSONObject().apply { put("text", question) }
                            ))
                        }
                    ))
                }

                val mediaType = MediaType.parse("application/json")
                val body = RequestBody.create(mediaType, jsonBody.toString())
                val request = Request.Builder()
                    .url("$GEMINI_URL?key=$apiKey")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val respJson = JSONObject(response.body?.string() ?: "")
                    val candidates = respJson.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val answer = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        Log.d(TAG, "✅ Gemini Response: ${answer.length} chars")
                        return@withContext answer
                    }
                } else {
                    Log.e(TAG, "❌ Gemini Error: ${response.code}")
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Gemini Exception: ${e.message}")
                null
            }
        }
    }
}
