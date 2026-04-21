package com.kmno.dropdate.data.remote.trakt

import retrofit2.http.GET
import retrofit2.http.Query

interface TraktApi {
    // extended=full adds: overview, released/first_aired, rating, votes, runtime
    @GET("movies/anticipated")
    suspend fun getAnticipatedMovies(
        @Query("limit") limit: Int = 10,
        @Query("extended") extended: String = "full",
    ): List<TraktAnticipatedMovieDto>

    @GET("shows/anticipated")
    suspend fun getAnticipatedShows(
        @Query("limit") limit: Int = 10,
        @Query("extended") extended: String = "full",
    ): List<TraktAnticipatedShowDto>
}
