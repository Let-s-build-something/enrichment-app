package lets.build.chatenrichment.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squadris.squadris.compose.theme.LocalTheme
import com.squadris.squadris.ext.scalingClickable

/**
 * Outlined text with transparent background
 */
@Composable
fun OutlinedButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    shape: Shape = LocalTheme.current.shapes.circularActionShape,
    activeColor: Color = LocalTheme.current.colors.primary,
    inactiveColor: Color = LocalTheme.current.colors.secondary,
    onClick: () -> Unit
) {
    val isPressed = remember(text) { mutableStateOf(false) }
    val color = animateColorAsState(
        when {
            isPressed.value -> activeColor
            enabled.not() -> LocalTheme.current.colors.disabled
            else -> inactiveColor
        },
        label = "colorClickableAnimation"
    )

    Row(
        modifier = modifier
            .scalingClickable(
                onTap = {
                    if(enabled) onClick()
                },
                onPress = { _, pressed ->
                    isPressed.value = pressed
                }
            )
            .clip(shape)
            .border(
                width = if (enabled) 1.dp else 0.dp,
                shape = shape,
                color = color.value
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        icon?.let { icon ->
            Icon(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp),
                imageVector = icon,
                contentDescription = null,
                tint = color.value
            )
        }
        if(text != null) {
            Text(
                modifier = Modifier
                    .padding(ButtonDefaults.ContentPadding)
                    .weight(1f, fill = false),
                text = text,
                style = TextStyle(
                    color = color.value,
                    fontSize = 16.sp
                )
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
                tint = color.value
            )
        }
    }
}

@Composable
@Preview
private fun Preview() {
    Row(
        modifier = Modifier
            .background(LocalTheme.current.colors.backgroundLight)
            .padding(16.dp)
    ) {
        val activated = true
        OutlinedButton(
            text = if(activated) "Activated" else "Inactivated",
            trailingIcon = if(activated) Icons.Outlined.Check else Icons.Outlined.Close,
            onClick = {},
            activeColor = LocalTheme.current.colors.disabled,
            inactiveColor = LocalTheme.current.colors.secondary,
        )
    }
}