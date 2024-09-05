package components.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import future_shared_module.theme.LocalTheme

/** Clickable icon for navigation */
@Composable
fun NavigationIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    size: Dp = 42.dp,
    imageVector: ImageVector,
    contentDescription: String,
    tint: Color = LocalTheme.current.colors.tetrial
) {
    Icon(
        modifier = modifier
            .padding(4.dp)
            .size(size)
            .clip(LocalTheme.current.shapes.rectangularActionShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint
    )
}