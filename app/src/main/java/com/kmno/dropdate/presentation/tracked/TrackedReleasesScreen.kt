package com.kmno.dropdate.presentation.tracked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmno.dropdate.BuildConfig
import com.kmno.dropdate.R
import com.kmno.dropdate.presentation.schedule.components.ReleaseCard
import com.kmno.dropdate.presentation.schedule.components.ReleaseDetailSheet
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackedReleasesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackedReleasesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tracked_release),
                        color = TextPrimary,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Background,
                    ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSecondary,
                        )
                    }
                },
                actions = {
                    if (BuildConfig.DEBUG) {
                        IconButton(onClick = viewModel::triggerTestNotification) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Test notification",
                                tint = TextSecondary,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Background),
        ) {
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
                    state.releases,
                    key = { _, r -> r.id },
                ) { index, release ->
                    ReleaseCard(
                        release = release,
                        index = index,
                        isTrackMode = true,
                        onClick = viewModel::onReleaseSelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Detail bottom sheet
            state.selectedRelease?.let { release ->
                ReleaseDetailSheet(
                    release = release,
                    onDismiss = viewModel::onSheetDismissed,
                    onToggleTrack = { viewModel.onToggleTracking(release) },
                )
            }
        }
    }
}
