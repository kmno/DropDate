package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesRed
import com.kmno.dropdate.ui.theme.Surface

@Composable
fun SyncProgressBar(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_progress")
    val xOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "xOffset",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(Dimens.ProgressBarHeight)
                .background(Surface.copy(alpha = 0.1f)),
    ) {
        Box(
            modifier =
                Modifier
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
