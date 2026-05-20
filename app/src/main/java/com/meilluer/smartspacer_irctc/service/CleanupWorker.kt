package com.meilluer.smartspacer_irctc.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.meilluer.smartspacer_irctc.Target
import com.meilluer.smartspacer_irctc.data.TicketRepository

class CleanupWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        TicketRepository.reset()
        
        val preferenceManager = com.meilluer.smartspacer_irctc.data.PreferenceManager(applicationContext)
        preferenceManager.saveTicketInfo(null)
        
        // Notify Smartspacer
        SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
        
        // Stop periodic updates
        WorkManager.getInstance(applicationContext).cancelUniqueWork("LiveTrainUpdate")
        
        return Result.success()
    }
}
