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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kmno.dropdate.R
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.DropDateTheme
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesRed
import com.kmno.dropdate.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import java.time.LocalDate
import com.kmno.dropdate.ui.theme.Surface as ThemeSurface

private val CardWidth = 140.dp
private val CardHeight = 200.dp

object PlatformBranding {
    @Suppress("MagicNumber")
    fun getColor(name: String): Color =
        when {
            name.contains("netflix", true) -> Color(0xFFE50914)
            name.contains("disney", true) -> Color(0xFF113CCF)
            name.contains("max", true) -> Color(0xFF0047FF)
            name.contains("prime", true) || name.contains("amazon", true) -> Color(0xFF00A8E1)
            name.contains("hulu", true) -> Color(0xFF1CE783)
            name.contains("apple", true) -> Color(0xFF555555)
            name.contains("peacock", true) -> Color(0xFF0F6CBD)
            name.contains("paramount", true) -> Color(0xFF0064FF)
            name.contains("crunchyroll", true) -> Color(0xFFF47521)
            name.contains("funimation", true) -> Color(0xFF7700BA)
            name.contains("amc", true) -> Color(0xFF3A3A3A)
            name.contains("mgm", true) -> Color(0xFF8B7536)
            name.contains("showtime", true) -> Color(0xFFCC0000)
            name.contains("showmax", true) -> Color(0xFF00D4FF)
            name.contains("fx", true) -> Color(0xFF000000)
            else -> Color(0xFF6B6B85)
        }

    fun getDisplayName(name: String): String =
        when {
            name.contains("netflix", true) -> "Netflix"
            name.contains("disney", true) -> "Disney+"
            name.contains("max", true) -> "HBO Max"
            name.contains("prime", true) -> "Prime"
            name.contains("amazon", true) -> "Prime"
            name.contains("apple", true) -> "Apple TV+"
            name.contains("paramount", true) -> "Paramount+"
            name.contains("crunchyroll", true) -> "Crunchyroll"
            name.contains("peacock", true) -> "peacock"
            name.contains("hulu", true) -> "hulu"
            name.contains("amc", true) -> "AMC+"
            name.contains("mgm", true) -> "MGM+"
            name.contains("showtime", true) -> "Showtime"
            name.contains("showmax", true) -> "ShowMax"
            name.contains("fx", true) -> "FX"
            else -> name.take(8).uppercase()
        }

    fun getLogoUrl(name: String): String? =
        when {
            name.contains(
                "netflix",
                true,
            ) -> "https://www.logo.wine/a/logo/Netflix/Netflix-Logo.wine.svg"

            name.contains(
                "disney",
                true,
            ) -> "https://www.logo.wine/a/logo/Disney%2B/Disney%2B-Logo.wine.svg"

            name.contains("hbo", true) ||
                name.contains(
                    "hbo",
                    true,
                ) -> "https://www.logo.wine/a/logo/HBO_Max/HBO_Max-Logo.wine.svg"

            name.contains("prime", true) ||
                name.contains(
                    "amazon",
                    true,
                ) -> "https://www.logo.wine/a/logo/Amazon_Prime/Amazon_Prime-Logo.wine.svg"

            name.contains("hulu", true) -> "https://www.logo.wine/a/logo/Hulu/Hulu-Logo.wine.svg"
            name.contains(
                "apple",
                true,
            ) -> "https://upload.wikimedia.org/wikipedia/commons/2/28/Apple_TV_Plus_Logo.svg"

            name.contains(
                "crunchy",
                true,
            ) -> "https://www.logo.wine/a/logo/Crunchyroll/Crunchyroll-Logo.wine.svg"

            else -> null
        }
}

@Composable
fun PlatformLogo(
    platform: String,
    modifier: Modifier = Modifier,
    useActualLogo: Boolean = false,
) {
    val logoUrl = if (useActualLogo) PlatformBranding.getLogoUrl(platform) else null
    if (logoUrl != null) {
        AsyncImage(
            model = logoUrl,
            contentDescription = platform,
            modifier = modifier.height(16.dp),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
        )
    } else {
        Surface(
            color = PlatformBranding.getColor(platform),
            shape = RoundedCornerShape(Dimens.SpacingSmall),
            modifier = modifier,
        ) {
            Text(
                text = PlatformBranding.getDisplayName(platform),
                color = Color.White,
                fontSize = Dimens.FontBadgeSize,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = Dimens.SpacingSmall, vertical = 1.dp),
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
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
        val accentColor =
            when (release.type) {
                ReleaseType.MOVIE -> MovieAmber
                ReleaseType.SERIES -> SeriesRed
                ReleaseType.ANIME -> AnimePurple
            }

        Box(
            modifier =
                modifier
                    .width(CardWidth)
                    .height(CardHeight)
                    .scale(scale)
                    .clip(RoundedCornerShape(Dimens.SpacingNormal))
                    .background(ThemeSurface)
                    .border(
                        0.5.dp,
                        accentColor.copy(alpha = 0.3f),
                        RoundedCornerShape(Dimens.SpacingNormal),
                    ).clickable(
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
                placeholder = painterResource(R.drawable.ic_placeholder), // Added
                error = painterResource(R.drawable.ic_placeholder),
                modifier = Modifier.fillMaxSize(),
            )

            // Bottom gradient scrim
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.4f to Color.Transparent,
                                1f to Background,
                            ),
                        ),
            )

            // Platform badge — top-right
            release.platform?.let { p ->
                PlatformLogo(
                    platform = p,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(Dimens.SpacingMedium),
                )
            }

            // Content overlay
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(Dimens.SpacingMedium),
            ) {
                Text(
                    text = release.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )

                // Rating
                release.rating?.let { r ->
                    Spacer(Modifier.height(1.dp))
                    if (r > 0) {
                        Text(
                            text = "★ ${"%.1f".format(r)}",
                            color = accentColor,
                            fontSize = Dimens.FontExtraSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.height(Dimens.SpacingSmall))

                // Badge or countdown
                if (release.status == ReleaseStatus.RELEASED) {
                    WatchBadge(platform = null)
                } else {
                    CountdownText(
                        airDate = release.airDate,
                        airTime = release.airTime,
                        showDetails = false,
                    )
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
    airDate =
        if (status == ReleaseStatus.UPCOMING) {
            LocalDate
                .now()
                .plusDays(14)
        } else {
            LocalDate.now().minusDays(2)
        },
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
            release =
                previewRelease(
                    title = "Dune: Part Three",
                    type = ReleaseType.MOVIE,
                    status = ReleaseStatus.UPCOMING,
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
            release =
                previewRelease(
                    id = "tvmaze_1",
                    title = "The Last of Us S02E05",
                    type = ReleaseType.SERIES,
                    status = ReleaseStatus.RELEASED,
                    platform = "HBO",
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
            release =
                previewRelease(
                    id = "jikan_1",
                    title = "Solo Leveling Season 2",
                    type = ReleaseType.ANIME,
                    status = ReleaseStatus.UPCOMING,
                    rating = 9.1f,
                    platform = "Crunchyroll",
                ),
            index = 1,
            onClick = {},
        )
    }
}
