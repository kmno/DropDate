package com.kmno.dropdate.data.remote.episodate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisoDateResponse(
    val total: Int = 0,
    val pages: Int = 0,
    @SerialName("tv_shows") val tvShows: List<EpisoDateShowDto> = emptyList(),
)

@Serializable
data class EpisoDateShowDto(
    val id: Int,
    val name: String,
    val permalink: String?,
    @SerialName("start_date") val startDate: String?,
    val network: String?,
    val status: String?,
    @SerialName("image_thumbnail_path") val imageThumbnailPath: String?,
    val countdown: EpisoDateCountdownDto?,
)

@Serializable
data class EpisoDateCountdownDto(
    val season: Int?,
    val episode: Int?,
    // UTC datetime: "YYYY-MM-DD HH:MM:SS" — gives hour/minute precision for countdowns
    @SerialName("air_date") val airDate: String?,
)
