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
import com.meilluer.smartspacer_irctc.data.PreferenceManager
import com.meilluer.smartspacer_irctc.data.TicketRepository
import com.meilluer.smartspacer_irctc.service.EmailScanWorker
import com.meilluer.smartspacer_irctc.service.EmailScanner
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val emailEdit = findViewById<EditText>(R.id.emailEdit)
        val passwordEdit = findViewById<EditText>(R.id.passwordEdit)
        val scanButton = findViewById<Button>(R.id.scanButton)
        val resultText = findViewById<TextView>(R.id.resultText)
        val debugToggle = findViewById<TextView>(R.id.debugToggle)
        val debugLayout = findViewById<android.view.View>(R.id.debugLayout)
        val customSenderEdit = findViewById<EditText>(R.id.customSenderEdit)
        val mockButton = findViewById<Button>(R.id.mockButton)

        val preferenceManager = PreferenceManager(this)
        emailEdit.setText(preferenceManager.getEmail())
        passwordEdit.setText(preferenceManager.getPassword())
        customSenderEdit.setText(preferenceManager.getCustomSender())

        // Load existing ticket info
        val existingTicket = preferenceManager.getTicketInfo()
        if (existingTicket != null) {
            TicketRepository.currentTicket = existingTicket
            displayTicketInfo(resultText, existingTicket)
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
                fromPlatform = if (current.fromPlatform.isEmpty()) (1..12).random().toString() else current.fromPlatform,
                toPlatform = if (current.toPlatform.isEmpty()) (1..12).random().toString() else current.toPlatform,
                nextStation = if (current.nextStation.isEmpty()) "STATION ${(1..50).random()}" else current.nextStation,
                journeyStarted = true
            )

            TicketRepository.currentTicket = mockTicket
            TicketRepository.target_visibility_flag = true
            preferenceManager.saveTicketInfo(mockTicket)

            com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
            displayTicketInfo(resultText, mockTicket)
            Toast.makeText(this, "Mock data applied. Widget visible for 2 mins.", Toast.LENGTH_SHORT).show()

            // Reset after 2 minutes
            mockButton.postDelayed({
                TicketRepository.target_visibility_flag = false
                com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
                Toast.makeText(applicationContext, "Debug visibility reset.", Toast.LENGTH_SHORT).show()
            }, 120000)
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
            val customSender = customSenderEdit.text.toString().ifEmpty { null }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and app password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            preferenceManager.saveCredentials(email, password)
            preferenceManager.saveCustomSender(customSender)
            scheduleDailyScan()

            scanButton.isEnabled = false
            resultText.text = "Scanning..."

            thread {
                val scanner = EmailScanner()
                val success = scanner.scanEmails(email, password, onlyUnread = false, customSender = customSender)

                runOnUiThread {
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
                        resultText.text = "Failed to find or parse ticket email."
                    }
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
