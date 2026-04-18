package com.kmno.dropdate.data.remote.jikan

import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApi {
    @GET("seasons/now")
    suspend fun getCurrentlyAiringAnime(
        @Query("page") page: Int = 1,
    ): JikanSeasonDto

    @GET("seasons/upcoming")
    suspend fun getUpcomingAnime(): JikanSeasonDto
}
