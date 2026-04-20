package com.kmno.dropdate.data.remote.tvmaze

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvMazeScheduleEntryDto(
    val id: Int,
    val name: String? = null,
    val airdate: String? = null,
    val airtime: String? = null,
    val runtime: Int? = null,
    @SerialName("_embedded") val embedded: TvMazeEmbeddedDto? = null,
    val season: Int? = null,
    val number: Int? = null,
) {
    val show: TvMazeShowDto? get() = embedded?.show
}

@Serializable
data class TvMazeEmbeddedDto(
    val show: TvMazeShowDto? = null
)

@Serializable
data class TvMazeShowDto(
    val id: Int,
    val name: String,
    val type: String,
    val language: String? = null,
    val image: TvMazeImageDto? = null,
    val rating: TvMazeRatingDto? = null,
    val summary: String? = null,
    val genres: List<String> = emptyList(),
    @SerialName("webChannel") val webChannel: TvMazeNetworkDto? = null,
    val network: TvMazeNetworkDto? = null,
)

@Serializable
data class TvMazeImageDto(
    val medium: String? = null,
    val original: String? = null,
)

@Serializable
data class TvMazeRatingDto(
    val average: Float? = null,
)

@Serializable
data class TvMazeNetworkDto(
    val name: String? = null,
)
