package com.kmno.dropdate.presentation.tracked

import com.kmno.dropdate.domain.model.Release

data class TrackedUiState(
    val loading: Boolean = true,
    val releases: List<Release> = emptyList(),
    val error: String? = null,
    val selectedRelease: Release? = null,
)
