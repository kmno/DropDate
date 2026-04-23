package com.kmno.dropdate.presentation.schedule

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmno.dropdate.R
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.presentation.schedule.components.ContentTypeChips
import com.kmno.dropdate.presentation.schedule.components.ReleaseCard
import com.kmno.dropdate.presentation.schedule.components.ReleaseDetailSheet
import com.kmno.dropdate.presentation.schedule.components.ReleaseSection
import com.kmno.dropdate.presentation.schedule.components.TopBar
import com.kmno.dropdate.presentation.schedule.components.WeekScroller
import com.kmno.dropdate.ui.theme.All
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesRed
import com.kmno.dropdate.ui.theme.Surface
import com.kmno.dropdate.ui.theme.SurfaceAlt
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = hiltViewModel()) {
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

    Scaffold(
        topBar = { TopBar(onRefresh = viewModel::onRefresh) }
    ) { paddingValues ->
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
                        modifier = Modifier.pointerInput(Unit) {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.PaddingLarge),
                            textAlign = TextAlign.Center,
                            fontSize = Dimens.FontNormal,
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
                            val allReleases = state.releases.values.flatten()
                            val movies = allReleases.filter { it.type == ReleaseType.MOVIE }
                            val series = allReleases.filter { it.type == ReleaseType.SERIES }
                            val anime = allReleases.filter { it.type == ReleaseType.ANIME }

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
                                contentPadding = PaddingValues(
                                    start = Dimens.PaddingMedium,
                                    end = Dimens.PaddingMedium,
                                    top = Dimens.PaddingSmall,
                                    bottom = 120.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingNormal),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingNormal),
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
            } // end PullToRefreshBox

            // Floating Week Scroller at the bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        horizontal = Dimens.PaddingExtraLarge, vertical = Dimens.PaddingLarge
                    )
                    .shadow(
                        elevation = 8.dp, shape = RoundedCornerShape(Dimens.FloatingBoxCornerRadius)
                    )
                    .background(SurfaceAlt, RoundedCornerShape(Dimens.FloatingBoxCornerRadius))
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
                    modifier = Modifier.pointerInput(Unit) {
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

@Composable
private fun AppIconLoadingScreen() {
    val transition = rememberInfiniteTransition("iconLoading")

    // Whole group bobs up and down
    val floatY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatY",
    )

    // Each dot pulses with a 200 ms stagger via phase offset
    val scale0 by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            StartOffset(0, StartOffsetType.FastForward),
        ),
        label = "s0",
    )
    val scale1 by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            StartOffset(200, StartOffsetType.FastForward),
        ),
        label = "s1",
    )
    val scale2 by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            StartOffset(400, StartOffsetType.FastForward),
        ),
        label = "s2",
    )
    val scale3 by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            StartOffset(600, StartOffsetType.FastForward),
        ),
        label = "s3",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 2×2 icon — mirrors the launcher icon layout
            Column(
                modifier = Modifier.graphicsLayer { translationY = floatY },
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
                    LoadingDot(color = All, scale = scale0) // top-left  — blue
                    LoadingDot(color = MovieAmber, scale = scale1) // top-right — amber
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
                    LoadingDot(color = SeriesRed, scale = scale2) // bot-left  — red
                    LoadingDot(color = AnimePurple, scale = scale3) // bot-right — purple
                }
            }

            Spacer(Modifier.height(Dimens.PaddingLarge + 4.dp))

            Text(
                text = "DropDate",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )

            Spacer(Modifier.height(Dimens.SpacingMedium))

            Text(
                text = stringResource(R.string.fetching_releases),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun LoadingDot(
    color: Color,
    scale: Float,
) {
    Box(
        modifier = Modifier
            .size(Dimens.LoadingDotSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(color, CircleShape),
    )
}

@Composable
private fun SyncProgressBar(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_progress")
    val xOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "xOffset",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ProgressBarHeight)
            .background(Surface.copy(alpha = 0.1f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .fillMaxHeight()
                .graphicsLayer { translationX = size.width * xOffset }
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MovieAmber.copy(alpha = 0f),
                            MovieAmber,
                            SeriesRed,
                            AnimePurple,
                            AnimePurple.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
    }
}
