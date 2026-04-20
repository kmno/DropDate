package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.ui.theme.DropDateTheme
import com.kmno.dropdate.ui.theme.ReleasedGreen
import com.kmno.dropdate.ui.theme.TextPrimary

@Composable
fun WatchBadge(platform: String?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "watchBadgeScale",
    )

    LaunchedEffect(Unit) { visible = true }

    Row(
        modifier = modifier
            .scale(scale)
            .background(ReleasedGreen, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = platform ?: "Watch",
            tint = TextPrimary,
            modifier = Modifier.size(10.dp),
        )
        if (!platform.isNullOrBlank()) {
            Spacer(Modifier.width(3.dp))
            Text(
                text = platform,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080810)
@Composable
private fun WatchBadgeWithPlatformPreview() {
    DropDateTheme { WatchBadge(platform = "Netflix") }
}

@Preview(showBackground = true, backgroundColor = 0xFF080810)
@Composable
private fun WatchBadgeNoPlatformPreview() {
    DropDateTheme { WatchBadge(platform = null) }
}
