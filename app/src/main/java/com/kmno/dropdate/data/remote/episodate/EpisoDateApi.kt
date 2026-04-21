package com.kmno.dropdate.data.remote.episodate

import retrofit2.http.GET
import retrofit2.http.Query

interface EpisoDateApi {
    @GET("most-popular")
    suspend fun getMostPopular(
        @Query("page") page: Int = 1,
    ): EpisoDateResponse
}
