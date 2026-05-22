package com.meilluer.smartspacer_irctc.service

import androidx.work.Worker
import androidx.work.WorkerParameters
import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.meilluer.smartspacer_irctc.Target
import com.meilluer.smartspacer_irctc.data.TicketRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale

class LiveUpdateWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val api: RailRadarApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.railradar.org")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RailRadarApi::class.java)
    }

    override fun doWork(): Result {
        val ticket = TicketRepository.currentTicket ?: return Result.failure()
        val trainNumber = ticket.trainNumber
        
        // 1. Get Instance ID (mapping boarding date to instance)
        // Boarding date format: "13-Dec-2024" -> Needs to match Instance ID usually "YYYY-MM-DD"
        val inputFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        
        val instanceId = try {
            val date = inputFormat.parse(ticket.boardingDate)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            trainNumber // Fallback if parsing fails, though unlikely
        }

        try {
            val response = api.getTrainStatus(trainNumber, instanceId).execute()
            if (response.isSuccessful && response.body() != null) {
                val status = response.body()!!
                
                TicketRepository.delay = status.delay
                
                // Find our stations in the list
                val fromCode = extractStationCode(ticket.fromStation)
                val toCode = extractStationCode(ticket.toStation)
                
                val fromStationStatus = status.stations.find { it.stationCode == fromCode }
                val toStationStatus = status.stations.find { it.stationCode == toCode }
                
                // Fetch and store all stations if not already present
                if (TicketRepository.allStations.isEmpty() && status.stations.isNotEmpty()) {
                    TicketRepository.allStations = status.stations.map { it.stationCode }
                }

                // Detect journey start
                if (!TicketRepository.journeyStarted) {
                    fromStationStatus?.let {
                        if (it.actualArrival != null || it.actualDeparture != null) {
                            TicketRepository.journeyStarted = true
                            rescheduleHourlyUpdates()
                        }
                    }
                }

                // Update next station and check for destination arrival
                if (TicketRepository.journeyStarted) {
                    val fromIndex = status.stations.indexOf(fromStationStatus)
                    if (fromIndex != -1) {
                        val next = status.stations.subList(fromIndex + 1, status.stations.size)
                            .find { it.actualArrival == null }
                        next?.let {
                            TicketRepository.nextStation = it.stationCode
                        }
                    }

                    // Check if destination has been reached or passed
                    val toIndex = TicketRepository.allStations.indexOf(toCode)
                    if (toIndex != -1) {
                        // Check if any station at or after destination has an actual arrival
                        val arrivedAtOrAfter = status.stations.filter { s ->
                            val idx = TicketRepository.allStations.indexOf(s.stationCode)
                            idx >= toIndex && s.actualArrival != null
                        }.isNotEmpty()

                        if (arrivedAtOrAfter) {
                            scheduleCleanup()
                        }
                    }
                }

                fromStationStatus?.let {
                    val updatedTime = it.estimatedDeparture ?: it.actualDeparture ?: it.scheduledDeparture
                    if (updatedTime != null) {
                        TicketRepository.departureTime = formatTime(updatedTime)
                    }
                    it.platform?.let { p -> TicketRepository.fromPlatform = p }
                }
                
                toStationStatus?.let {
                    val updatedTime = it.estimatedArrival ?: it.actualArrival ?: it.scheduledArrival
                    if (updatedTime != null) {
                        TicketRepository.arrivalTime = formatTime(updatedTime)
                    }
                    it.platform?.let { p -> TicketRepository.toPlatform = p } // Fixed: was fromPlatform

                    // Check if train has arrived at destination
                    if (it.actualArrival != null) {
                        scheduleCleanup()
                    }
                }

                // Persist updates
                val preferenceManager = com.meilluer.smartspacer_irctc.data.PreferenceManager(applicationContext)
                preferenceManager.saveTicketInfo(TicketRepository.currentTicket)

                // Notify Smartspacer of updates
                SmartspacerTargetProvider.notifyChange(applicationContext, Target::class.java, "IRCTC_ticket")
                
                return Result.success()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Result.retry()
    }

    private fun rescheduleHourlyUpdates() {
        val updateRequest = androidx.work.PeriodicWorkRequestBuilder<LiveUpdateWorker>(1, java.util.concurrent.TimeUnit.HOURS)
            .build()
        
        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "LiveTrainUpdate",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
    }

    private fun scheduleCleanup() {
        val cleanupRequest = androidx.work.OneTimeWorkRequestBuilder<CleanupWorker>()
            .setInitialDelay(30, java.util.concurrent.TimeUnit.MINUTES)
            .build()
        
        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "JourneyCleanup",
            androidx.work.ExistingWorkPolicy.KEEP, // Keep if already scheduled
            cleanupRequest
        )
    }

    private fun extractStationCode(stationString: String): String {
        // stationString: "JAIPUR (JP)" -> returns "JP"
        return stationString.substringAfter("(").substringBefore(")")
    }

    private fun formatTime(timeStr: String): String {
        // RailRadar might return ISO or HH:mm:ss. We want HH:mm
        return if (timeStr.contains("T")) {
            timeStr.substringAfter("T").substring(0, 5)
        } else if (timeStr.length >= 5) {
            timeStr.substring(0, 5)
        } else {
            timeStr
        }
    }
}
