package com.meilluer.smartspacer_irctc.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.meilluer.smartspacer_irctc.data.PreferenceManager

class EmailScanWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val preferenceManager = PreferenceManager(applicationContext)
        val email = preferenceManager.getEmail()
        val password = preferenceManager.getPassword()
        val customSender = preferenceManager.getCustomSender()
        val customSubject = preferenceManager.getCustomSubject()

        if (email == null || password == null) {
            return Result.failure()
        }

        val scanner = EmailScanner()
        val success = scanner.scanEmails(email, password, onlyUnread = true, customSender = customSender, customSubject = customSubject)

        if (success) {
            com.meilluer.smartspacer_irctc.data.TicketRepository.currentTicket?.let {
                preferenceManager.saveTicketInfo(it)
                com.meilluer.smartspacer_irctc.util.TicketScheduler.scheduleVisibilityUpdate(applicationContext, it)
                com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.notifyChange(applicationContext, com.meilluer.smartspacer_irctc.Target::class.java, "IRCTC_ticket")
            }
        }

        return if (success) {
            Result.success()
        } else {
            // We might want to retry later if it failed due to network, 
            // but for now, we'll just say success to not spam retries if no new emails.
            Result.success()
        }
    }
}
