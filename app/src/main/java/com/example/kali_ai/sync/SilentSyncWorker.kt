package com.example.kali_ai.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kali_ai.BuildConfig
import com.example.kali_ai.database.AppDatabase

class SilentSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SilentSync"
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val dao = db.conversationDao()
            
            val githubSync = GitHubSyncManager(
                BuildConfig.GITHUB_TOKEN,
                BuildConfig.GITHUB_USER,
                BuildConfig.GITHUB_REPO
            )
            
            if (!githubSync.isOnline()) {
                Log.d(TAG, "📴 No internet, skipping sync")
                return Result.retry()
            }
            
            val unsynced = dao.getUnsyncedConversations()
            
            if (unsynced.isEmpty()) {
                Log.d(TAG, "✅ Nothing to sync")
                return Result.success()
            }
            
            val allConversations = dao.getAllConversations().first()
            val success = githubSync.uploadToGitHub(allConversations)
            
            if (success) {
                unsynced.forEach { conv ->
                    dao.markAsSynced(conv.id)
                }
                Log.d(TAG, "✅ Synced ${unsynced.size} conversations to GitHub")
                Result.success()
            } else {
                Log.e(TAG, "❌ GitHub upload failed")
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync error: ${e.message}")
            Result.retry()
        }
    }
}
