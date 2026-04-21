package com.kmno.dropdate.presentation.schedule

import com.kmno.dropdate.domain.model.Release
import java.time.DayOfWeek
import java.time.LocalDate

enum class ContentFilter { ALL, MOVIES, SERIES, ANIME }

data class ScheduleUiState(
    val selectedWeekStart: LocalDate  = LocalDate.now().with(DayOfWeek.MONDAY),
    val selectedDay: LocalDate        = LocalDate.now(),
    val activeFilter: ContentFilter   = ContentFilter.ALL,
    val releases: Map<LocalDate, List<Release>> = emptyMap(),
    val selectedRelease: Release?     = null,
    val isLoading: Boolean            = false,
    val isSyncing: Boolean            = false,
    val error: String?                = null,
    val canGoBack: Boolean = true,
    val canGoForward: Boolean = true,
)
