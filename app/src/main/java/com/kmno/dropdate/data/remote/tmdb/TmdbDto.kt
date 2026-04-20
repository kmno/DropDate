package com.kmno.dropdate.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbMovieListDto(
    val results: List<TmdbMovieDto> = emptyList(),
)

@Serializable
data class TmdbMovieDto(
    val id: Int,
    val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Float? = null,
    val overview: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

// Minimal response for image-only lookups (used for Trakt anticipated items)
// ignoreUnknownKeys = true in Json config means extra fields are safely ignored
@Serializable
data class TmdbImagesDto(
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("backdrop_path") val backdropPath: String?,
)

@Serializable
data class TmdbTvListDto(
    val results: List<TmdbTvDto>,
)

@Serializable
data class TmdbTvDto(
    val id: Int,
    val name: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Float? = null,
    val overview: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)
