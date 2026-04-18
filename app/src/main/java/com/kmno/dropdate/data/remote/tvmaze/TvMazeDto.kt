package com.kmno.dropdate.data.remote.tvmaze

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvMazeScheduleEntryDto(
    val id: Int,
    val name: String?,
    val airdate: String?,
    val airtime: String?,
    val runtime: Int?,
    val show: TvMazeShowDto?,
    val season: Int?,
    val number: Int?,
)

@Serializable
data class TvMazeShowDto(
    val id: Int,
    val name: String,
    val image: TvMazeImageDto?,
    val rating: TvMazeRatingDto?,
    val summary: String?,
    @SerialName("webChannel") val webChannel: TvMazeNetworkDto?,
    val network: TvMazeNetworkDto?,
)

@Serializable
data class TvMazeImageDto(
    val medium: String?,
    val original: String?,
)

@Serializable
data class TvMazeRatingDto(
    val average: Float?,
)

@Serializable
data class TvMazeNetworkDto(
    val name: String?,
)
