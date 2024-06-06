package com.squadris.squadris.compose.components.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.squadris.squadris.compose.theme.LocalTheme

/** Clickable icon for navigation */
@Composable
fun NavigationIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    tint: Color = LocalTheme.current.colors.tetrial
) {
    Icon(
        modifier = modifier
            .padding(4.dp)
            .size(42.dp)
            .clip(LocalTheme.current.shapes.rectangularActionShape)
            .clickable(
                indication = rememberRipple(
                    bounded = true,
                    color = LocalTheme.current.colors.contrastActionLight
                ),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick()
            }
            .padding(8.dp),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint
    )
}