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

    fun saveGeminiApiKey(apiKey: String) {
        prefs.edit().putString("gemini_api_key", apiKey).apply()
    }

    fun getGeminiApiKey(): String? = prefs.getString("gemini_api_key", null)

    fun saveCustomSender(sender: String?) {
        prefs.edit().putString("custom_sender", sender).apply()
    }

    fun saveCustomSubject(subject: String?) {
        prefs.edit().putString("custom_subject", subject).apply()
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
                remove("arrivalDate")
                remove("allStations")
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
                putString("arrivalDate", ticket.arrivalDate)
                putString("allStations", ticket.allStations.joinToString("|"))
            }
            apply()
        }
    }

    fun saveVisibility(visible: Boolean) {
        prefs.edit().putBoolean("target_visibility_flag", visible).apply()
    }

    fun getVisibility(): Boolean = prefs.getBoolean("target_visibility_flag", false)

    fun getTicketInfo(): TicketInfo? {
        val trainNumber = prefs.getString("trainNumber", null) ?: return null
        val allStationsString = prefs.getString("allStations", "") ?: ""
        val allStations = if (allStationsString.isEmpty()) emptyList() else allStationsString.split("|")

        return TicketInfo(
            trainNumber = trainNumber,
            trainName = prefs.getString("trainName", "") ?: "",
            seatNumber = prefs.getString("seatNumber", "") ?: "",
            coachNumber = prefs.getString("coachNumber", "") ?: "",
            boardingDate = prefs.getString("boardingDate", "") ?: "",
            departureTime = prefs.getString("departureTime", "") ?: "",
            fromStation = prefs.getString("fromStation", "") ?: "",
            toStation = prefs.getString("toStation", "") ?: "",
            arrivalDate = prefs.getString("arrivalDate", "") ?: "",
            arrivalTime = prefs.getString("arrivalTime", "") ?: "",
            delay = prefs.getInt("delay", 0),
            fromPlatform = prefs.getString("fromPlatform", "") ?: "",
            toPlatform = prefs.getString("toPlatform", "") ?: "",
            nextStation = prefs.getString("nextStation", "") ?: "",
            journeyStarted = prefs.getBoolean("journeyStarted", false),
            seatType = prefs.getString("seatType", "") ?: "",
            allStations = allStations
        )
    }

    fun getEmail(): String? = prefs.getString("email", null)
    fun getPassword(): String? = prefs.getString("password", null)
    fun getCustomSender(): String? = prefs.getString("custom_sender", null)
    fun getCustomSubject(): String? = prefs.getString("custom_subject", null)
}
