package com.meilluer.smartspacer_irctc.data

data class TicketInfo(
    val trainNumber: String = "",
    val trainName: String = "",
    val seatNumber: String = "",
    val coachNumber: String = "",
    val boardingDate: String = "",
    var departureTime: String = "", // Changed to var for updates
    val fromStation: String = "", // e.g. "JAIPUR (JP)"
    val toStation: String = "",   // e.g. "MGR CHENNAI CTL (MAS)"
    val arrivalDate: String = "", // Added arrival date
    var arrivalTime: String = "",   // Changed to var for updates
    var delay: Int = 0,            // New delay variable
    var fromPlatform: String = "", // New platform info
    var toPlatform: String = "",    // New platform info
    var nextStation: String = "",
    var journeyStarted: Boolean = false,
    var seatType: String = "",
    var allStations: List<String> = emptyList() // Added station list
)

object TicketRepository {
    var currentTicket: TicketInfo? = null
    var target_visibility_flag: Boolean = false

    // Helper for global variables if needed as individual properties
    val trainNumber: String
        get() = currentTicket?.trainNumber ?: ""
    val trainName: String
        get() = currentTicket?.trainName ?: ""
    val seatNumber: String
        get() = currentTicket?.seatNumber ?: ""
    val coachNumber: String
        get() = currentTicket?.coachNumber ?: ""
    var seatType: String
        get() {
            val ticket = currentTicket ?: return ""
            if (ticket.seatType.isNotEmpty()) return ticket.seatType
            return com.meilluer.smartspacer_irctc.util.SeatUtil.getSeatType(ticket.coachNumber, ticket.seatNumber)
        }
        set(value) { currentTicket?.seatType = value }
    val boardingDate: String
        get() = currentTicket?.boardingDate ?: ""
    var departureTime: String
        get() = currentTicket?.departureTime ?: ""
        set(value) { currentTicket?.departureTime = value }
    val fromStation: String
        get() = if (com.meilluer.smartspacer_irctc.BuildConfig.DEBUG) "sta 1" else (currentTicket?.fromStation ?: "")
    val toStation: String
        get() = if (com.meilluer.smartspacer_irctc.BuildConfig.DEBUG) "too 10" else (currentTicket?.toStation ?: "")
    val arrivalDate: String
        get() = currentTicket?.arrivalDate ?: ""
    var arrivalTime: String
        get() = currentTicket?.arrivalTime ?: ""
        set(value) { currentTicket?.arrivalTime = value }
    var delay: Int
        get() = currentTicket?.delay ?: 0
        set(value) { currentTicket?.delay = value }
    var fromPlatform: String
        get() = currentTicket?.fromPlatform ?: ""
        set(value) { currentTicket?.fromPlatform = value }
    var toPlatform: String
        get() = currentTicket?.toPlatform ?: ""
        set(value) { currentTicket?.toPlatform = value }
    var nextStation: String
        get() = currentTicket?.nextStation ?: ""
        set(value) { currentTicket?.nextStation = value }
    var journeyStarted: Boolean
        get() = currentTicket?.journeyStarted ?: false
        set(value) { currentTicket?.journeyStarted = value }
    var allStations: List<String>
        get() = currentTicket?.allStations ?: emptyList()
        set(value) { currentTicket?.allStations = value }

    fun reset() {
        currentTicket = null
        target_visibility_flag = false
    }
}
