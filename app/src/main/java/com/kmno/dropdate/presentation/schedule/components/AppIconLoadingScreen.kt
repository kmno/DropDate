package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kmno.dropdate.R
import com.kmno.dropdate.ui.theme.All
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesRed
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary

@Composable
fun AppIconLoadingScreen(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition("iconLoading")

    // Whole group bobs up and down
    val floatY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -18f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "floatY",
    )

    // Each dot pulses with a 200 ms stagger via phase offset
    val scale0 by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                tween(900, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
                StartOffset(0, StartOffsetType.FastForward),
            ),
        label = "s0",
    )
    val scale1 by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                tween(900, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
                StartOffset(200, StartOffsetType.FastForward),
            ),
        label = "s1",
    )
    val scale2 by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                tween(900, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
                StartOffset(400, StartOffsetType.FastForward),
            ),
        label = "s2",
    )
    val scale3 by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                tween(900, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
                StartOffset(600, StartOffsetType.FastForward),
            ),
        label = "s3",
    )

    Box(
        modifier =
            modifier
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
        modifier =
            Modifier
                .size(Dimens.LoadingDotSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.background(color, CircleShape),
    )
}
