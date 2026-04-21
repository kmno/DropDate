package com.kmno.dropdate.data.remote.trakt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TraktAnticipatedMovieDto(
    val score: Int?,
    val movie: TraktMovieDto?,
)

@Serializable
data class TraktMovieDto(
    val title: String,
    val year: Int?,
    val ids: TraktIdsDto?,
    val overview: String?,
    val released: String?, // "YYYY-MM-DD"
    val rating: Float?,
    val votes: Int?,
)

@Serializable
data class TraktAnticipatedShowDto(
    val score: Int?,
    val show: TraktShowDto?,
)

@Serializable
data class TraktShowDto(
    val title: String,
    val year: Int?,
    val ids: TraktIdsDto?,
    val overview: String?,
    @SerialName("first_aired") val firstAired: String?, // ISO-8601 datetime
    val rating: Float?,
    val votes: Int?,
)

@Serializable
data class TraktIdsDto(
    val trakt: Int?,
    val slug: String?,
    val imdb: String?,
    val tmdb: Int?,
)
