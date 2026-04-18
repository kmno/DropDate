package com.kmno.dropdate.data.remote.animeschedule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeScheduleEntryDto(
    val route: String,
    val title: String,
    @SerialName("imageVersionRoute") val imageRoute: String?,
    @SerialName("episodeDate") val episodeDate: String?,
    @SerialName("episodeTime") val episodeTime: String?,
    val streams: List<AnimeScheduleStreamDto> = emptyList(),
    val score: Float?,
    val description: String?,
)

@Serializable
data class AnimeScheduleStreamDto(
    val name: String?,
)
