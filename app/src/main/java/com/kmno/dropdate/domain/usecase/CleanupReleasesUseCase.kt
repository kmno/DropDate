package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.repository.ReleaseRepository
import java.time.LocalDate
import javax.inject.Inject

class CleanupReleasesUseCase
@Inject
constructor(
    private val repository: ReleaseRepository,
) {
    suspend operator fun invoke(before: LocalDate) = repository.deleteOldReleases(before)
}
