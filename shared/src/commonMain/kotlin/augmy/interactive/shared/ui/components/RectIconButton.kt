package augmy.interactive.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ext.scalingClickable

@Composable
fun RectIconButton(
    modifier: Modifier = Modifier,
    shape: Shape = LocalTheme.current.shapes.rectangularActionShape,
    imageVector: ImageVector,
    contentDescription: String?,
    enabled: Boolean = true,
    onTap: (Offset) -> Unit,
    activeColor: Color = LocalTheme.current.colors.brandMain,
    inActiveColor: Color = LocalTheme.current.colors.tetrial
) {
    val backgroundColor = animateColorAsState(
        targetValue = if(enabled) activeColor else inActiveColor
    )
    val iconColor = animateColorAsState(
        targetValue = if(enabled) inActiveColor else activeColor
    )

    Icon(
        modifier = modifier
            .size(32.dp)
            .background(
                color = backgroundColor.value,
                shape = shape
            )
            .scalingClickable(onTap = onTap),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = iconColor.value
    )
}