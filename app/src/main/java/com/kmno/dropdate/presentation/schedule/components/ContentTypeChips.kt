package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.presentation.schedule.ContentFilter
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesBlue
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary

private val filters = listOf(
    ContentFilter.ALL,
    ContentFilter.MOVIES,
    ContentFilter.SERIES,
    ContentFilter.ANIME,
)

private fun ContentFilter.label() = when (this) {
    ContentFilter.ALL    -> "All"
    ContentFilter.MOVIES -> "Movies"
    ContentFilter.SERIES -> "Series"
    ContentFilter.ANIME  -> "Anime"
}

private fun ContentFilter.accentColor() = when (this) {
    ContentFilter.ALL    -> SeriesBlue
    ContentFilter.MOVIES -> MovieAmber
    ContentFilter.SERIES -> SeriesBlue
    ContentFilter.ANIME  -> AnimePurple
}

@Composable
fun ContentTypeChips(
    activeFilter: ContentFilter,
    onFilterSelected: (ContentFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = filters.indexOf(activeFilter).coerceAtLeast(0)
    val accentColor = activeFilter.accentColor()

    val chipWidths = remember { mutableStateListOf<Float>() }
        .also { list -> repeat(filters.size - list.size) { list.add(0f) } }

    val density = LocalDensity.current
    val offsetDp by animateDpAsState(
        targetValue = with(density) { chipWidths.take(selectedIndex).sum().toDp() },
        animationSpec = tween(200),
        label = "chipIndicatorOffset",
    )
    val indicatorWidthDp by animateDpAsState(
        targetValue = with(density) { chipWidths.getOrElse(selectedIndex) { 0f }.toDp() },
        animationSpec = tween(200),
        label = "chipIndicatorWidth",
    )

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            filters.forEachIndexed { i, filter ->
                val isActive = filter == activeFilter
                val textColor by animateColorAsState(
                    targetValue = if (isActive) accentColor else TextSecondary,
                    animationSpec = tween(200),
                    label = "chipColor_$i",
                )
                Text(
                    text = filter.label(),
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .onGloballyPositioned { coords ->
                            chipWidths[i] = coords.size.width.toFloat()
                        },
                )
            }
        }

        // Sliding underline indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = offsetDp)
                .width(indicatorWidthDp)
                .height(2.dp)
                .background(accentColor, RoundedCornerShape(1.dp))
        )
    }
}
