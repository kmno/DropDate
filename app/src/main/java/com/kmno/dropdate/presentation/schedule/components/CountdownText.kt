package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.kmno.dropdate.R
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.DropDateTheme
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CountdownText(
    airDate: LocalDate,
    airTime: LocalTime?,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
) {
    val targetEpoch =
        remember(airDate, airTime) {
            val time = airTime ?: LocalTime.of(23, 59, 59)
            ZonedDateTime
                .of(airDate, time, ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

    var remainingMs by remember { mutableLongStateOf(targetEpoch - System.currentTimeMillis()) }

    LaunchedEffect(targetEpoch) {
        while (remainingMs > 0) {
            val waitTime =
                when {
                    remainingMs > 3600_000 -> 60_000L // Update every minute if > 1h
                    else -> 1_000L // Update every second if < 1h
                }
            delay(waitTime)
            remainingMs = targetEpoch - System.currentTimeMillis()
        }
    }

    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val days = (totalSeconds / 86400).toInt()
    val hours = ((totalSeconds % 86400) / 3600).toInt()
    val minutes = ((totalSeconds % 3600) / 60).toInt()

    val labelText =
        when {
            days > 1 -> stringResource(R.string.drops_in_days, days)
            days == 1 -> stringResource(R.string.drops_tomorrow)
            hours > 0 -> stringResource(R.string.drops_in_hours_minutes, hours, minutes)
            minutes > 0 -> stringResource(R.string.drops_in_minutes, minutes)
            else -> stringResource(R.string.drops_in_less_than_minute)
        }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }
    val tbdText = stringResource(R.string.time_tbd)
    val exactDateText =
        remember(airDate, airTime, tbdText) {
            val datePart = airDate.format(dateFormatter)
            val timePart = "• ${airTime?.format(timeFormatter) ?: tbdText}"
            "$datePart $timePart"
        }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDetails) {
            val infiniteTransition = rememberInfiniteTransition(label = "clock_tick")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "rotation",
            )
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(Dimens.IconSmall)
                        .graphicsLayer(rotationZ = rotation),
                tint = TextPrimary,
            )
            Spacer(modifier = Modifier.width(Dimens.SpacingNormal))
        }

        Column {
            AnimatedContent(
                targetState = labelText,
                transitionSpec = {
                    slideInVertically { -it } togetherWith slideOutVertically { it }
                },
                label = "label_animation",
            ) { text ->
                Text(
                    text = text,
                    fontSize = Dimens.FontNormal,
                    fontWeight =
                        if (showDetails) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                    color = TextPrimary,
                )
            }
            if (showDetails) {
                Text(
                    text = exactDateText,
                    fontSize = Dimens.FontExtraSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080810)
@Composable
private fun CountdownTextUpcomingPreview() {
    DropDateTheme {
        CountdownText(
            airDate = LocalDate.now().plusDays(3),
            airTime = LocalTime.of(20, 0),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080810)
@Composable
private fun CountdownTextNoTimePreview() {
    DropDateTheme {
        CountdownText(
            airDate = LocalDate.now().plusDays(12),
            airTime = null,
        )
    }
}
