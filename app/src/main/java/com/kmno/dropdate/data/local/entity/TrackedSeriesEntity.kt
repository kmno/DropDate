package com.kmno.dropdate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_series")
data class TrackedSeriesEntity(
    @PrimaryKey val seriesId: String,
)
