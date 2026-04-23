package com.kmno.dropdate.presentation.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.usecase.CleanupReleasesUseCase
import com.kmno.dropdate.domain.usecase.GetWeekReleasesUseCase
import com.kmno.dropdate.domain.usecase.SyncReleasesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ScheduleViewModel
@Inject
constructor(
    private val getWeekReleases: GetWeekReleasesUseCase,
    private val syncReleases: SyncReleasesUseCase,
    private val cleanupReleases: CleanupReleasesUseCase,
) : ViewModel() {
    private val today = LocalDate.now()

    // 3-week frame: last Mon → next Sun
    private val minDay = today.with(DayOfWeek.MONDAY).minusWeeks(1)
    private val maxDay = today.with(DayOfWeek.MONDAY).plusWeeks(1).plusDays(6)

    // Tracks which week-start dates have already been fetched this session
    private val syncedWeeks = mutableSetOf<LocalDate>()

    private val _state =
        MutableStateFlow(
            ScheduleUiState(isLoading = true).let { s ->
                s.copy(canGoBack = s.selectedDay > minDay, canGoForward = s.selectedDay < maxDay)
            },
        )
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        // Observe DB — reacts to week/day/filter changes
        viewModelScope.launch {
            _state
                .map { it.selectedWeekStart to it.selectedDay }
                .distinctUntilChanged()
                .debounce(timeoutMillis = 200)
                .flatMapLatest { (weekStart, selectedDay) ->
                    _state.update { it.copy(isLoading = true) }
                    getWeekReleases(weekStart, weekStart.plusDays(6))
                        .map { releases -> releases.groupAndFilter(selectedDay) }
                }.collectLatest { grouped ->
                    _state.update { it.copy(releases = grouped, isLoading = false) }
                }
        }

        // Lazy-fetch: sync a week the first time it becomes visible
        viewModelScope.launch {
            _state
                .map { it.selectedWeekStart }
                .distinctUntilChanged()
                .collect { weekStart ->
                    if (weekStart !in syncedWeeks) fetchWeek(weekStart)
                }
        }

        // Clean up releases older than last Monday at startup
        viewModelScope.launch { cleanupReleases(minDay) }
    }

    private suspend fun fetchWeek(weekStart: LocalDate) {
        _state.update { it.copy(isSyncing = true, error = null) }
        try {
            syncReleases(weekStart, weekStart.plusDays(6))
                .onSuccess { syncedWeeks.add(weekStart) }
                .onFailure { e ->
                    Log.e("ScheduleViewModel", "Failed to sync week $weekStart: ${e.message}")
                    _state.update { it.copy(error = e.message) }
                }
        } finally {
            _state.update { it.copy(isSyncing = false) }
        }
    }

    fun onDaySelected(day: LocalDate) {
        if (day !in minDay..maxDay) return
        _state.update {
            it.copy(
                selectedDay = day,
                canGoBack = day > minDay,
                canGoForward = day < maxDay,
            )
        }
    }

    fun onDoubleTapDay(day: LocalDate) {
        if (day !in minDay..maxDay) return
        val newWeekStart = day.minusDays(3)
        _state.update {
            it.copy(
                selectedDay = day,
                selectedWeekStart = newWeekStart,
                canGoBack = day > minDay,
                canGoForward = day < maxDay,
            )
        }
    }

    fun onWeekChanged(weekStart: LocalDate) {
        _state.update { it.copy(selectedWeekStart = weekStart) }
    }

    fun onFilterChanged(filter: ContentFilter) {
        _state.update { it.copy(activeFilter = filter) }
    }

    fun onSwipeDay(isNext: Boolean) {
        val currentDay = _state.value.selectedDay
        val newDay = if (isNext) currentDay.plusDays(1) else currentDay.minusDays(1)

        // Clamp to 3-week frame
        if (newDay < minDay || newDay > maxDay) return

        // Update week if we cross boundaries
        val currentWeekStart = _state.value.selectedWeekStart
        val newWeekStart =
            if (newDay.isBefore(currentWeekStart)) {
                newDay.minusDays(newDay.dayOfWeek.value.toLong() - 1)
            } else if (newDay.isAfter(currentWeekStart.plusDays(6))) {
                newDay.minusDays(newDay.dayOfWeek.value.toLong() - 1)
            } else {
                currentWeekStart
            }

        _state.update {
            it.copy(
                selectedDay = newDay,
                selectedWeekStart = newWeekStart,
                canGoBack = newDay > minDay,
                canGoForward = newDay < maxDay,
            )
        }
    }

    fun onSwipeFilter(isNext: Boolean) {
        val filters = ContentFilter.entries
        val currentIndex = filters.indexOf(_state.value.activeFilter)
        val nextIndex =
            if (isNext) {
                (currentIndex + 1) % filters.size
            } else {
                (currentIndex - 1 + filters.size) % filters.size
            }
        _state.update { it.copy(activeFilter = filters[nextIndex]) }
    }

    fun onReleaseSelected(release: Release) {
        _state.update { it.copy(selectedRelease = release) }
    }

    fun onSheetDismissed() {
        _state.update { it.copy(selectedRelease = null) }
    }

    fun onRefresh() {
        val weekStart = _state.value.selectedWeekStart
        syncedWeeks.remove(weekStart) // force re-fetch for the visible week
        viewModelScope.launch { fetchWeek(weekStart) }
    }

    private fun List<Release>.groupAndFilter(
        selectedDay: LocalDate,
    ): Map<LocalDate, List<Release>> {
        return this.filter { it.airDate == selectedDay }
            .groupBy { it.airDate }
    }
}
