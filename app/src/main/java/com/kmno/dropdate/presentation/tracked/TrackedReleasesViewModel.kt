package com.kmno.dropdate.presentation.tracked

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.kmno.dropdate.core.analytics.AnalyticsHelper
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.usecase.GetTrackedReleasesUseCase
import com.kmno.dropdate.domain.usecase.SetTrackingUseCase
import com.kmno.dropdate.worker.AiringReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Suppress("MagicNumber")
@HiltViewModel
class TrackedReleasesViewModel
    @Inject
    constructor(
        private val getTrackedReleasesUseCase: GetTrackedReleasesUseCase,
        private val setTracking: SetTrackingUseCase,
        private val analyticsHelper: AnalyticsHelper,
        private val workManager: WorkManager,
    ) : ViewModel() {
        private val _state = MutableStateFlow(TrackedUiState(loading = true))
        val state = _state.asStateFlow()

        init {
            viewModelScope.launch {
                getTrackedReleasesUseCase()
                    .debounce(80L)
                    .collect { releases ->
                        _state.update { it.copy(loading = false, releases = releases) }
                    }
            }
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

        fun triggerTestNotification() {
            AiringReminderWorker.scheduleTest(workManager)
        }

        fun onSheetDismissed() {
            _state.update { it.copy(selectedRelease = null) }
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
    }
