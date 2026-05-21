package com.meilluer.smartspacer_irctc.service

import com.meilluer.smartspacer_irctc.data.TicketInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class IrctcParser {

    fun parseEmail(htmlContent: String): TicketInfo? {
        val doc: Document = Jsoup.parse(htmlContent)
        val text = doc.text()
        
        var trainNumber = ""
        var trainName = ""
        var seatNumber = ""
        var coachNumber = ""
        var boardingDate = ""
        var departureTime = ""
        var fromStation = ""
        var toStation = ""
        var arrivalTime = ""

        // Strategy 1: Table-row based extraction (more precise if layout matches)
        val allRows = doc.select("tr")
        for (row in allRows) {
            val cells = row.select("td")
            if (cells.isEmpty()) continue
            
            val rowText = row.text()
            
            // Flexible matching for Train No. / Name
            if (rowText.contains("Train No", ignoreCase = true) && rowText.contains("Name", ignoreCase = true)) {
                val value = cells.last()?.text() ?: ""
                if (value.contains("/")) {
                    val parts = value.split("/")
                    trainNumber = parts[0].trim().filter { it.isDigit() }
                    trainName = parts[1].trim()
                } else if (trainNumber.isEmpty()) {
                    // Try regex if simple split fails
                    val regex = Regex("(\\d{5})\\s*/?\\s*(.*)")
                    regex.find(value)?.let {
                        trainNumber = it.groupValues[1]
                        trainName = it.groupValues[2]
                    }
                }
            }
            
            if (rowText.contains("From", ignoreCase = true) && rowText.contains("To", ignoreCase = true) && !rowText.contains("Address", ignoreCase = true)) {
                val value = cells.last()?.text() ?: ""
                if (value.contains("/")) {
                    val parts = value.split("/")
                    fromStation = parts[0].trim()
                    toStation = parts[1].trim()
                }
            }
            
            if (rowText.contains("Date of Journey", ignoreCase = true) || rowText.contains("Boarding Date", ignoreCase = true)) {
                boardingDate = cells.last()?.text() ?: ""
            }
            
            if (rowText.contains("Scheduled Departure", ignoreCase = true)) {
                departureTime = cells.last()?.text() ?: ""
                if (departureTime.contains(" ")) {
                    departureTime = departureTime.substringAfterLast(" ").trim()
                }
            }
            
            if (rowText.contains("Scheduled Arrival", ignoreCase = true)) {
                arrivalTime = cells.last()?.text() ?: ""
                if (arrivalTime.contains(" ")) {
                    arrivalTime = arrivalTime.substringAfterLast(" ").trim()
                }
            }
        }

        // Strategy 2: Regex fallback for critical missing info
        if (trainNumber.isEmpty()) {
            val trainRegex = Regex("Train No\\.?\\s*/?\\s*Name\\s*[:\\-]?\\s*(\\d{5})\\s*/?\\s*([^\\n\\r]*)", RegexOption.IGNORE_CASE)
            trainRegex.find(text)?.let {
                trainNumber = it.groupValues[1]
                trainName = it.groupValues[2].trim()
            }
        }

        if (fromStation.isEmpty()) {
            val stationRegex = Regex("From\\s*/\\s*To\\s*[:\\-]?\\s*([^/\\n\\r]*)\\s*/\\s*([^\\n\\r]*)", RegexOption.IGNORE_CASE)
            stationRegex.find(text)?.let {
                fromStation = it.groupValues[1].trim()
                toStation = it.groupValues[2].trim()
            }
        }

        // Passenger details table
        val tables = doc.select("table")
        val passengerTable = tables.find { 
            val t = it.text().lowercase()
            t.contains("coach") && (t.contains("seat") || t.contains("berth")) 
        }
        
        passengerTable?.let { table ->
            val rows = table.select("tr")
            val headerRow = rows.find { it.text().lowercase().contains("coach") }
            if (headerRow != null) {
                val headers = headerRow.select("td").map { it.text().lowercase() }
                val coachIdx = headers.indexOfFirst { it.contains("coach") }
                val seatIdx = headers.indexOfFirst { it.contains("seat") || it.contains("berth") }
                
                val headerIdx = rows.indexOf(headerRow)
                if (rows.size > headerIdx + 1) {
                    val dataRow = rows[headerIdx + 1]
                    val cells = dataRow.select("td")
                    if (coachIdx != -1 && cells.size > coachIdx) {
                        coachNumber = cells[coachIdx].text().trim()
                    }
                    if (seatIdx != -1 && cells.size > seatIdx) {
                        seatNumber = cells[seatIdx].text().trim()
                    }
                }
            }
        }
        
        // Final fallback for seat/coach if table parsing failed
        if (coachNumber.isEmpty()) {
            Regex("Coach\\s*[:\\-]?\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE).find(text)?.let {
                coachNumber = it.groupValues[1]
            }
        }
        if (seatNumber.isEmpty()) {
            Regex("(?:Seat|Berth)\\s*No\\.?\\s*[:\\-]?\\s*(\\d+)", RegexOption.IGNORE_CASE).find(text)?.let {
                seatNumber = it.groupValues[1]
            }
        }

        if (trainNumber.isEmpty() || fromStation.isEmpty()) return null

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
