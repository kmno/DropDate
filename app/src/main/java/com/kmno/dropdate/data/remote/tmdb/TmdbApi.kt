package com.kmno.dropdate.data.remote.tmdb

import com.kmno.dropdate.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("page") page: Int = 1,
    ): TmdbMovieListDto

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
    ): TmdbMovieListDto
}
