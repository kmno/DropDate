package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.ui.theme.Dimens

@Composable
fun LazyGrid(
    releases: List<Release>,
    onReleaseSelect: (Release) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(2),
        contentPadding =
            PaddingValues(
                start = Dimens.PaddingMedium,
                end = Dimens.PaddingMedium,
                top = Dimens.PaddingSmall,
                bottom = 120.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingNormal),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingNormal),
    ) {
        itemsIndexed(
            releases,
            key = { _, r -> r.id },
        ) { index, release ->
            ReleaseCard(
                release = release,
                index = index,
                onClick = onReleaseSelect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
