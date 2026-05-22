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
        val departureString = "${ticket.boardingDate} ${ticket.departureTime}"
        val arrivalString = "${ticket.arrivalDate.ifEmpty { ticket.boardingDate }} ${ticket.arrivalTime}"
        val format = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.ENGLISH)
        
        try {
            val departureDate = format.parse(departureString) ?: return
            val arrivalDate = format.parse(arrivalString) ?: departureDate
            
            val departureMillis = departureDate.time
            val arrivalMillis = arrivalDate.time
            
            val targetMillis = departureMillis - TimeUnit.HOURS.toMillis(4)
            val currentMillis = System.currentTimeMillis()
            
            if (currentMillis < targetMillis) {
                // Future journey, more than 4 hours away
                val delay = targetMillis - currentMillis
                val workRequest = OneTimeWorkRequestBuilder<VisibilityFlagWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()
                
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "VisibilityFlagUpdate",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            } else if (currentMillis < arrivalMillis + TimeUnit.HOURS.toMillis(2)) {
                // Journey is within 4 hours of departure OR currently happening (up to 2 hours after arrival)
                val preferenceManager = com.meilluer.smartspacer_irctc.data.PreferenceManager(context)
                preferenceManager.saveVisibility(true)
                com.meilluer.smartspacer_irctc.data.TicketRepository.target_visibility_flag = true
                
                // Also trigger live updates immediately
                val updateRequest = androidx.work.PeriodicWorkRequestBuilder<com.meilluer.smartspacer_irctc.service.LiveUpdateWorker>(30, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "LiveTrainUpdate",
                    androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                    updateRequest
                )
            } else {
                // Journey is already in the past (more than 2 hours after arrival)
                val preferenceManager = com.meilluer.smartspacer_irctc.data.PreferenceManager(context)
                preferenceManager.saveVisibility(false)
                com.meilluer.smartspacer_irctc.data.TicketRepository.target_visibility_flag = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
