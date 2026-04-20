package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.presentation.schedule.ContentFilter
import com.kmno.dropdate.ui.theme.All
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.DropDateTheme
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesRed
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
    ContentFilter.ALL -> All
    ContentFilter.MOVIES -> MovieAmber
    ContentFilter.SERIES -> SeriesRed
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
    val animatedAccentColor by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(200),
        label = "indicatorColor",
    )

    val chipWidths = remember { mutableStateListOf(*Array(filters.size) { 0f }) }
    val chipOffsets = remember { mutableStateListOf(*Array(filters.size) { 0f }) }

    val density = LocalDensity.current
    val offsetDp by animateDpAsState(
        targetValue = with(density) { chipOffsets.getOrElse(selectedIndex) { 0f }.toDp() },
        animationSpec = tween(200),
        label = "chipIndicatorOffset",
    )
    val indicatorWidthDp by animateDpAsState(
        targetValue = with(density) { chipWidths.getOrElse(selectedIndex) { 0f }.toDp() },
        animationSpec = tween(200),
        label = "chipIndicatorWidth",
    )

    Box(modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)) {
        // Sliding underline indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = offsetDp)
                .width(indicatorWidthDp)
                .height(2.dp)
                .background(animatedAccentColor, RoundedCornerShape(1.dp))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEachIndexed { i, filter ->
                val isActive = filter == activeFilter
                val textColor by animateColorAsState(
                    targetValue = if (isActive) animatedAccentColor else TextSecondary,
                    animationSpec = tween(200),
                    label = "chipColor_$i",
                )
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            chipWidths[i] = coords.size.width.toFloat()
                            chipOffsets[i] = coords.positionInParent().x
                        }
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label(),
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080810, widthDp = 360)
@Composable
private fun ContentTypeChipsAllPreview() {
    DropDateTheme { ContentTypeChips(activeFilter = ContentFilter.ALL, onFilterSelected = {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF080810, widthDp = 360)
@Composable
private fun ContentTypeChipsMoviesPreview() {
    DropDateTheme { ContentTypeChips(activeFilter = ContentFilter.MOVIES, onFilterSelected = {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF080810, widthDp = 360)
@Composable
private fun ContentTypeChipsAnimePreview() {
    DropDateTheme { ContentTypeChips(activeFilter = ContentFilter.ANIME, onFilterSelected = {}) }
}
