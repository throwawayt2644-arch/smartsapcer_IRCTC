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

        val preferenceManager = PreferenceManager(this)
        emailEdit.setText(preferenceManager.getEmail())
        passwordEdit.setText(preferenceManager.getPassword())
        customSenderEdit.setText(preferenceManager.getCustomSender())

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
                            com.meilluer.smartspacer_irctc.util.TicketScheduler.scheduleVisibilityUpdate(this@MainActivity, ticket)
                            com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
                        }
                        resultText.text = """
                            Success!
                            Train: ${TicketRepository.trainNumber} - ${TicketRepository.trainName}
                            Coach: ${TicketRepository.coachNumber}, Seat: ${TicketRepository.seatNumber} (${TicketRepository.seatType})
                            From: ${TicketRepository.fromStation}
                            To: ${TicketRepository.toStation}
                            Date: ${TicketRepository.boardingDate}
                            Departure: ${TicketRepository.departureTime}
                            Arrival: ${TicketRepository.arrivalTime}
                        """.trimIndent()
                    } else {
                        resultText.text = "Failed to find or parse ticket email."
                    }
                }
            }
        }
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
