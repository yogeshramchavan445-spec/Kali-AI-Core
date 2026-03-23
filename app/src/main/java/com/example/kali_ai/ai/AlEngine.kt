package com.example.kali_ai.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.kali_ai.BuildConfig
import com.example.kali_ai.database.AppDatabase
import com.example.kali_ai.database.Conversation
import com.example.kali_ai.network.OnlineApiManager
import com.example.kali_ai.sync.GitHubSyncManager
import com.example.kali_ai.sync.SyncScheduler
import kotlinx.coroutines.*
import java.util.*

class AIEngine(private val context: Context) {
    
    private val db = AppDatabase.getInstance(context)
    private val dao = db.conversationDao()
    private val apiManager = OnlineApiManager()
    private val githubSync = GitHubSyncManager(
        BuildConfig.GITHUB_TOKEN,
        BuildConfig.GITHUB_USER,
        BuildConfig.GITHUB_REPO
    )
    
    private var tts: TextToSpeech? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "AIEngine"
    }

    // Offline Commands (Phone Control - Always Works)
    private val offlineCommands = mapOf(
        "लाइट जला" to "cmd_light_on",
        "लाइट बुझा" to "cmd_light_off",
        "कॉल उठा" to "cmd_answer_call",
        "कॉल काट" to "cmd_end_call",
        "बैक जाओ" to "cmd_back",
        "होम जाओ" to "cmd_home",
        "रीसेंट खोलो" to "cmd_recent",
        "व्हाट्सएप खोलो" to "app_whatsapp",
        "कैमरा खोलो" to "app_camera",
        "फोन खोलो" to "app_phone",
        "मैसेज खोलो" to "app_messages",
        "ब्राउज़र खोलो" to "app_browser",
        "यूट्यूब खोलो" to "app_youtube",
        "वॉल्यूम बढ़ा" to "cmd_volume_up",
        "वॉल्यूम कम" to "cmd_volume_down",
        "स्क्रीनशॉट" to "cmd_screenshot",        "सेटिंग्स खोलो" to "app_settings"
    )

    init {
        initTTS()
        initializeSync()
        Log.d(TAG, "🤖 AI Engine Initialized")
    }

    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                tts?.setPitch(0.9f)
                tts?.setSpeechRate(0.9f)
                Log.d(TAG, "✅ TTS Initialized")
            } else {
                Log.e(TAG, "❌ TTS Failed")
            }
        }
    }

    private fun initializeSync() {
        SyncScheduler.scheduleSilentSync(context)
        
        scope.launch {
            if (githubSync.isOnline()) {
                downloadFromGitHub()
            }
        }
    }

    private suspend fun downloadFromGitHub() {
        val conversations = githubSync.downloadFromGitHub()
        conversations?.forEach { conv ->
            val exists = dao.findMatchingConversation(conv.question)
            if (exists == null) {
                dao.insertConversation(conv.copy(syncedToGitHub = true))
                Log.d(TAG, "⬇️ Restored from GitHub: ${conv.question}")
            }
        }
    }

    // Main Process Command Function
    fun processCommand(input: String, callback: (String, String?) -> Unit) {
        scope.launch {
            val lowerInput = input.lowercase().trim()
            var finalAnswer = ""
            var commandCode: String? = null
            var apiSource = "local"
            Log.d(TAG, "📥 Processing: $input")

            // 1. Check Offline Commands (Phone Control)
            val matchedCommand = offlineCommands.entries.find { 
                lowerInput.contains(it.key.lowercase()) 
            }
            
            if (matchedCommand != null) {
                commandCode = matchedCommand.value
                finalAnswer = "ठीक है, ${matchedCommand.key}"
                saveConversation(input, finalAnswer, commandCode, "offline_command")
                speak(finalAnswer)
                Log.d(TAG, "✅ Offline Command: ${matchedCommand.key}")
                withContext(Dispatchers.Main) { callback(finalAnswer, commandCode) }
                return@launch
            }

            // 2. Check Local Database (Previous Conversations)
            val localData = dao.findMatchingConversation(lowerInput)
            if (localData != null) {
                speak(localData.answer)
                Log.d(TAG, "✅ Found in Local DB")
                withContext(Dispatchers.Main) { callback(localData.answer, null) }
                return@launch
            }

            // 3. Online Fallback (DeepSeek → Gemini)
            if (githubSync.isOnline()) {
                speak("एक पल रुकिए...")
                
                var onlineAnswer = apiManager.getDeepSeekAnswer(
                    input, 
                    BuildConfig.DEEPSEEK_KEY
                )
                apiSource = "deepseek"

                if (onlineAnswer.isNullOrBlank()) {
                    onlineAnswer = apiManager.getGeminiAnswer(
                        input, 
                        BuildConfig.GEMINI_KEY
                    )
                    apiSource = "gemini"
                }

                if (!onlineAnswer.isNullOrBlank()) {
                    finalAnswer = onlineAnswer
                    saveConversation(input, finalAnswer, null, apiSource)
                    speak(finalAnswer)
                    Log.d(TAG, "✅ Online Answer from $apiSource")                    withContext(Dispatchers.Main) { callback(finalAnswer, null) }
                } else {
                    val errorMsg = "मुझे इसका जवाब नहीं मिला।"
                    speak(errorMsg)
                    saveConversation(input, errorMsg, null, "online_no_answer")
                    Log.d(TAG, "❌ No Answer from APIs")
                    withContext(Dispatchers.Main) { callback(errorMsg, null) }
                }
            } else {
                val offlineMsg = "ऑफलाइन मोड: मुझे यह नहीं पता। जब इंटरनेट होगा तो सीख जाऊंगा।"
                speak(offlineMsg)
                saveConversation(input, offlineMsg, null, "offline_unknown")
                Log.d(TAG, "📴 Offline - No Answer")
                withContext(Dispatchers.Main) { callback(offlineMsg, null) }
            }
        }
    }

    private fun saveConversation(question: String, answer: String, command: String?, source: String) {
        scope.launch {
            val conv = Conversation(
                question = question,
                answer = answer,
                command = command,
                syncedToGitHub = false,
                source = source
            )
            dao.insertConversation(conv)
            Log.d(TAG, "💾 Saved to Local DB: $question")
            
            if (githubSync.isOnline()) {
                SyncScheduler.triggerImmediateSync(context)
            }
        }
    }

    private fun speak(text: String) {
        withContext(Dispatchers.Main) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        scope.cancel()
        Log.d(TAG, "🔇 AI Engine Destroyed")
    }
}
