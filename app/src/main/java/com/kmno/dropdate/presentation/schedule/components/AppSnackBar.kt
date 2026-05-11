package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kmno.dropdate.ui.theme.All
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesRed
import com.kmno.dropdate.ui.theme.Surface
import com.kmno.dropdate.ui.theme.SurfaceAlt
import com.kmno.dropdate.ui.theme.TextPrimary

@Composable
fun AppSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(Dimens.PaddingMedium)
                .shadow(
                    12.dp,
                    RoundedCornerShape(Dimens.FloatingBoxCornerRadius),
                ).background(
                    SurfaceAlt,
                    RoundedCornerShape(Dimens.FloatingBoxCornerRadius),
                ).border(
                    width = 1.dp,
                    color = Surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(Dimens.FloatingBoxCornerRadius),
                ).padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingSmall + 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
        ) {
            AppIconMini()
            Text(
                text = snackbarData.visuals.message,
                color = TextPrimary,
                fontSize = Dimens.FontNormal,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AppIconMini() {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.size(6.dp).background(All, CircleShape))
            Box(Modifier.size(6.dp).background(MovieAmber, CircleShape))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.size(6.dp).background(SeriesRed, CircleShape))
            Box(Modifier.size(6.dp).background(AnimePurple, CircleShape))
        }
    }
}
