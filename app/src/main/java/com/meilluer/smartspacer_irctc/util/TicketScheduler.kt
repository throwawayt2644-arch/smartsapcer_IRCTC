package com.meilluer.smartspacer_irctc.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.meilluer.smartspacer_irctc.data.TicketInfo
import com.meilluer.smartspacer_irctc.service.VisibilityFlagWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object TicketScheduler {

    fun scheduleVisibilityUpdate(context: Context, ticket: TicketInfo) {
        // IRCTC dates are usually like "13-Dec-2024" and time "19:35"
        val dateTimeString = "${ticket.boardingDate} ${ticket.departureTime}"
        val format = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.ENGLISH)
        
        try {
            val departureDate = format.parse(dateTimeString) ?: return
            val departureMillis = departureDate.time
            
            val targetMillis = departureMillis - TimeUnit.HOURS.toMillis(4)
            val currentMillis = System.currentTimeMillis()
            
            val delay = targetMillis - currentMillis
            
            if (delay > 0) {
                val workRequest = OneTimeWorkRequestBuilder<VisibilityFlagWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()
                
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "VisibilityFlagUpdate",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            } else {
                // If it's already less than 4 hours before departure, turn it on immediately
                com.meilluer.smartspacer_irctc.data.TicketRepository.target_visibility_flag = true
                
                // Also trigger live updates immediately
                val updateRequest = androidx.work.PeriodicWorkRequestBuilder<com.meilluer.smartspacer_irctc.service.LiveUpdateWorker>(30, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "LiveTrainUpdate",
                    androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                    updateRequest
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
