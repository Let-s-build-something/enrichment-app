package base.global.verification

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.theme.LocalTheme

@Composable
fun ClickableTile(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isHovered = remember { mutableStateOf(false) }
    val color = animateColorAsState(
        targetValue = if(isSelected || isHovered.value) {
            LocalTheme.current.colors.secondary
        }else LocalTheme.current.colors.backgroundLight
    )
    val width = animateDpAsState(
        targetValue = if(isSelected || isHovered.value) 4.dp else 1.dp
    )

    Column(
        modifier = modifier
            .border(
                width = width.value,
                color = color.value,
                shape = LocalTheme.current.shapes.rectangularActionShape
            )
            .padding(vertical = 10.dp, horizontal = 12.dp)
            .scalingClickable(
                key = text,
                scaleInto = .95f,
                onHover = { hovered ->
                    isHovered.value = hovered
                }
            ) {
                onClick()
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            imageVector = icon,
            contentDescription = null,
            tint = LocalTheme.current.colors.secondary
        )
        Text(
            text = text,
            style = LocalTheme.current.styles.subheading
        )
    }
}