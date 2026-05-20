package com.meilluer.smartspacer_irctc.service

import com.meilluer.smartspacer_irctc.data.TicketInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class IrctcParser {

    fun parseEmail(htmlContent: String): TicketInfo? {
        val doc: Document = Jsoup.parse(htmlContent)
        
        // IRCTC emails are often complex tables. We can try to find text by labels.
        val text = doc.text()
        
        // Alternative: Look for tables and rows
        val tables = doc.select("table")
        
        var trainNumber = ""
        var trainName = ""
        var seatNumber = ""
        var coachNumber = ""
        var boardingDate = ""
        var departureTime = ""
        var fromStation = ""
        var toStation = ""
        var arrivalTime = ""

        // Extract using regex or searching for labels in tables
        val allRows = doc.select("tr")
        for (row in allRows) {
            val rowText = row.text()
            
            if (rowText.contains("Train No. / Name", ignoreCase = true)) {
                val value = row.select("td").last()?.text() ?: ""
                if (value.contains("/")) {
                    val parts = value.split("/")
                    trainNumber = parts[0].trim()
                    trainName = parts[1].trim()
                }
            }
            
            if (rowText.contains("From / To", ignoreCase = true)) {
                val value = row.select("td").last()?.text() ?: ""
                if (value.contains("/")) {
                    val parts = value.split("/")
                    fromStation = parts[0].trim()
                    toStation = parts[1].trim()
                }
            }
            
            if (rowText.contains("Date of Journey", ignoreCase = true)) {
                boardingDate = row.select("td").last()?.text() ?: ""
            }
            
            if (rowText.contains("Scheduled Departure", ignoreCase = true)) {
                departureTime = row.select("td").last()?.text() ?: ""
                // Sometimes it includes the date, let's keep it as is or trim if it's too long
                if (departureTime.contains(" ")) {
                    departureTime = departureTime.substringAfterLast(" ").trim()
                }
            }
            
            if (rowText.contains("Scheduled Arrival", ignoreCase = true)) {
                arrivalTime = row.select("td").last()?.text() ?: ""
                if (arrivalTime.contains(" ")) {
                    arrivalTime = arrivalTime.substringAfterLast(" ").trim()
                }
            }
        }

        // Passenger details table usually has headers: Status Coach Seat / Berth
        // We look for a row that has "Coach" and "Seat" and then take the next row.
        val passengerTable = tables.find { it.text().contains("Coach", ignoreCase = true) && it.text().contains("Seat", ignoreCase = true) }
        passengerTable?.let { table ->
            val rows = table.select("tr")
            // Skip header row
            if (rows.size > 1) {
                // Find index of Coach and Seat
                val headerRow = rows.find { it.text().contains("Coach", ignoreCase = true) }
                val headers = headerRow?.select("td")?.map { it.text().lowercase() } ?: emptyList()
                
                val coachIdx = headers.indexOfFirst { it.contains("coach") }
                val seatIdx = headers.indexOfFirst { it.contains("seat") }
                
                // Get first passenger (row after header)
                val dataRow = rows[rows.indexOf(headerRow) + 1]
                val cells = dataRow.select("td")
                if (coachIdx != -1 && cells.size > coachIdx) {
                    coachNumber = cells[coachIdx].text()
                }
                if (seatIdx != -1 && cells.size > seatIdx) {
                    seatNumber = cells[seatIdx].text()
                }
            }
        }

        if (trainNumber.isEmpty()) return null

        return TicketInfo(
            trainNumber = trainNumber,
            trainName = trainName,
            seatNumber = seatNumber,
            coachNumber = coachNumber,
            boardingDate = boardingDate,
            departureTime = departureTime,
            fromStation = fromStation,
            toStation = toStation,
            arrivalTime = arrivalTime
        )
    }
}
