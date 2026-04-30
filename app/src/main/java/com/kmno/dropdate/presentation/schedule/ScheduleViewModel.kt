package com.kmno.dropdate.presentation.schedule

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmno.dropdate.core.analytics.AnalyticsHelper
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.usecase.CleanupReleasesUseCase
import com.kmno.dropdate.domain.usecase.GetWeekReleasesUseCase
import com.kmno.dropdate.domain.usecase.SearchReleasesUseCase
import com.kmno.dropdate.domain.usecase.SetTrackingUseCase
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

private const val DEBOUNCE = 200L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ScheduleViewModel
    @Inject
    constructor(
        private val getWeekReleases: GetWeekReleasesUseCase,
        private val syncReleases: SyncReleasesUseCase,
        private val cleanupReleases: CleanupReleasesUseCase,
        private val searchReleasesUseCase: SearchReleasesUseCase,
        private val setTracking: SetTrackingUseCase,
        private val analyticsHelper: AnalyticsHelper,
    ) : ViewModel() {
        private val today = LocalDate.now()

        // 3-week frame: last Mon → next Sun
        private val minDay = today.with(DayOfWeek.MONDAY).minusWeeks(1)
        private val maxDay = today.with(DayOfWeek.MONDAY).plusWeeks(1).plusDays(6)

        // Tracks which week-start dates have already been fetched this session
        private val syncedWeeks = mutableSetOf<LocalDate>()

        private val searchQuery = MutableStateFlow("")

        private val _state =
            MutableStateFlow(
                ScheduleUiState(isLoading = true).let { s ->
                    s.copy(
                        canGoBack = s.selectedWeekStart > minDay,
                        canGoForward = s.selectedWeekStart < maxDay.minusDays(6),
                    )
                },
            )
        val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

        init {
            // Observe DB — reacts to week/day/filter changes
            viewModelScope.launch {
                _state
                    .map { it.selectedWeekStart to it.selectedDay }
                    .distinctUntilChanged()
                    .debounce(timeoutMillis = DEBOUNCE)
                    .flatMapLatest { (weekStart, selectedDay) ->
                        _state.update { it.copy(isLoading = true) }
                        getWeekReleases(
                            weekStart,
                            weekStart.plusDays(6),
                        ).map { releases -> releases.groupAndFilter(selectedDay) }
                    }.collectLatest { grouped ->
                        _state.update { it.copy(releases = grouped, isLoading = false) }
                    }
            }

            // Lazy-fetch: sync a week the first time it becomes visible
            viewModelScope.launch {
                _state.map { it.selectedWeekStart }.distinctUntilChanged().collect { weekStart ->
                    if (weekStart !in syncedWeeks) fetchWeek(weekStart)
                }
            }

            // Clean up releases older than last Monday at startup
            viewModelScope.launch { cleanupReleases(minDay) }

            // search in all releases
            viewModelScope.launch {
                searchQuery
                    .debounce(DEBOUNCE)
                    .distinctUntilChanged()
                    .flatMapLatest { query ->
                        if (query.isBlank()) {
                            flowOf(emptyMap())
                        } else {
                            searchReleasesUseCase(query = query).map { releases ->
                                releases.groupBy { it.airDate }
                            }
                        }
                    }.collectLatest { a ->
                        // _state.update { it.copy(releases = a) }
                    }
            }
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
                    canGoBack = newWeekStart > minDay,
                    canGoForward = newWeekStart < maxDay.minusDays(6),
                )
            }
        }

        fun onSwipeWeek(isNext: Boolean) {
            val currentWeekStart = _state.value.selectedWeekStart
            val newWeekStart =
                if (isNext) currentWeekStart.plusWeeks(1) else currentWeekStart.minusWeeks(1)

            // Clamp week start to [minDay, maxDay - 6]
            val clampedWeekStart = newWeekStart.coerceIn(minDay, maxDay.minusDays(6))

            val currentDay = _state.value.selectedDay
            val newDay =
                currentDay
                    .plusWeeks(if (isNext) 1 else -1)
                    .coerceIn(clampedWeekStart, clampedWeekStart.plusDays(6))

            _state.update {
                it.copy(
                    selectedDay = newDay,
                    selectedWeekStart = clampedWeekStart,
                    canGoBack = clampedWeekStart > minDay,
                    canGoForward = clampedWeekStart < maxDay.minusDays(6),
                )
            }
        }

        fun onFilterChanged(filter: ContentFilter) {
            _state.update { it.copy(activeFilter = filter) }
            analyticsHelper.logEvent(
                "filter_changed",
                Bundle().apply {
                    putString("filter_name", filter.name)
                },
            )
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
                    canGoBack = newWeekStart > minDay,
                    canGoForward = newWeekStart < maxDay.minusDays(6),
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
            analyticsHelper.logEvent(
                AnalyticsHelper.Events.CONTENT_SELECTED,
                Bundle().apply {
                    putString(AnalyticsHelper.Params.CONTENT_ID, release.id)
                    putString(AnalyticsHelper.Params.CONTENT_TYPE, release.type.name)
                    putString("release_title", release.title)
                },
            )
        }

        fun onSheetDismissed() {
            _state.update { it.copy(selectedRelease = null) }
        }

        fun onSearchQueryChanged(query: String) {
            _state.update { it.copy(searchQuery = query) }
            // searchQuery.value = query
        }

        fun onSearchToggled() {
            _state.update {
                if (it.isSearchActive) {
                    it.copy(isSearchActive = false, searchQuery = "")
                } else {
                    it.copy(isSearchActive = true)
                }
            }
        }

        fun onRefresh() {
            val weekStart = _state.value.selectedWeekStart
            syncedWeeks.remove(weekStart) // force re-fetch for the visible week
            viewModelScope.launch { fetchWeek(weekStart) }
            analyticsHelper.logEvent("manual_refresh")
        }

        fun onToggleTracking(release: Release) {
            val newTracked = !release.isTracked
            // Optimistically update the sheet so the button flips instantly
            _state.update { s ->
                val updated =
                    s.selectedRelease
                        ?.takeIf { it.id == release.id }
                        ?.copy(isTracked = newTracked)
                s.copy(selectedRelease = updated ?: s.selectedRelease)
            }
            viewModelScope.launch {
                setTracking(release, newTracked).onFailure {
                    // Revert optimistic update on failure
                    _state.update { s ->
                        val reverted =
                            s.selectedRelease
                                ?.takeIf { it.id == release.id }
                                ?.copy(isTracked = release.isTracked)
                        s.copy(selectedRelease = reverted ?: s.selectedRelease)
                    }
                }
            }
        }

        private fun List<Release>.groupAndFilter(selectedDay: LocalDate): Map<LocalDate, List<Release>> =
            this.filter { it.airDate == selectedDay }.groupBy { it.airDate }
    }
