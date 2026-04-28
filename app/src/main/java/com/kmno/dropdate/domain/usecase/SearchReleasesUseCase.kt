package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchReleasesUseCase
    @Inject
    constructor(
        private val repository: ReleaseRepository,
    ) {
        operator fun invoke(query: String): Flow<List<Release>> = repository.searchReleasesTitle(query)
    }
