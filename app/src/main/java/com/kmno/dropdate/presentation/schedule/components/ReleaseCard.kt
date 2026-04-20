package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.DropDateTheme
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesRed
import com.kmno.dropdate.ui.theme.Surface
import com.kmno.dropdate.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import java.time.LocalDate

private val CardWidth  = 140.dp
private val CardHeight = 210.dp

private fun platformLabel(name: String): String = when {
    name.contains("netflix", ignoreCase = true) -> "NETFLIX"
    name.contains("max", ignoreCase = true) -> "MAX"
    name.contains("hbo", ignoreCase = true) -> "HBO"
    name.contains("hulu", ignoreCase = true) -> "HULU"
    name.contains("prime", ignoreCase = true) -> "PRIME"
    name.contains("amazon", ignoreCase = true) -> "PRIME"
    name.contains("disney", ignoreCase = true) -> "DISNEY+"
    name.contains("apple", ignoreCase = true) -> "APPLE TV+"
    name.contains("peacock", ignoreCase = true) -> "PEACOCK"
    name.contains("paramount", ignoreCase = true) -> "P+"
    name.contains("crunchyroll", ignoreCase = true) -> "CRUNCHY"
    name.contains("funimation", ignoreCase = true) -> "FUNI"
    name.contains("showtime", ignoreCase = true) -> "SHOWTIME"
    name.contains("starz", ignoreCase = true) -> "STARZ"
    name.contains("hidive", ignoreCase = true) -> "HIDIVE"
    else -> name.take(8).uppercase()
}

private fun platformColor(name: String): Color = when {
    name.contains("netflix", ignoreCase = true) -> Color(0xFFE50914)
    name.contains("max", ignoreCase = true) -> Color(0xFF7B61FF)
    name.contains("hbo", ignoreCase = true) -> Color(0xFF7B61FF)
    name.contains("hulu", ignoreCase = true) -> Color(0xFF1CE783)
    name.contains("prime", ignoreCase = true) -> Color(0xFF00A8E1)
    name.contains("amazon", ignoreCase = true) -> Color(0xFF00A8E1)
    name.contains("disney", ignoreCase = true) -> Color(0xFF113CCF)
    name.contains("apple", ignoreCase = true) -> Color(0xFFAAAAAA)
    name.contains("peacock", ignoreCase = true) -> Color(0xFF0073E6)
    name.contains("paramount", ignoreCase = true) -> Color(0xFF0064FF)
    name.contains("crunchyroll", ignoreCase = true) -> Color(0xFFF47521)
    name.contains("funimation", ignoreCase = true) -> Color(0xFF7700BA)
    name.contains("showtime", ignoreCase = true) -> Color(0xFFCC0000)
    name.contains("starz", ignoreCase = true) -> Color(0xFF000000)
    else -> Color(0xFF888888)
}

@Composable
fun ReleaseCard(
    release: Release,
    index: Int,
    onClick: (Release) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 30L)
        visible = true
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardScale",
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
    ) {
        val accentColor = when (release.type) {
            ReleaseType.MOVIE  -> MovieAmber
            ReleaseType.SERIES -> SeriesRed
            ReleaseType.ANIME  -> AnimePurple
        }

        Box(
            modifier = modifier
                .width(CardWidth)
                .height(CardHeight)
                .scale(scale)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onClick(release) },
                ),
        ) {
            // Poster image
            AsyncImage(
                model = release.posterUrl,
                contentDescription = release.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Bottom gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.4f to Color.Transparent,
                            1f to Background,
                        )
                    )
            )

            // Platform badge — top-right
            release.platform?.let { p ->
                val pColor = platformColor(p)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(pColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = platformLabel(p),
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }

            // Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Text(
                    text = release.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )

                if (release.genres.isNotEmpty()) {
                    Text(
                        text = release.genres.take(2).joinToString(" • "),
                        color = TextPrimary.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Rating
                release.rating?.let { r ->
                    if (r > 0) {
                        Text(
                            text = "★ ${"%.1f".format(r)}",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))

                // Badge or countdown
                if (release.status == ReleaseStatus.RELEASED) {
                    WatchBadge(platform = null)
                } else {
                    CountdownText(airDate = release.airDate, airTime = release.airTime)
                }
            }
        }
    }
}

private fun previewRelease(
    id: String = "tmdb_1",
    title: String = "Dune: Part Three",
    type: ReleaseType = ReleaseType.MOVIE,
    status: ReleaseStatus = ReleaseStatus.UPCOMING,
    rating: Float? = 8.4f,
    platform: String? = "Theaters",
) = Release(
    id = id,
    title = title,
    posterUrl = null,
    backdropUrl = null,
    type = type,
    status = status,
    airDate = if (status == ReleaseStatus.UPCOMING) LocalDate.now()
        .plusDays(14) else LocalDate.now().minusDays(2),
    airTime = null,
    platform = platform,
    episodeLabel = null,
    rating = rating,
    synopsis = null,
)

@Preview(showBackground = true, backgroundColor = 0xFF080810)
@Composable
private fun ReleaseCardMovieUpcomingPreview() {
    DropDateTheme {
        ReleaseCard(
            release = previewRelease(
                title = "Dune: Part Three",
                type = ReleaseType.MOVIE,
                status = ReleaseStatus.UPCOMING
            ),
            index = 0,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080810)
@Composable
private fun ReleaseCardSeriesReleasedPreview() {
    DropDateTheme {
        ReleaseCard(
            release = previewRelease(
                id = "tvmaze_1",
                title = "The Last of Us S02E05",
                type = ReleaseType.SERIES,
                status = ReleaseStatus.RELEASED,
                platform = "HBO"
            ),
            index = 0,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080810)
@Composable
private fun ReleaseCardAnimePreview() {
    DropDateTheme {
        ReleaseCard(
            release = previewRelease(
                id = "jikan_1",
                title = "Solo Leveling Season 2",
                type = ReleaseType.ANIME,
                status = ReleaseStatus.UPCOMING,
                rating = 9.1f,
                platform = "Crunchyroll"
            ),
            index = 1,
            onClick = {},
        )
    }
}
