package com.example.kali_ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.*

class KaliVoiceService : Service() {
    
    companion object {
        private const val TAG = "KaliVoice"
        private const val WAKE_WORD = "kali"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "kali_voice_channel"
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startListening()
        Log.d(TAG, "🎤 Voice Service Started")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kali Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Always listening for wake word"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Kali AI")
            .setContentText("सुन रहा है...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun startListening() {
        if (isListening) return
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.forEach { text ->
                    Log.d(TAG, "👂 Heard: $text")
                    
                    if (text.lowercase().contains(WAKE_WORD)) {
                        // Wake word detected
                        Log.d(TAG, "⚡ Wake Word Detected!")
                        // Trigger AI - send broadcast or start activity
                        val aiIntent = Intent(this@KaliVoiceService, MainActivity::class.java)
                        aiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        aiIntent.putExtra("wake_word_detected", true)
                        aiIntent.putExtra("voice_input", text)
                        startActivity(aiIntent)
                    }
                }
                
                // Restart listening
                startListening()
            }
            
            override fun onError(error: Int) {
                Log.e(TAG, "❌ Speech Error: $error")
                startListening()
            }
                        override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("hi", "IN"))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer?.startListening(intent)
        isListening = true
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        isListening = false
        Log.d(TAG, "🔇 Voice Service Stopped")
    }
}
