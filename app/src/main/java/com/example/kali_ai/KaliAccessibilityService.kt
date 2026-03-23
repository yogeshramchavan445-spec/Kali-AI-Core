package com.example.kali_ai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class KaliAccessibilityService : AccessibilityService() {
    
    companion object {
        @Volatile var instance: KaliAccessibilityService? = null
        private const val TAG = "KaliAccess"
    }
    
    override fun onServiceConnected() {
        instance = this
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.d(TAG, "✅ Accessibility Service Connected")
    }
    
    // Click by Text
    fun clickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return findAndClick(root, text)
    }
    
    private fun findAndClick(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text != null && node.text.toString().contains(text, ignoreCase = true)) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClick(child, text)) return true
        }
        return false
    }    
    // Click by ID
    fun clickById(resourceId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(resourceId)
        if (nodes.isNotEmpty()) {
            nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        return false
    }
    
    // Phone Control Functions
    fun answerCall(): Boolean {
        return clickByText("Answer") || clickByText("स्वीकार") || 
               clickByText("Accept") || clickByText("उठाएं")
    }
    
    fun endCall(): Boolean {
        return clickByText("End") || clickByText("काटें") || 
               clickByText("Reject") || clickByText("अस्वीकार")
    }
    
    fun openApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent?.let { startActivity(it) }
    }
    
    fun scrollDown(): Boolean {
        return rootInActiveWindow?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
    }
    
    fun scrollUp(): Boolean {
        return rootInActiveWindow?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) ?: false
    }
    
    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    fun goRecent() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
    
    fun tapAt(x: Float, y: Float) {        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.StrokeDescription(path, 0, 100)
        dispatchGesture(GestureDescription.Builder().addStroke(gesture).build(), null, null)
    }
    
    fun getScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        return extractText(root)
    }
    
    private fun extractText(node: AccessibilityNodeInfo): String {
        var text = if (node.text != null) node.text.toString() + " " else ""
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            text += extractText(child)
        }
        return text
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "❌ Accessibility Service Destroyed")
    }
}
