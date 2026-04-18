package com.kmno.dropdate.data.remote.animeschedule

import retrofit2.http.GET
import retrofit2.http.Query

interface AnimeScheduleApi {
    @GET("timetables")
    suspend fun getTimetable(
        @Query("tz") timezone: String = "UTC",
    ): List<AnimeScheduleEntryDto>
}
