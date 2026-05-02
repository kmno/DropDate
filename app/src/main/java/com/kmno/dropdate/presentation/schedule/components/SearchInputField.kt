package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.kmno.dropdate.R
import com.kmno.dropdate.ui.theme.All
import com.kmno.dropdate.ui.theme.Dimens
import com.kmno.dropdate.ui.theme.Surface
import com.kmno.dropdate.ui.theme.SurfaceAlt
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary

@Composable
fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.FloatingBoxCornerRadius)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(durationMillis = 350))
                    .shadow(elevation = 12.dp, shape = shape)
                    .background(SurfaceAlt, shape)
                    .border(
                        width = 1.dp,
                        color = Surface.copy(alpha = 0.5f),
                        shape = shape,
                    ).height(56.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.PaddingMedium),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(Dimens.IconLarge),
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle =
                        TextStyle(
                            color = TextPrimary,
                            fontSize = Dimens.FontMedium,
                        ),
                    cursorBrush = SolidColor(All),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = Dimens.SpacingMedium),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_releases),
                                    color = TextSecondary,
                                    fontSize = Dimens.FontMedium,
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_search),
                        tint = TextSecondary,
                        modifier = Modifier.size(Dimens.IconMedium),
                    )
                }
            }
        }
    }
}
