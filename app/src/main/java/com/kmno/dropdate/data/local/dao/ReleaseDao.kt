package com.kmno.dropdate.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kmno.dropdate.data.local.entity.ReleaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDao {

    @Query("""
        SELECT * FROM releases
        WHERE airDate BETWEEN :from AND :to
        ORDER BY airDate ASC, rating DESC
    """)
    fun observeByWeek(from: String, to: String): Flow<List<ReleaseEntity>>

    @Upsert
    suspend fun upsertAll(releases: List<ReleaseEntity>)

    @Query("DELETE FROM releases WHERE syncedAt < :threshold")
    suspend fun deleteStale(threshold: Long)

    @Query("DELETE FROM releases WHERE airDate < :before")
    suspend fun deleteOldReleases(before: String)
}
