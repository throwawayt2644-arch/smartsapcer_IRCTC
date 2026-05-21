package com.meilluer.smartspacer_irctc.service

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.meilluer.smartspacer_irctc.data.TicketInfo
import org.jsoup.Jsoup

class IrctcParser(private val apiKey: String) {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    suspend fun parseEmail(htmlContent: String): TicketInfo? {
        if (apiKey.isEmpty()) throw Exception("Gemini API Key is empty")
        
        val doc = Jsoup.parse(htmlContent)
        val text = doc.text()
        
        val prompt = """
            Extract the following IRCTC ticket information from the provided email content and return it strictly in JSON format.
            If a value is not found, use an empty string.
            
            Fields:
            - trainNumber: 5-digit number
            - trainName: Name of the train
            - seatNumber: Seat or berth number
            - coachNumber: Coach identifier (e.g., S1, B2, A1, M3, etc.)
            - boardingDate: Date of journey (format: dd-MMM-yyyy, e.g., 25-May-2026)
            - departureTime: Scheduled departure time (24h format: HH:mm)
            - fromStation: Source station name with code (e.g., JAIPUR (JP))
            - toStation: Destination station name with code (e.g., NEW DELHI (NDLS))
            - arrivalTime: Scheduled arrival time (24h format: HH:mm)
            - seatType: Type of seat (LB for Lower Berth, MB for Middle Berth, UB for Upper Berth, SL for Side Lower, SU for Side Upper, etc.)

            Email Content:
            $text
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            val json = response.text ?: throw Exception("Gemini API returned an empty response")
            Gson().fromJson(json, TicketInfo::class.java)
        } catch (e: Exception) {
            throw Exception("Gemini API Error: ${e.message}", e)
        }
    }
}
