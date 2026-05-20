package com.meilluer.smartspacer_irctc.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("irctc_prefs", Context.MODE_PRIVATE)

    fun saveCredentials(email: String, password: String) {
        prefs.edit().apply {
            putString("email", email)
            putString("password", password)
            apply()
        }
    }

    fun saveCustomSender(sender: String?) {
        prefs.edit().putString("custom_sender", sender).apply()
    }

    fun saveTicketInfo(ticket: TicketInfo?) {
        prefs.edit().apply {
            if (ticket == null) {
                remove("trainNumber")
                remove("trainName")
                remove("seatNumber")
                remove("coachNumber")
                remove("boardingDate")
                remove("departureTime")
                remove("fromStation")
                remove("toStation")
                remove("arrivalTime")
                remove("delay")
                remove("fromPlatform")
                remove("toPlatform")
                remove("nextStation")
                remove("journeyStarted")
                remove("seatType")
            } else {
                putString("trainNumber", ticket.trainNumber)
                putString("trainName", ticket.trainName)
                putString("seatNumber", ticket.seatNumber)
                putString("coachNumber", ticket.coachNumber)
                putString("boardingDate", ticket.boardingDate)
                putString("departureTime", ticket.departureTime)
                putString("fromStation", ticket.fromStation)
                putString("toStation", ticket.toStation)
                putString("arrivalTime", ticket.arrivalTime)
                putInt("delay", ticket.delay)
                putString("fromPlatform", ticket.fromPlatform)
                putString("toPlatform", ticket.toPlatform)
                putString("nextStation", ticket.nextStation)
                putBoolean("journeyStarted", ticket.journeyStarted)
                putString("seatType", ticket.seatType)
            }
            apply()
        }
    }

    fun getTicketInfo(): TicketInfo? {
        val trainNumber = prefs.getString("trainNumber", null) ?: return null
        return TicketInfo(
            trainNumber = trainNumber,
            trainName = prefs.getString("trainName", "") ?: "",
            seatNumber = prefs.getString("seatNumber", "") ?: "",
            coachNumber = prefs.getString("coachNumber", "") ?: "",
            boardingDate = prefs.getString("boardingDate", "") ?: "",
            departureTime = prefs.getString("departureTime", "") ?: "",
            fromStation = prefs.getString("fromStation", "") ?: "",
            toStation = prefs.getString("toStation", "") ?: "",
            arrivalTime = prefs.getString("arrivalTime", "") ?: "",
            delay = prefs.getInt("delay", 0),
            fromPlatform = prefs.getString("fromPlatform", "") ?: "",
            toPlatform = prefs.getString("toPlatform", "") ?: "",
            nextStation = prefs.getString("nextStation", "") ?: "",
            journeyStarted = prefs.getBoolean("journeyStarted", false),
            seatType = prefs.getString("seatType", "") ?: ""
        )
    }

    fun getEmail(): String? = prefs.getString("email", null)
    fun getPassword(): String? = prefs.getString("password", null)
    fun getCustomSender(): String? = prefs.getString("custom_sender", null)
}
