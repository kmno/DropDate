package com.kmno.dropdate.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.domain.usecase.GetWeekReleasesUseCase
import com.kmno.dropdate.domain.usecase.SyncReleasesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getWeekReleases: GetWeekReleasesUseCase,
    private val syncReleases: SyncReleasesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state
                .map { it.selectedWeekStart to it.activeFilter }
                .distinctUntilChanged()
                .flatMapLatest { (weekStart, filter) ->
                    _state.update { it.copy(isLoading = true) }
                    getWeekReleases(weekStart, weekStart.plusDays(6))
                        .map { releases -> releases.groupAndFilter(filter) }
                }
                .collectLatest { grouped ->
                    _state.update { it.copy(releases = grouped, isLoading = false) }
                }
        }
        onRefresh()
    }

    fun onDaySelected(day: LocalDate) {
        _state.update { it.copy(selectedDay = day) }
    }

    fun onWeekChanged(weekStart: LocalDate) {
        _state.update { it.copy(selectedWeekStart = weekStart) }
    }

    fun onFilterChanged(filter: ContentFilter) {
        _state.update { it.copy(activeFilter = filter) }
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
                    .onFailure { e -> _state.update { it.copy(error = e.message) } }
            } finally {
                _state.update { it.copy(isSyncing = false) }
            }
        }
    }

    private fun List<Release>.groupAndFilter(filter: ContentFilter): Map<LocalDate, List<Release>> {
        val filtered = when (filter) {
            ContentFilter.ALL    -> this
            ContentFilter.MOVIES -> filter { it.type == ReleaseType.MOVIE }
            ContentFilter.SERIES -> filter { it.type == ReleaseType.SERIES }
            ContentFilter.ANIME  -> filter { it.type == ReleaseType.ANIME }
        }
        return filtered.groupBy { it.airDate }
    }
}
