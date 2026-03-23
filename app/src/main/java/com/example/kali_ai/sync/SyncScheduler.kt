package com.example.kali_ai.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncScheduler {
    
    // Silent background sync every 15 minutes
    fun scheduleSilentSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SilentSyncWorker>(
            15, TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            10, TimeUnit.MINUTES
        )
        .addTag("kali_sync")
        .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "kali_silent_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    // Immediate sync when internet comes back
    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SilentSyncWorker>()
            .setConstraints(constraints)
            .addTag("kali_immediate_sync")
            .build()
        
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
    
    // Cancel all sync work
    fun cancelAllSync(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("kali_sync")
    }
}
