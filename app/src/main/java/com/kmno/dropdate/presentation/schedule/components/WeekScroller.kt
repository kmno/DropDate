package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.ui.theme.SeriesBlue
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val DayItemWidth = 44.dp

@Composable
fun WeekScroller(
    weekStart: LocalDate,
    selectedDay: LocalDate,
    today: LocalDate = LocalDate.now(),
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val selectedIndex = days.indexOfFirst { it == selectedDay }.coerceAtLeast(0)

    val pillOffset by animateDpAsState(
        targetValue = DayItemWidth * selectedIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pillOffset",
    )

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Sliding pill background
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(DayItemWidth)
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SeriesBlue.copy(alpha = 0.2f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            days.forEachIndexed { _, day ->
                val isSelected = day == selectedDay
                val isToday = day == today
                Column(
                    modifier = Modifier
                        .width(DayItemWidth)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDaySelected(day) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .take(3).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) SeriesBlue else TextSecondary,
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
                                .background(SeriesBlue, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
