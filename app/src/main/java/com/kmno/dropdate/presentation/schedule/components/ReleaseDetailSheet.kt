package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.ReleasedGreen
import com.kmno.dropdate.ui.theme.SeriesBlue
import com.kmno.dropdate.ui.theme.Surface
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseDetailSheet(
    release: Release,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accentColor = when (release.type) {
        ReleaseType.MOVIE  -> MovieAmber
        ReleaseType.SERIES -> SeriesBlue
        ReleaseType.ANIME  -> AnimePurple
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            // Hero — blurred backdrop + poster
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                // Backdrop (blurred via heavy downscale)
                AsyncImage(
                    model = release.backdropUrl ?: release.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                // Dark scrim
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.4f),
                                1f to Background,
                            )
                        )
                )
                // Poster thumbnail
                AsyncImage(
                    model = release.posterUrl,
                    contentDescription = release.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp)
                        .size(width = 90.dp, height = 130.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }

            // Metadata
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))

                // Title
                Text(
                    text = release.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )

                Spacer(Modifier.height(4.dp))

                // Type · Year · Episode
                val metaRemainder = buildString {
                    release.airDate.year.let { append("$it") }
                    release.episodeLabel?.let { append(" · $it") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = release.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                    )
                    if (metaRemainder.isNotBlank()) {
                        Text(text = " · $metaRemainder", fontSize = 13.sp, color = TextSecondary)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Star rating
                release.rating?.let { rating ->
                    StarRating(rating = rating, accentColor = accentColor)
                    Spacer(Modifier.height(6.dp))
                }

                // Platform dot
                release.platform?.let { platform ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accentColor, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = platform, fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Synopsis
                release.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
                    SynopsisSection(synopsis = synopsis, accentColor = accentColor)
                    Spacer(Modifier.height(16.dp))
                }

                // CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (release.status == ReleaseStatus.RELEASED) ReleasedGreen else Surface)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (release.status == ReleaseStatus.RELEASED) {
                        Text("▶  Watch Now", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        CountdownText(airDate = release.airDate, airTime = release.airTime)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StarRating(rating: Float, accentColor: Color) {
    var animatedRating by remember { mutableStateOf(0f) }
    val displayRating by animateFloatAsState(
        targetValue = animatedRating,
        animationSpec = tween(durationMillis = 600),
        label = "starFill",
    )
    LaunchedEffect(Unit) { animatedRating = rating }

    val starCount = (displayRating / 2f).roundToInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            val filled = i < starCount
            Text(
                text = if (filled) "★" else "☆",
                color = if (filled) accentColor else TextSecondary,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = "${"%.1f".format(rating)} / 10",
            fontSize = 13.sp,
            color = TextSecondary,
        )
    }
}

@Composable
private fun SynopsisSection(synopsis: String, accentColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    val preview = synopsis.take(180).let { if (synopsis.length > 180) "$it…" else it }

    Column {
        Text(
            text = "Synopsis",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
        )
        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
            },
            label = "synopsisExpand",
        ) { isExpanded ->
            Text(
                text = if (isExpanded) synopsis else preview,
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 20.sp,
            )
        }

        if (synopsis.length > 180) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    text = if (expanded) "Show less ↑" else "Show more ↓",
                    color = accentColor,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
