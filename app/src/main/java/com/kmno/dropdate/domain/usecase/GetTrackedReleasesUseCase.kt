package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrackedReleasesUseCase
    @Inject
    constructor(
        private val repository: ReleaseRepository,
    ) {
        operator fun invoke(): Flow<List<Release>> = repository.getTrackedReleases()
    }
