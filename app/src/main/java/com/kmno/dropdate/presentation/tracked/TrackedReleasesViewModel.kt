package com.kmno.dropdate.presentation.tracked

import android.os.Bundle
import androidx.work.WorkManager
import com.kmno.dropdate.core.analytics.AnalyticsHelper
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.usecase.GetTrackedReleasesUseCase
import com.kmno.dropdate.domain.usecase.SetTrackingUseCase
import com.kmno.dropdate.presentation.BaseViewModel
import com.kmno.dropdate.worker.AiringReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(FlowPreview::class)
@Suppress("MagicNumber")
@HiltViewModel
class TrackedReleasesViewModel
    @Inject
    constructor(
        private val getTrackedReleasesUseCase: GetTrackedReleasesUseCase,
        private val setTracking: SetTrackingUseCase,
        private val workManager: WorkManager,
    ) : BaseViewModel() {
        private val _state = MutableStateFlow(TrackedUiState(loading = true))
        val state = _state.asStateFlow()

        init {
            launch {
                getTrackedReleasesUseCase()
                    .debounce(80L)
                    .collect { releases ->
                        _state.update { it.copy(loading = false, releases = releases) }
                    }
            }
        }

        fun onReleaseSelected(release: Release) {
            _state.update { it.copy(selectedRelease = release) }
            logAnalyticsEvent(
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

            logAnalyticsEvent(
                if (newTracked) AnalyticsHelper.Events.FAVORITE_ADDED else AnalyticsHelper.Events.FAVORITE_REMOVED,
                Bundle().apply {
                    putString(AnalyticsHelper.Params.CONTENT_ID, release.id)
                    putString(AnalyticsHelper.Params.CONTENT_TYPE, release.type.name)
                    putString("release_title", release.title)
                },
            )

            // Optimistically update the sheet so the button flips instantly
            _state.update { s ->
                val updated =
                    s.selectedRelease
                        ?.takeIf { it.id == release.id }
                        ?.copy(isTracked = newTracked)
                s.copy(selectedRelease = updated ?: s.selectedRelease)
            }
            launch {
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
