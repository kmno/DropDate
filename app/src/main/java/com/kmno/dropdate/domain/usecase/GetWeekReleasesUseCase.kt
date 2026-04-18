package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetWeekReleasesUseCase @Inject constructor(
    private val repository: ReleaseRepository
) {
    operator fun invoke(weekStart: LocalDate, weekEnd: LocalDate): Flow<List<Release>> =
        repository.getReleasesForWeek(weekStart, weekEnd)
}
