package com.kmno.dropdate.data.remote.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanSeasonDto(
    val data: List<JikanAnimeDto> = emptyList(),
)

@Serializable
data class JikanAnimeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String,
    val images: JikanImagesDto?,
    val score: Float?,
    val synopsis: String?,
    val status: String?,
    @SerialName("aired") val aired: JikanAiredDto?,
    @SerialName("broadcast") val broadcast: JikanBroadcastDto?,
    val episodes: Int?,
)

@Serializable
data class JikanImagesDto(
    val jpg: JikanImageDto?,
)

@Serializable
data class JikanImageDto(
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("large_image_url") val largeImageUrl: String?,
)

@Serializable
data class JikanAiredDto(
    val from: String?,
)

@Serializable
data class JikanBroadcastDto(
    val day: String?,
    val time: String?,
    val timezone: String?,
)
