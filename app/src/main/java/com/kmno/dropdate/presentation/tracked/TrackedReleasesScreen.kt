package com.kmno.dropdate.presentation.tracked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmno.dropdate.BuildConfig
import com.kmno.dropdate.R
import com.kmno.dropdate.presentation.OneTimeEvents
import com.kmno.dropdate.presentation.schedule.components.AppSnackbar
import com.kmno.dropdate.presentation.schedule.components.LazyGrid
import com.kmno.dropdate.presentation.schedule.components.ReleaseDetailSheet
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackedReleasesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackedReleasesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is OneTimeEvents.SnackAlert -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short,
                    )
                }
                is OneTimeEvents.DialogAlert -> {}
            }
        }
    }

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
                        IconButton(onClick = viewModel::triggerRealDataTest) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Test real data notification",
                                tint = TextSecondary,
                            )
                        }
                        IconButton(onClick = viewModel::triggerTestNotification) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Test dummy notification",
                                tint = TextSecondary,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = {
            if (state.selectedRelease == null) {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    AppSnackbar(data)
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Background),
            contentAlignment = Alignment.Center,
        ) {
            if (state.releases.isNotEmpty()) {
                LazyGrid(
                    releases = state.releases,
                    viewModel::onReleaseSelected,
                )

                // Detail bottom sheet
                state.selectedRelease?.let { release ->
                    ReleaseDetailSheet(
                        release = release,
                        onDismiss = viewModel::onSheetDismissed,
                        onToggleTrack = { viewModel.onToggleTracking(release) },
                        snackbarHostState = snackbarHostState,
                    )
                }
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.no_trackings_found),
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = Dimens.FontNormal,
                )
            }
        }
    }
}
