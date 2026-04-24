package com.kmno.dropdate.presentation.schedule

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmno.dropdate.R
import com.kmno.dropdate.core.analytics.LocalAnalyticsHelper
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.presentation.schedule.components.AppIconLoadingScreen
import com.kmno.dropdate.presentation.schedule.components.ContentTypeChips
import com.kmno.dropdate.presentation.schedule.components.ReleaseCard
import com.kmno.dropdate.presentation.schedule.components.ReleaseDetailSheet
import com.kmno.dropdate.presentation.schedule.components.ReleaseSection
import com.kmno.dropdate.presentation.schedule.components.SyncProgressBar
import com.kmno.dropdate.presentation.schedule.components.TopBar
import com.kmno.dropdate.presentation.schedule.components.WeekScroller
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesRed
import com.kmno.dropdate.ui.theme.Surface
import com.kmno.dropdate.ui.theme.SurfaceAlt
import com.kmno.dropdate.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val analyticsHelper = LocalAnalyticsHelper.current
    LaunchedEffect(Unit) {
        analyticsHelper.logScreenView("Schedule")
    }

    val view = LocalView.current
    LaunchedEffect(Unit) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    // Anchor scroll to selected day
    val lazyListState = rememberLazyListState()
    val sortedDates = remember(state.releases) { state.releases.keys.sorted() }
    val selectedDayIndex =
        remember(state.selectedDay, sortedDates) {
            sortedDates.indexOfFirst { it == state.selectedDay }.coerceAtLeast(0)
        }
    LaunchedEffect(state.selectedDay) {
        if (sortedDates.isNotEmpty()) lazyListState.animateScrollToItem(selectedDayIndex)
    }

    Scaffold(
        topBar = { TopBar(onRefresh = viewModel::onRefresh) },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Background),
        ) {
            PullToRefreshBox(
                isRefreshing = state.isSyncing,
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
                indicator = {},
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Custom linear sync indicator using section colors
                    if (state.isSyncing) {
                        SyncProgressBar()
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    // Content type filter chips
                    ContentTypeChips(
                        activeFilter = state.activeFilter,
                        onFilterSelected = viewModel::onFilterChanged,
                        modifier =
                            Modifier.pointerInput(Unit) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (totalDrag > 100) {
                                            viewModel.onSwipeFilter(isNext = false)
                                        } else if (totalDrag < -100) {
                                            viewModel.onSwipeFilter(isNext = true)
                                        }
                                        totalDrag = 0f
                                    },
                                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                                )
                            },
                    )

                    HorizontalDivider(color = Surface, thickness = 1.dp)

                    // Error display
                    state.error?.let { error ->
                        Text(
                            text = stringResource(R.string.error_prefix, error),
                            color = SeriesRed,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.PaddingLarge),
                            textAlign = TextAlign.Center,
                            fontSize = Dimens.FontNormal,
                        )
                    }

                    // Feed — animated on filter change
                    val allReleasesForDay =
                        remember(state.releases) {
                            state.releases.values.flatten()
                        }

                    AnimatedContent(
                        targetState = state.activeFilter,
                        transitionSpec = {
                            (
                                fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                    slideInHorizontally(
                                        initialOffsetX = { it / 4 },
                                        animationSpec = tween(220, delayMillis = 90),
                                    )
                            ) togetherWith
                                (
                                    fadeOut(animationSpec = tween(90)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { -it / 4 },
                                            animationSpec = tween(90),
                                        )
                                )
                        },
                        label = "feedTransition",
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                    ) { filter ->
                        val filteredReleases =
                            remember(allReleasesForDay, filter) {
                                when (filter) {
                                    ContentFilter.ALL -> allReleasesForDay
                                    ContentFilter.MOVIES -> allReleasesForDay.filter { it.type == ReleaseType.MOVIE }
                                    ContentFilter.SERIES -> allReleasesForDay.filter { it.type == ReleaseType.SERIES }
                                    ContentFilter.ANIME -> allReleasesForDay.filter { it.type == ReleaseType.ANIME }
                                }
                            }

                        if (filteredReleases.isEmpty() && !state.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.no_releases_found),
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = Dimens.FontNormal,
                                )
                            }
                        } else if (filter == ContentFilter.ALL) {
                            // Category rows
                            val movies = filteredReleases.filter { it.type == ReleaseType.MOVIE }
                            val series = filteredReleases.filter { it.type == ReleaseType.SERIES }
                            val anime = filteredReleases.filter { it.type == ReleaseType.ANIME }

                            LazyColumn(
                                state = lazyListState,
                                contentPadding = PaddingValues(bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                            ) {
                                if (movies.isNotEmpty()) {
                                    item {
                                        ReleaseSection(
                                            title = stringResource(R.string.filter_movies),
                                            accentColor = MovieAmber,
                                            releases = movies,
                                            onReleaseClick = viewModel::onReleaseSelected,
                                            onMoreClick = { viewModel.onFilterChanged(ContentFilter.MOVIES) },
                                        )
                                    }
                                }
                                if (series.isNotEmpty()) {
                                    item {
                                        ReleaseSection(
                                            title = stringResource(R.string.filter_series),
                                            accentColor = SeriesRed,
                                            releases = series,
                                            onReleaseClick = viewModel::onReleaseSelected,
                                            onMoreClick = { viewModel.onFilterChanged(ContentFilter.SERIES) },
                                        )
                                    }
                                }
                                if (anime.isNotEmpty()) {
                                    item {
                                        ReleaseSection(
                                            title = stringResource(R.string.filter_anime),
                                            accentColor = AnimePurple,
                                            releases = anime,
                                            onReleaseClick = viewModel::onReleaseSelected,
                                            onMoreClick = { viewModel.onFilterChanged(ContentFilter.ANIME) },
                                        )
                                    }
                                }
                            }
                        } else {
                            // Lazy Grid of releases
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding =
                                    PaddingValues(
                                        start = Dimens.PaddingMedium,
                                        end = Dimens.PaddingMedium,
                                        top = Dimens.PaddingSmall,
                                        bottom = 120.dp,
                                    ),
                                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingNormal),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingNormal),
                            ) {
                                itemsIndexed(
                                    filteredReleases,
                                    key = { _, r -> r.id },
                                ) { index, release ->
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
            } // end PullToRefreshBox

            // Floating Week Scroller at the bottom
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            horizontal = Dimens.PaddingExtraLarge,
                            vertical = Dimens.PaddingLarge,
                        ).shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(Dimens.FloatingBoxCornerRadius),
                        ).background(SurfaceAlt, RoundedCornerShape(Dimens.FloatingBoxCornerRadius))
                        .padding(vertical = Dimens.SpacingSmall),
            ) {
                WeekScroller(
                    weekStart = state.selectedWeekStart,
                    selectedDay = state.selectedDay,
                    canGoBack = state.canGoBack,
                    canGoForward = state.canGoForward,
                    onDaySelected = viewModel::onDaySelected,
                    onDoubleTapDay = viewModel::onDoubleTapDay,
                    onPreviousClick = { viewModel.onSwipeDay(isNext = false) },
                    onNextClick = { viewModel.onSwipeDay(isNext = true) },
                    modifier =
                        Modifier.pointerInput(Unit) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (totalDrag > 100) {
                                        viewModel.onSwipeDay(isNext = false)
                                    } else if (totalDrag < -100) {
                                        viewModel.onSwipeDay(isNext = true)
                                    }
                                    totalDrag = 0f
                                },
                                onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                            )
                        },
                )
            }

            // Detail bottom sheet
            state.selectedRelease?.let { release ->
                ReleaseDetailSheet(
                    release = release,
                    onDismiss = viewModel::onSheetDismissed,
                )
            }

            // Full-screen loading on first launch — fades in when DB empty + loading/syncing
            val showLoading = state.releases.isEmpty() && (state.isLoading || state.isSyncing)
            AnimatedVisibility(
                visible = showLoading,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(600)),
            ) {
                AppIconLoadingScreen()
            }
        }
    }
}
