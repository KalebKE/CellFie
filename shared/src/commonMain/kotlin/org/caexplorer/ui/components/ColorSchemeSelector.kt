package org.caexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.caexplorer.domain.colorscheme.*
import org.caexplorer.domain.colorscheme.ColorScheme as CAColorScheme

/**
 * Color scheme selector showing a horizontal strip of color scheme previews.
 */
@Composable
fun ColorSchemeSelector(
    selectedScheme: CAColorScheme,
    onSchemeSelected: (CAColorScheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val schemes = remember {
        listOf(
            RainbowColorScheme(),
            FireColorScheme(),
            BlackAndWhiteColorScheme(),
            WhiteAndBlackColorScheme(),
            BlueDiamondColorScheme(),
            GreenOceanColorScheme(),
            ChocolateColorScheme(),
            YellowJacketColorScheme(),
            KindOfBluesColorScheme(),
            PurpleHazeColorScheme(),
            WaterLiliesColorScheme(),
            RandomColorScheme()
        )
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(schemes) { scheme ->
            ColorSchemeChip(
                scheme = scheme,
                isSelected = scheme::class == selectedScheme::class,
                onClick = { onSchemeSelected(scheme) }
            )
        }
    }
}

@Composable
private fun ColorSchemeChip(
    scheme: CAColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val previewColors = remember(scheme) {
        (0 until 6).map { i -> scheme.getColor(i, 6) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Color swatch (gradient preview)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(previewColors)
                )
                .then(
                    if (isSelected) Modifier.border(
                        3.dp,
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
                    else Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
                )
        )

        Spacer(Modifier.height(4.dp))

        Text(
            scheme.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
