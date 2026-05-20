package com.meilluer.smartspacer_irctc.service

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RailRadarApi {

    @GET("/api/v1/trains/{trainNumber}/instances")
    fun getTrainInstances(@Path("trainNumber") trainNumber: String): Call<List<TrainInstance>>

    @GET("/api/v1/trains/{trainNumber}/instances/{instanceId}")
    fun getTrainStatus(
        @Path("trainNumber") trainNumber: String,
        @Path("instanceId") instanceId: String
    ): Call<TrainStatusResponse>
}

data class TrainInstance(
    val instanceId: String, // e.g. "2024-12-13"
    val date: String
)

data class TrainStatusResponse(
    val delay: Int,
    val stations: List<StationStatus>
)

data class StationStatus(
    val stationCode: String,
    val platform: String?,
    val scheduledArrival: String?,
    val actualArrival: String?,
    val estimatedArrival: String?,
    val scheduledDeparture: String?,
    val actualDeparture: String?,
    val estimatedDeparture: String?
)
