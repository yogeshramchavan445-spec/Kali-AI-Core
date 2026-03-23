package com.example.kali_ai.sync

import android.util.Base64
import android.util.Log
import com.example.kali_ai.database.Conversation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class GitHubSyncManager(
    private val token: String,
    private val username: String,
    private val repoName: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val baseUrl = "https://api.github.com/repos/$username/$repoName/contents"
    private val fileName = "kali_conversations.json"
    
    companion object {
        private const val TAG = "GitHubSync"
    }
    
    // Download from GitHub
    suspend fun downloadFromGitHub(): List<Conversation>? {
        return withContext(Dispatchers.IO) {
            try {
                if (token.isEmpty() || username.isEmpty() || repoName.isEmpty()) {
                    Log.e(TAG, "GitHub credentials missing")
                    return@withContext null
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/$fileName")
                    .addHeader("Authorization", "token $token")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {                    val jsonString = response.body?.string() ?: return@withContext null
                    val json = JSONObject(jsonString)
                    val content = json.getString("content")
                    
                    val decodedBytes = Base64.decode(content, Base64.DEFAULT)
                    val decodedString = String(decodedBytes)
                    
                    val type = object : TypeToken<List<Conversation>>() {}.type
                    val conversations = gson.fromJson<List<Conversation>>(decodedString, type)
                    
                    Log.d(TAG, "⬇️ Downloaded ${conversations.size} conversations from GitHub")
                    conversations
                } else {
                    Log.d(TAG, "📭 No file on GitHub yet (404)")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Download error: ${e.message}")
                null
            }
        }
    }
    
    // Upload to GitHub (Silent)
    suspend fun uploadToGitHub(conversations: List<Conversation>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (token.isEmpty() || username.isEmpty() || repoName.isEmpty()) {
                    Log.e(TAG, "GitHub credentials missing")
                    return@withContext false
                }
                
                val sha = getFileSha()
                val jsonString = gson.toJson(conversations)
                val base64Content = Base64.encodeToString(jsonString.toByteArray(), Base64.NO_WRAP)
                
                val jsonBody = JSONObject().apply {
                    put("message", "Auto-sync: ${conversations.size} conversations")
                    put("content", base64Content)
                    if (sha != null) put("sha", sha)
                }
                
                val mediaType = MediaType.parse("application/json")
                val body = RequestBody.create(mediaType, jsonBody.toString())
                
                val request = Request.Builder()
                    .url("$baseUrl/$fileName")
                    .addHeader("Authorization", "token $token")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .put(body)                    .build()
                
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                
                Log.d(TAG, "⬆️ Upload: ${if(success) "✅ Success" else "❌ Failed"} (${response.code})")
                success
            } catch (e: Exception) {
                Log.e(TAG, "❌ Upload error: ${e.message}")
                false
            }
        }
    }
    
    private suspend fun getFileSha(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/$fileName")
                    .addHeader("Authorization", "token $token")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    json.getString("sha")
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    // Check internet
    fun isOnline(): Boolean {
        return try {
            val request = Request.Builder()
                .url("https://github.com")
                .head()
                .timeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
