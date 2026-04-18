package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun CountdownText(
    airDate: LocalDate,
    airTime: LocalTime?,
    modifier: Modifier = Modifier,
) {
    val targetEpoch = remember(airDate, airTime) {
        val time = airTime ?: LocalTime.of(23, 59, 59)
        ZonedDateTime.of(airDate, time, ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    }

    var remainingMs by remember { mutableLongStateOf(targetEpoch - System.currentTimeMillis()) }

    LaunchedEffect(targetEpoch) {
        while (remainingMs > 0) {
            delay(1_000)
            remainingMs = targetEpoch - System.currentTimeMillis()
        }
    }

    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val days    = (totalSeconds / 86400).toInt()
    val hours   = ((totalSeconds % 86400) / 3600).toInt()
    val minutes = ((totalSeconds % 3600) / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CountdownSegment(days, "d")
        Separator()
        CountdownSegment(hours, "h")
        Separator()
        CountdownSegment(minutes, "m")
        Separator()
        CountdownSegment(seconds, "s")
    }
}

@Composable
private fun CountdownSegment(value: Int, unit: String) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            slideInVertically { it } togetherWith slideOutVertically { -it }
        },
        label = "countdown_$unit",
    ) { v ->
        Text(
            text = "%02d".format(v),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = TextPrimary,
        )
    }
    Text(text = unit, fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
}

@Composable
private fun Separator() {
    Text(text = " ", fontSize = 10.sp, color = TextSecondary)
}
