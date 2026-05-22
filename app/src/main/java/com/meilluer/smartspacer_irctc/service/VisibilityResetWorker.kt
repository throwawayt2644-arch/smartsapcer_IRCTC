package com.meilluer.smartspacer_irctc.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.meilluer.smartspacer_irctc.Target
import com.meilluer.smartspacer_irctc.data.PreferenceManager
import com.meilluer.smartspacer_irctc.data.TicketRepository

class VisibilityResetWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val preferenceManager = PreferenceManager(applicationContext)
        preferenceManager.saveVisibility(false)
        TicketRepository.target_visibility_flag = false
        
        // Notify Smartspacer
        SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
        
        return Result.success()
    }
}
