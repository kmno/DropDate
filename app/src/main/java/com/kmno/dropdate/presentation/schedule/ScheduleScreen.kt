package com.kmno.dropdate.presentation.schedule

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.presentation.schedule.components.ContentTypeChips
import com.kmno.dropdate.presentation.schedule.components.ReleaseCard
import com.kmno.dropdate.presentation.schedule.components.ReleaseDetailSheet
import com.kmno.dropdate.presentation.schedule.components.ReleaseSection
import com.kmno.dropdate.presentation.schedule.components.WeekScroller
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesRed
import com.kmno.dropdate.ui.theme.Surface
import com.kmno.dropdate.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val view = LocalView.current
    LaunchedEffect(Unit) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    // Anchor scroll to selected day
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val sortedDates = remember(state.releases) { state.releases.keys.sorted() }
    val selectedDayIndex = remember(state.selectedDay, sortedDates) {
        sortedDates.indexOfFirst { it == state.selectedDay }.coerceAtLeast(0)
    }
    LaunchedEffect(state.selectedDay) {
        if (sortedDates.isNotEmpty()) lazyListState.animateScrollToItem(selectedDayIndex)
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Background),
        ) {
            PullToRefreshBox(
                isRefreshing = state.isSyncing,
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Week scroller — sticky header
                    WeekScroller(
                        weekStart = state.selectedWeekStart,
                        selectedDay = state.selectedDay,
                        onDaySelected = viewModel::onDaySelected,
                        onDoubleTapDay = viewModel::onDoubleTapDay,
                        onPreviousClick = { viewModel.onSwipeDay(isNext = false) },
                        onNextClick = { viewModel.onSwipeDay(isNext = true) },
                        modifier = Modifier.pointerInput(Unit) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (totalDrag > 100) viewModel.onSwipeDay(isNext = false)
                                    else if (totalDrag < -100) viewModel.onSwipeDay(isNext = true)
                                    totalDrag = 0f
                                },
                                onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
                            )
                        }
                    )

                    HorizontalDivider(color = Surface, thickness = 1.dp)

                    // Content type filter chips
                    ContentTypeChips(
                        activeFilter = state.activeFilter,
                        onFilterSelected = viewModel::onFilterChanged,
                        modifier = Modifier.pointerInput(Unit) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (totalDrag > 100) viewModel.onSwipeFilter(isNext = false)
                                    else if (totalDrag < -100) viewModel.onSwipeFilter(isNext = true)
                                    totalDrag = 0f
                                },
                                onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
                            )
                        }
                    )

                    HorizontalDivider(color = Surface, thickness = 1.dp)

                    Spacer(Modifier.height(8.dp))

                    // Error display
                    state.error?.let { error ->
                        Text(
                            text = "Error: $error",
                            color = SeriesRed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }

                    // Feed — animated on filter change
                    AnimatedContent(
                        targetState = state.activeFilter,
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally { it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 4 })
                        },
                        label = "feedTransition",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) { filter ->
                        val flat = state.releases.values.flatten()

                        if (flat.isEmpty() && !state.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No releases found for this day.\nTry another date or pull to refresh.",
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp
                                )
                            }
                        } else if (filter == ContentFilter.ALL) {
                            // Category rows
                            val allReleases = state.releases.values.flatten()
                            val movies = allReleases.filter { it.type == ReleaseType.MOVIE }
                            val series = allReleases.filter { it.type == ReleaseType.SERIES }
                            val anime = allReleases.filter { it.type == ReleaseType.ANIME }

                            LazyColumn(
                                state = lazyListState,
                                contentPadding = PaddingValues(bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                if (movies.isNotEmpty()) item {
                                    ReleaseSection(
                                        title = "Movies",
                                        accentColor = MovieAmber,
                                        releases = movies,
                                        onReleaseClick = viewModel::onReleaseSelected,
                                    )
                                }
                                if (series.isNotEmpty()) item {
                                    ReleaseSection(
                                        title = "Series",
                                        accentColor = SeriesRed,
                                        releases = series,
                                        onReleaseClick = viewModel::onReleaseSelected,
                                    )
                                }
                                if (anime.isNotEmpty()) item {
                                    ReleaseSection(
                                        title = "Anime",
                                        accentColor = AnimePurple,
                                        releases = anime,
                                        onReleaseClick = viewModel::onReleaseSelected,
                                    )
                                }
                            }
                        } else {
                            // Flat vertical list
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                itemsIndexed(flat, key = { _, r -> r.id }) { index, release ->
                                    ReleaseCard(
                                        release = release,
                                        index = index,
                                        onClick = viewModel::onReleaseSelected,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Detail bottom sheet
            state.selectedRelease?.let { release ->
                ReleaseDetailSheet(
                    release = release,
                    onDismiss = viewModel::onSheetDismissed,
                )
            }
        }

    }
}

