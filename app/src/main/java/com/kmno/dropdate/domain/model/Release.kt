package com.kmno.dropdate.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Release(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: ReleaseType,
    val status: ReleaseStatus,
    val airDate: LocalDate,
    val airTime: LocalTime?,
    val platform: String?,
    val episodeLabel: String?,
    val rating: Float?,
    val synopsis: String?,
)
