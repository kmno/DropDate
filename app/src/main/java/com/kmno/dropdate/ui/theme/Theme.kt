package com.kmno.dropdate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DropDateColorScheme = darkColorScheme(
    primary = SeriesRed,
    secondary       = MovieAmber,
    tertiary        = AnimePurple,
    background      = Background,
    surface         = Surface,
    onPrimary       = TextPrimary,
    onSecondary     = TextPrimary,
    onBackground    = TextPrimary,
    onSurface       = TextPrimary,
    surfaceVariant  = SurfaceAlt,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun DropDateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DropDateColorScheme,
        typography  = Typography,
        content     = content,
    )
}
