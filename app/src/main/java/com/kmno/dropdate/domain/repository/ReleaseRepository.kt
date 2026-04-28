package com.kmno.dropdate.domain.repository

import com.kmno.dropdate.domain.model.Release
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ReleaseRepository {
    fun getReleasesForWeek(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Flow<List<Release>>

    suspend fun syncReleases(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Result<Unit>

    suspend fun deleteOldReleases(before: LocalDate)

    fun searchReleasesTitle(query: String): Flow<List<Release>>
}
