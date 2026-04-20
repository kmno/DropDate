package com.kmno.dropdate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "releases")
data class ReleaseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: String,
    val status: String,
    val airDate: String,
    val airTime: String?,
    val platform: String?,
    val episodeLabel: String?,
    val rating: Float?,
    val synopsis: String?,
    val genres: String? = null,
    val syncedAt: Long,
)
