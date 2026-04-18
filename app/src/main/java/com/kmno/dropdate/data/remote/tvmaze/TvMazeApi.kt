package com.kmno.dropdate.data.remote.tvmaze

import retrofit2.http.GET
import retrofit2.http.Query

interface TvMazeApi {
    @GET("schedule/web")
    suspend fun getStreamingSchedule(
        @Query("date") date: String,
    ): List<TvMazeScheduleEntryDto>

    @GET("schedule")
    suspend fun getBroadcastSchedule(
        @Query("date") date: String,
    ): List<TvMazeScheduleEntryDto>
}
