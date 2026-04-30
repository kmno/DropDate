package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.DropDateTheme
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.TextSecondary
import java.time.LocalDate

@Composable
fun ReleaseSection(
    title: String,
    accentColor: Color,
    releases: List<Release>,
    onReleaseClick: (Release) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var headerVisible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { headerVisible = true }

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn() + expandVertically(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Colored dot
                Box(
                    modifier =
                        Modifier
                            .size(Dimens.PaddingSmall)
                            .clip(CircleShape)
                            .background(accentColor),
                )

                Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )

                Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

                Spacer(modifier = Modifier.weight(1f))

                // More icon
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "More $title",
                    modifier =
                        Modifier
                            .size(Dimens.IconMedium)
                            .clickable { onMoreClick() },
                    tint = TextSecondary,
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.PaddingMedium),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium + 2.dp),
        ) {
            itemsIndexed(releases, key = { _, r -> r.id }) { index, release ->
                ReleaseCard(
                    release = release,
                    index = index,
                    onClick = onReleaseClick,
                )
            }
        }
    }
}

private fun fakeRelease(
    id: String,
    title: String,
    type: ReleaseType,
    status: ReleaseStatus = ReleaseStatus.UPCOMING,
) = Release(
    id = id,
    seriesId = id,
    title = title,
    posterUrl = null,
    backdropUrl = null,
    type = type,
    status = status,
    airDate =
        if (status == ReleaseStatus.UPCOMING) {
            LocalDate
                .now()
                .plusDays(5)
        } else {
            LocalDate.now().minusDays(1)
        },
    airTime = null,
    platform = null,
    episodeLabel = null,
    rating = 7.8f,
    synopsis = null,
)

@Preview(showBackground = true, backgroundColor = 0xFF080810, widthDp = 360)
@Composable
private fun ReleaseSectionMoviesPreview() {
    DropDateTheme {
        ReleaseSection(
            title = "Movies",
            accentColor = MovieAmber,
            releases =
                listOf(
                    fakeRelease("1", "Dune: Part Three", ReleaseType.MOVIE),
                    fakeRelease(
                        "2",
                        "Mission: Impossible 8",
                        ReleaseType.MOVIE,
                        ReleaseStatus.RELEASED,
                    ),
                    fakeRelease("3", "Avatar 3", ReleaseType.MOVIE),
                ),
            onReleaseClick = {},
            onMoreClick = {},
        )
    }
}
