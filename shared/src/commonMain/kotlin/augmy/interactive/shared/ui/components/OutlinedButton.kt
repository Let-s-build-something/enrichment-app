package augmy.interactive.shared.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import augmy.interactive.shared.ui.theme.LocalTheme
import future_shared_module.ext.scalingClickable

/**
 * Outlined text with transparent background
 */
@Composable
fun OutlinedButton(
    modifier: Modifier = Modifier,
    thenModifier: Modifier = Modifier,
    text: String? = null,
    isActivated: Boolean = true,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
    content: @Composable RowScope.(TextStyle) -> Unit = {},
    activeColor: Color = LocalTheme.current.colors.primary,
    inactiveColor: Color = LocalTheme.current.colors.secondary,
    onClick: () -> Unit
) {
    val color = if (isActivated && enabled) activeColor else inactiveColor
    Row(
        modifier = modifier
            .scalingClickable(
                enabled = enabled,
                onTap = {
                    onClick()
                }
            )
            .border(
                width = if (enabled) 1.dp else 0.dp,
                shape = LocalTheme.current.shapes.componentShape,
                color = color
            )
            .clip(LocalTheme.current.shapes.componentShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(thenModifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val textStyle = TextStyle(
            color = color,
            fontSize = 14.sp
        )
        content(textStyle)
        if(text != null) {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = text,
                style = textStyle
            )
        }
        trailingIcon?.let { icon ->
            Icon(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .requiredHeight(
                        with(LocalDensity.current) {
                            14.sp.toDp()
                        }
                    )
                    .requiredWidth(
                        with(LocalDensity.current) {
                            14.sp.toDp()
                        }
                    ),
                imageVector = icon,
                contentDescription = null,
                tint = color
            )
        }
    }
}