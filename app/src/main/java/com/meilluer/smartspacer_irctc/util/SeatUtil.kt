package com.meilluer.smartspacer_irctc.util

object SeatUtil {
    fun getSeatType(coach: String, seat: String): String {
        if (seat.contains("LB", true)) return "LB"
        if (seat.contains("MB", true)) return "MB"
        if (seat.contains("UB", true)) return "UB"
        if (seat.contains("SL", true)) return "SL"
        if (seat.contains("SU", true)) return "SU"

        val seatNum = seat.filter { it.isDigit() }.toIntOrNull() ?: return ""
        val coachPrefix = coach.uppercase().filter { it.isLetter() }

        return when {
            // Sleeper (S) or 3rd AC (B, M, G, BE) - 8 berth bay
            coachPrefix.startsWith("S") || coachPrefix.startsWith("B") || 
            coachPrefix.startsWith("M") || coachPrefix.startsWith("G") || 
            coachPrefix.startsWith("BE") -> {
                when (seatNum % 8) {
                    1, 4 -> "LB"
                    2, 5 -> "MB"
                    3, 6 -> "UB"
                    7 -> "SL"
                    0 -> "SU"
                    else -> ""
                }
            }
            // 2nd AC (A) - 6 berth bay
            coachPrefix.startsWith("A") -> {
                when (seatNum % 6) {
                    1, 3 -> "LB"
                    2, 4 -> "UB"
                    5 -> "SL"
                    0 -> "SU"
                    else -> ""
                }
            }
            // 1st AC (H) - Usually 4 berth cabin or 2 berth coupe, harder to predict but often LB/UB
            coachPrefix.startsWith("H") -> {
                if (seatNum % 2 == 1) "LB" else "UB"
            }
            else -> ""
        }
    }
}
