package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.repository.ReleaseRepository
import java.time.LocalDate
import javax.inject.Inject

class SyncReleasesUseCase
@Inject
constructor(
    private val repository: ReleaseRepository,
) {
    suspend operator fun invoke(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Result<Unit> = repository.syncReleases(weekStart, weekEnd)
}
