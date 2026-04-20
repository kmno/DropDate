package com.kmno.dropdate.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

private const val API_KEY = "8d1d75d45d64e354b12b88ad332d01fe"

/**
 * Common Provider IDs for US:
 * Netflix (8), Hulu (15), Disney+ (337), Amazon Prime (119), Apple TV+ (350), 
 * Max (1899), Peacock (386), Paramount+ (531), Crunchyroll (283)
 */
const val TMDB_POPULAR_PROVIDERS = "8|15|337|119|350|1899|386|531|283"

interface TmdbApi {
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String = API_KEY,
        @Query("primary_release_date.gte") startDate: String,
        @Query("primary_release_date.lte") endDate: String,
        @Query("watch_region") watchRegion: String = "US",
        @Query("with_original_language") language: String = "en",
        @Query("sort_by") sortBy: String = "primary_release_date.asc",
    ): TmdbMovieListDto

    // @GET("discover/tv")
    @GET("tv/on_the_air")
    suspend fun discoverTv(
        @Query("api_key") apiKey: String = API_KEY,
        @Query("first_air_date.gte") startDate: String,
        @Query("first_air_date.lte") endDate: String,
        @Query("with_watch_providers") watchProviders: String? = null,
        @Query("watch_region") watchRegion: String = "US",
        @Query("with_original_language") language: String = "en",
        @Query("sort_by") sortBy: String = "first_air_date.asc",
    ): TmdbTvListDto

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String = API_KEY,
        @Query("page") page: Int = 1,
    ): TmdbMovieListDto

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String = API_KEY,
    ): TmdbMovieListDto

    @GET("tv/on_the_air")
    suspend fun getUpcomingTv(
        @Query("api_key") apiKey: String = API_KEY,
        @Query("page") page: Int = 1,
    ): TmdbTvListDto

    @GET("tv/popular")
    suspend fun getPopularTv(
        @Query("api_key") apiKey: String = API_KEY,
    ): TmdbTvListDto

    @GET("movie/{id}")
    suspend fun getMovieImages(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String = API_KEY,
    ): TmdbImagesDto

    @GET("tv/{id}")
    suspend fun getTvImages(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String = API_KEY,
    ): TmdbImagesDto
}
