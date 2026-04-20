package com.kmno.dropdate.presentation.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.domain.usecase.GetWeekReleasesUseCase
import com.kmno.dropdate.domain.usecase.SyncReleasesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getWeekReleases: GetWeekReleasesUseCase,
    private val syncReleases: SyncReleasesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState(isLoading = true))
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state
                .map { Triple(it.selectedWeekStart, it.selectedDay, it.activeFilter) }
                .distinctUntilChanged()
                .flatMapLatest { (weekStart, selectedDay, filter) ->
                    _state.update { it.copy(isLoading = true) }
                    getWeekReleases(weekStart, weekStart.plusDays(6))
                        .map { releases ->
                            // println("$$$$$$$$$$$$$$$$$$$$$$$$$ Received releases: $releases")
                            releases.groupAndFilter(filter, selectedDay)
                        }
                }
                .collectLatest { grouped ->
                    _state.update { it.copy(releases = grouped, isLoading = false) }
                    println("Updated state: ${_state.value}")
                }
        }
        onRefresh()
    }

    fun onDaySelected(day: LocalDate) {
        _state.update { it.copy(selectedDay = day) }
    }

    fun onDoubleTapDay(day: LocalDate) {
        // Center the selected day in the week view (3 days before, 3 days after)
        val newWeekStart = day.minusDays(3)
        _state.update { it.copy(selectedDay = day, selectedWeekStart = newWeekStart) }
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

        // Update week if we cross boundaries
        val currentWeekStart = _state.value.selectedWeekStart
        val newWeekStart = if (newDay.isBefore(currentWeekStart)) {
            newDay.minusDays(newDay.dayOfWeek.value.toLong() - 1)
        } else if (newDay.isAfter(currentWeekStart.plusDays(6))) {
            newDay.minusDays(newDay.dayOfWeek.value.toLong() - 1)
        } else {
            currentWeekStart
        }

        _state.update { it.copy(selectedDay = newDay, selectedWeekStart = newWeekStart) }
    }

    fun onSwipeFilter(isNext: Boolean) {
        val filters = ContentFilter.entries
        val currentIndex = filters.indexOf(_state.value.activeFilter)
        val nextIndex = if (isNext) {
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
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, error = null) }
            try {
                syncReleases(weekStart, weekStart.plusDays(6))
                    .onFailure { e ->
                        Log.e("ScheduleViewModel", "Failed to sync releases ${e.message}")
                        _state.update { it.copy(error = e.message) }
                    }
            } finally {
                _state.update { it.copy(isSyncing = false) }
            }
        }
    }

    private fun List<Release>.groupAndFilter(
        filter: ContentFilter,
        selectedDay: LocalDate
    ): Map<LocalDate, List<Release>> {
        val filtered = this.filter { release ->
            val matchesFilter = when (filter) {
                ContentFilter.ALL -> true
                ContentFilter.MOVIES -> release.type == ReleaseType.MOVIE
                ContentFilter.SERIES -> release.type == ReleaseType.SERIES
                ContentFilter.ANIME -> release.type == ReleaseType.ANIME
            }
            matchesFilter && release.airDate == selectedDay
        }
        return filtered.groupBy { it.airDate }
    }
}
