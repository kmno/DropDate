package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.ui.theme.DropDateTheme
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
            val waitTime = when {
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
    val seconds = (totalSeconds % 60).toInt()

    val labelText = when {
        days > 1 -> "Drops in $days days"
        days == 1 -> "Drops tomorrow"
        hours > 0 -> "Drops in ${hours}h ${minutes}m"
        minutes > 0 -> "Drops in ${minutes}m"
        else -> "Drops in < 1m"
    }

    Text(
        text = labelText,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
    )
}

@Composable
private fun CountdownSegment(value: Int, unit: String) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            slideInVertically { -it } togetherWith slideOutVertically { it }
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
