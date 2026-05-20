package com.meilluer.smartspacer_irctc.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.meilluer.smartspacer_irctc.Target
import com.meilluer.smartspacer_irctc.data.TicketRepository
import java.util.concurrent.TimeUnit

class VisibilityFlagWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        TicketRepository.target_visibility_flag = true

        // Notify Smartspacer
        SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
        
        // Start periodic live updates (every 30 mins)
        val updateRequest = PeriodicWorkRequestBuilder<LiveUpdateWorker>(30, TimeUnit.MINUTES)
            .build()
        
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "LiveTrainUpdate",
            ExistingPeriodicWorkPolicy.REPLACE,
            updateRequest
        )
        
        return Result.success()
    }
}
