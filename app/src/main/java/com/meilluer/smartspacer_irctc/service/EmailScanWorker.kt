package com.meilluer.smartspacer_irctc.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meilluer.smartspacer_irctc.data.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmailScanWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val preferenceManager = PreferenceManager(applicationContext)
        val email = preferenceManager.getEmail()
        val password = preferenceManager.getPassword()
        val geminiApiKey = preferenceManager.getGeminiApiKey() ?: ""
        val customSender = preferenceManager.getCustomSender()
        val customSubject = preferenceManager.getCustomSubject()

        if (email == null || password == null || geminiApiKey.isEmpty()) {
            return@withContext Result.failure()
        }

        val scanner = EmailScanner()
        try {
            val success = scanner.scanEmails(email, password, geminiApiKey, onlyUnread = true, customSender = customSender, customSubject = customSubject)

            if (success) {
                com.meilluer.smartspacer_irctc.data.TicketRepository.currentTicket?.let {
                    preferenceManager.saveTicketInfo(it)
                    com.meilluer.smartspacer_irctc.util.TicketScheduler.scheduleVisibilityUpdate(applicationContext, it)
                    com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.notifyChange(applicationContext, com.meilluer.smartspacer_irctc.Target::class.java, "IRCTC_ticket")
                }
                return@withContext Result.success()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry() // Retry on transient errors like network/API issues
        }

        Result.success()
    }
}
