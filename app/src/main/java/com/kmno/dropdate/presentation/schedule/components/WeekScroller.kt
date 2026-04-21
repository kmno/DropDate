package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.ui.theme.All
import com.kmno.dropdate.ui.theme.DropDateTheme
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val DayItemWidth = 50.dp

@Composable
fun WeekScroller(
    weekStart: LocalDate,
    selectedDay: LocalDate,
    today: LocalDate = LocalDate.now(),
    canGoBack: Boolean = true,
    canGoForward: Boolean = true,
    onDaySelected: (LocalDate) -> Unit,
    onDoubleTapDay: (LocalDate) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val selectedIndex = days.indexOfFirst { it == selectedDay }.coerceAtLeast(0)

    var totalWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousClick,
            enabled = canGoBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Day",
                tint = if (canGoBack) TextSecondary else TextSecondary.copy(alpha = 0.25f),
                modifier = Modifier.size(20.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { totalWidth = it.size.width }
        ) {
            val itemWidthPx = totalWidth / 7f
            val itemWidthDp = with(density) { itemWidthPx.toDp() }

            val pillOffset by animateDpAsState(
                targetValue = itemWidthDp * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "pillOffset",
            )

            // Sliding pill background
            if (totalWidth > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(itemWidthDp)
                        .height(65.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(All.copy(alpha = 0.15f))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                days.forEach { day ->
                    val isSelected = day == selectedDay
                    val isToday = day == today
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(day) {
                                detectTapGestures(
                                    onTap = { onDaySelected(day) },
                                    onDoubleTap = { onDoubleTapDay(day) }
                                )
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = day.dayOfWeek.getDisplayName(
                                TextStyle.SHORT,
                                Locale.getDefault()
                            )
                                .take(3).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) All else TextSecondary,
                        )
                        Text(
                            text = day.dayOfMonth.toString(),
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TextPrimary else TextSecondary,
                        )
                        if (isToday) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(All, CircleShape)
                            )
                        } else {
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onNextClick,
            enabled = canGoForward,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next Day",
                tint = if (canGoForward) TextSecondary else TextSecondary.copy(alpha = 0.25f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080810, widthDp = 360)
@Composable
private fun WeekScrollerPreview() {
    val monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
    DropDateTheme {
        WeekScroller(
            weekStart = monday,
            selectedDay = monday.plusDays(2),
            today = monday.plusDays(2),
            onDaySelected = {},
            onDoubleTapDay = {},
            onPreviousClick = {},
            onNextClick = {},
        )
    }
}
