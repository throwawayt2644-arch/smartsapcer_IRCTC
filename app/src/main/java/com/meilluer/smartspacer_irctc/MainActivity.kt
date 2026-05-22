package com.meilluer.smartspacer_irctc

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.lifecycle.lifecycleScope
import com.meilluer.smartspacer_irctc.data.PreferenceManager
import com.meilluer.smartspacer_irctc.data.TicketRepository
import com.meilluer.smartspacer_irctc.service.EmailScanWorker
import com.meilluer.smartspacer_irctc.service.EmailScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val emailEdit = findViewById<EditText>(R.id.emailEdit)
        val passwordEdit = findViewById<EditText>(R.id.passwordEdit)
        val geminiApiKeyEdit = findViewById<EditText>(R.id.geminiApiKeyEdit)
        val scanButton = findViewById<Button>(R.id.scanButton)
        val resultText = findViewById<TextView>(R.id.resultText)
        val debugToggle = findViewById<TextView>(R.id.debugToggle)
        val debugLayout = findViewById<android.view.View>(R.id.debugLayout)
        val customSenderEdit = findViewById<EditText>(R.id.customSenderEdit)
        val customSubjectEdit = findViewById<EditText>(R.id.customSubjectEdit)
        val mockButton = findViewById<Button>(R.id.mockButton)
        val showNowButton = findViewById<Button>(R.id.showNowButton)
        val resetButton = findViewById<Button>(R.id.resetButton)
        val applyCustomButton = findViewById<Button>(R.id.applyCustomButton)

        val customTrainNum = findViewById<EditText>(R.id.customTrainNum)
        val customTrainName = findViewById<EditText>(R.id.customTrainName)
        val customFromStation = findViewById<EditText>(R.id.customFromStation)
        val customToStation = findViewById<EditText>(R.id.customToStation)
        val customCoach = findViewById<EditText>(R.id.customCoach)
        val customSeat = findViewById<EditText>(R.id.customSeat)

        val preferenceManager = PreferenceManager(this)
        emailEdit.setText(preferenceManager.getEmail())
        passwordEdit.setText(preferenceManager.getPassword())
        geminiApiKeyEdit.setText(preferenceManager.getGeminiApiKey())
        customSenderEdit.setText(preferenceManager.getCustomSender())
        customSubjectEdit.setText(preferenceManager.getCustomSubject())

        // Load existing ticket info
        val existingTicket = preferenceManager.getTicketInfo()
        if (existingTicket != null) {
            TicketRepository.currentTicket = existingTicket
            TicketRepository.target_visibility_flag = preferenceManager.getVisibility()
            displayTicketInfo(resultText, existingTicket)
            
            // Pre-fill custom fields with current ticket if exists
            customTrainNum.setText(existingTicket.trainNumber)
            customTrainName.setText(existingTicket.trainName)
            customFromStation.setText(existingTicket.fromStation)
            customToStation.setText(existingTicket.toStation)
            customCoach.setText(existingTicket.coachNumber)
            customSeat.setText(existingTicket.seatNumber)
        }

        resetButton.setOnClickListener {
            preferenceManager.clearAll()
            TicketRepository.reset()
            
            emailEdit.setText("")
            passwordEdit.setText("")
            geminiApiKeyEdit.setText("")
            customSenderEdit.setText("")
            customSubjectEdit.setText("")
            customTrainNum.setText("")
            customTrainName.setText("")
            customFromStation.setText("")
            customToStation.setText("")
            customCoach.setText("")
            customSeat.setText("")
            resultText.text = "All data cleared."
            
            com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
            Toast.makeText(this, "All variables and preferences reset.", Toast.LENGTH_SHORT).show()
        }

        applyCustomButton.setOnClickListener {
            val ticket = com.meilluer.smartspacer_irctc.data.TicketInfo(
                trainNumber = customTrainNum.text.toString().ifEmpty { "12345" },
                trainName = customTrainName.text.toString().ifEmpty { "CUSTOM EXPRESS" },
                fromStation = customFromStation.text.toString().ifEmpty { "START (ST)" },
                toStation = customToStation.text.toString().ifEmpty { "END (ED)" },
                boardingDate = "01-Jan-2027",
                departureTime = "10:00",
                arrivalTime = "22:00",
                coachNumber = customCoach.text.toString().ifEmpty { "B1" },
                seatNumber = customSeat.text.toString().ifEmpty { "25" },
                seatType = "LB",
                journeyStarted = true
            )

            TicketRepository.currentTicket = ticket
            TicketRepository.target_visibility_flag = true
            preferenceManager.saveTicketInfo(ticket)
            preferenceManager.saveVisibility(true)

            com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
            displayTicketInfo(resultText, ticket)
            Toast.makeText(this, "Custom ticket applied.", Toast.LENGTH_SHORT).show()
        }

        mockButton.setOnClickListener {
            val current = TicketRepository.currentTicket ?: com.meilluer.smartspacer_irctc.data.TicketInfo(
                trainNumber = "12345",
                trainName = "MOCK EXPRESS",
                fromStation = "START (ST)",
                toStation = "END (ED)",
                boardingDate = "01-Jan-2027",
                departureTime = "10:00",
                arrivalTime = "22:00",
                coachNumber = "B1",
                seatNumber = "25",
                seatType = "LB"
            )

            // Fill in missing live data with mock values
            val mockTicket = current.copy(
                delay = if (current.delay == 0) (5..60).random() else current.delay,
                fromPlatform = "1",
                toPlatform = "10",
                nextStation = if (current.nextStation.isEmpty()) "STATION ${(1..50).random()}" else current.nextStation,
                journeyStarted = true
            )

            TicketRepository.currentTicket = mockTicket
            TicketRepository.target_visibility_flag = true
            preferenceManager.saveTicketInfo(mockTicket)
            preferenceManager.saveVisibility(true)

            com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
            displayTicketInfo(resultText, mockTicket)
            Toast.makeText(this, "Mock data applied. Widget visible for 2 mins.", Toast.LENGTH_SHORT).show()

            // Reset after 2 minutes using WorkManager for reliability
            val resetRequest = androidx.work.OneTimeWorkRequestBuilder<com.meilluer.smartspacer_irctc.service.VisibilityResetWorker>()
                .setInitialDelay(2, TimeUnit.MINUTES)
                .build()
            
            WorkManager.getInstance(this).enqueueUniqueWork(
                "DebugVisibilityReset",
                androidx.work.ExistingWorkPolicy.REPLACE,
                resetRequest
            )
        }

        showNowButton.setOnClickListener {
            val ticket = TicketRepository.currentTicket
            if (ticket != null) {
                TicketRepository.target_visibility_flag = true
                preferenceManager.saveVisibility(true)
                com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
                Toast.makeText(this, "Visibility flag set to true.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No ticket info available to show.", Toast.LENGTH_SHORT).show()
            }
        }

        debugToggle.setOnClickListener {
            debugLayout.visibility = if (debugLayout.visibility == android.view.View.VISIBLE) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
        }

        scanButton.setOnClickListener {
            val email = emailEdit.text.toString()
            val password = passwordEdit.text.toString()
            val geminiApiKey = geminiApiKeyEdit.text.toString()
            val customSender = customSenderEdit.text.toString().ifEmpty { null }
            val customSubject = customSubjectEdit.text.toString().ifEmpty { null }

            if (email.isEmpty() || password.isEmpty() || geminiApiKey.isEmpty()) {
                Toast.makeText(this, "Please enter email, app password and Gemini API Key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            preferenceManager.saveCredentials(email, password)
            preferenceManager.saveGeminiApiKey(geminiApiKey)
            preferenceManager.saveCustomSender(customSender)
            preferenceManager.saveCustomSubject(customSubject)
            scheduleDailyScan()

            scanButton.isEnabled = false
            resultText.text = "Scanning..."

            lifecycleScope.launch {
                val scanner = EmailScanner()
                try {
                    val success = withContext(Dispatchers.IO) {
                        scanner.scanEmails(email, password, geminiApiKey, onlyUnread = true, customSender = customSender, customSubject = customSubject)
                    }

                    scanButton.isEnabled = true
                    if (success) {
                        val ticket = TicketRepository.currentTicket
                        if (ticket != null) {
                            preferenceManager.saveTicketInfo(ticket)
                            com.meilluer.smartspacer_irctc.util.TicketScheduler.scheduleVisibilityUpdate(this@MainActivity, ticket)
                            com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
                            displayTicketInfo(resultText, ticket)
                        }
                    } else {
                        resultText.text = "No unread IRCTC booking confirmation emails found."
                    }
                } catch (e: Exception) {
                    scanButton.isEnabled = true
                    resultText.text = "Error: ${e.message}"
                    e.printStackTrace()
                }
            }
        }
    }

    private fun displayTicketInfo(resultText: TextView, ticket: com.meilluer.smartspacer_irctc.data.TicketInfo) {
        val delayText = if (ticket.delay > 0) " (${ticket.delay}m delay)" else ""
        val platformText = if (ticket.fromPlatform.isNotEmpty()) "\nPlatform: ${ticket.fromPlatform}" else ""
        val nextStationText = if (ticket.nextStation.isNotEmpty()) "\nNext: ${ticket.nextStation}" else ""
        
        resultText.text = """
            Success!
            Train: ${ticket.trainNumber} - ${ticket.trainName}$delayText
            Coach: ${ticket.coachNumber}, Seat: ${ticket.seatNumber} (${ticket.seatType})
            From: ${ticket.fromStation}$platformText
            To: ${ticket.toStation}
            Date: ${ticket.boardingDate}
            Departure: ${ticket.departureTime}
            Arrival: ${ticket.arrivalTime}$nextStationText
        """.trimIndent()
    }

    private fun scheduleDailyScan() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Calculate initial delay to start at midnight
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        dueDate.set(Calendar.HOUR_OF_DAY, 0)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }
        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<EmailScanWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyEmailScan",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}
