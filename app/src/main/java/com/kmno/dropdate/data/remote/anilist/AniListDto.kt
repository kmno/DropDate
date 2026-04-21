package com.kmno.dropdate.data.remote.anilist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AniListRequest(
    val query: String,
    val variables: Map<String, Int>? = null,
)

@Serializable
data class AniListResponse(
    val data: AniListData?,
)

@Serializable
data class AniListData(
    @SerialName("Page") val page: AniListSchedulePage? = null,
)

@Serializable
data class AniListSchedulePage(
    val airingSchedules: List<AniListScheduleEntry> = emptyList(),
)

@Serializable
data class AniListScheduleEntry(
    val id: Int,
    val airingAt: Long,
    val episode: Int,
    val media: AniListMedia? = null,
)

@Serializable
data class AniListMedia(
    val id: Int,
    val title: AniListTitle? = null,
    val coverImage: AniListCoverImage? = null,
    val averageScore: Int? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val externalLinks: List<AniListExternalLink> = emptyList(),
)

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
)

@Serializable
data class AniListCoverImage(
    val large: String? = null,
)

@Serializable
data class AniListExternalLink(
    val site: String? = null,
)
