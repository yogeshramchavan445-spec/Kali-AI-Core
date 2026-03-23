package com.example.kali_ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.kali_ai.ai.AIEngine
import com.example.kali_ai.database.AppDatabase
import com.example.kali_ai.sync.SyncScheduler
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var aiEngine: AIEngine
    private lateinit var etInput: EditText
    private lateinit var tvOutput: TextView
    private lateinit var btnSend: Button
    private lateinit var btnVoice: ImageButton
    private lateinit var btnPermissions: Button
    private lateinit var btnSync: Button
    private lateinit var tvNetworkStatus: TextView
    private lateinit var tvSyncStatus: TextView
    
    private val PERMISSIONS_REQUEST_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Views
        etInput = findViewById(R.id.etInput)
        tvOutput = findViewById(R.id.tvOutput)
        btnSend = findViewById(R.id.btnSend)
        btnVoice = findViewById(R.id.btnVoice)
        btnPermissions = findViewById(R.id.btnPermissions)
        btnSync = findViewById(R.id.btnSync)
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)
        
        // Initialize AI Engine
        aiEngine = AIEngine(this)
                // Setup Click Listeners
        btnSend.setOnClickListener { sendMessage() }
        btnVoice.setOnClickListener { startVoiceInput() }
        btnPermissions.setOnClickListener { checkPermissions() }
        btnSync.setOnClickListener { forceSync() }
        
        // Check Permissions on Start
        checkPermissions()
        updateStatus()
    }
    
    private fun sendMessage() {
        val input = etInput.text.toString().trim()
        if (input.isNotEmpty()) {
            tvOutput.append("\n\nYou: $input")
            tvOutput.append("\nKali: सोच रहा है...")
            
            aiEngine.processCommand(input) { response, command ->
                runOnUiThread {
                    tvOutput.text = tvOutput.text.toString()
                        .replace("\nKali: सोच रहा है...", "")
                    tvOutput.append("\nKali: $response")
                    
                    // Scroll to bottom
                    (tvOutput.parent as ScrollView).fullScroll(View.FOCUS_DOWN)
                    
                    etInput.text.clear()
                    updateStatus()
                }
            }
        }
    }
    
    private fun startVoiceInput() {
        Toast.makeText(this, "बोलना शुरू करें...", Toast.LENGTH_SHORT).show()
        // Voice service already running in background
        // This button is for manual trigger
    }
    
    private fun forceSync() {
        Toast.makeText(this, "GitHub Sync शुरू हुआ...", Toast.LENGTH_SHORT).show()
        SyncScheduler.triggerImmediateSync(this)
    }
    
    private fun updateStatus() {
        lifecycleScope.launch {
            // Network Status
            val isConnected = isNetworkAvailable()
            tvNetworkStatus.text = if (isConnected) "🌐 ऑनलाइन" else "📴 ऑफलाइन"
            tvNetworkStatus.setTextColor(if (isConnected) 0xFF00FF00.toInt() else 0xFFFF0000.toInt())            
            // Database Count
            val db = AppDatabase.getInstance(this@MainActivity)
            val count = db.conversationDao().getCount()
            tvSyncStatus.text = "📊 DB: $count"
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS
        )
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
        
        // Check Accessibility Service
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "⚠️ Accessibility Service चालू करें", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        
        updateStatus()
    }
    
    private fun isAccessibilityEnabled(): Boolean {
        val serviceName = "$packageName/${KaliAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabled.contains(serviceName)
    }
        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            updateStatus()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        aiEngine.destroy()
    }
}
