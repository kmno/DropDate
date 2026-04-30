package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.repository.ReleaseRepository
import javax.inject.Inject

class SetTrackingUseCase
    @Inject
    constructor(
        private val repository: ReleaseRepository,
    ) {
        suspend operator fun invoke(
            release: Release,
            track: Boolean,
        ): Result<Unit> =
            runCatching {
                repository.setTracking(release.seriesId, track)
            }
    }
