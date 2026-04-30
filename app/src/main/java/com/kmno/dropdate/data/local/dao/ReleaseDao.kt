package com.kmno.dropdate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kmno.dropdate.data.local.entity.ReleaseEntity
import com.kmno.dropdate.data.local.entity.TrackedSeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDao {
    @Query(
        """
        SELECT * FROM releases
        WHERE airDate BETWEEN :from AND :to
        ORDER BY airDate ASC, rating DESC
    """,
    )
    fun observeByWeek(
        from: String,
        to: String,
    ): Flow<List<ReleaseEntity>>

    @Upsert
    suspend fun upsertAll(releases: List<ReleaseEntity>)

    @Query("DELETE FROM releases WHERE syncedAt < :threshold")
    suspend fun deleteStale(threshold: Long)

    @Query("DELETE FROM releases WHERE airDate < :before")
    suspend fun deleteOldReleases(before: String)

    @Query("SELECT * FROM releases WHERE title LIKE '%' || :query || '%'")
    fun searchReleasesTitle(query: String): Flow<List<ReleaseEntity>>

    // ── Tracking ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun trackSeries(entity: TrackedSeriesEntity)

    @Query("DELETE FROM tracked_series WHERE seriesId = :seriesId")
    suspend fun untrackSeries(seriesId: String)

    @Query("SELECT seriesId FROM tracked_series")
    fun observeTrackedSeriesIds(): Flow<List<String>>

    @Query(
        """
        SELECT * FROM releases
        WHERE seriesId IN (SELECT seriesId FROM tracked_series)
        ORDER BY airDate ASC
    """,
    )
    fun observeTracked(): Flow<List<ReleaseEntity>>
}
