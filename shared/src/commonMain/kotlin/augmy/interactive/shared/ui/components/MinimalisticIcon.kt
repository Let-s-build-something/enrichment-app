package augmy.interactive.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.interactive.shared.ui.theme.LocalTheme
import future_shared_module.ext.scalingClickable

/**
 * Clickable basic Icon with vector image with minimalistic size
 */
@Composable
fun MinimalisticIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    tint: Color = LocalTheme.current.colors.primary,
    contentDescription: String? = null,
    onTap: ((Offset) -> Unit)? = null
) {
    Icon(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .then(if(onTap != null) {
                modifier.scalingClickable(onTap = onTap)
            }else modifier)
            .padding(5.dp),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint
    )
}

/**
 * Clickable basic Icon with vector image with minimalistic size
 */
@Composable
fun MinimalisticFilledIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    tint: Color = LocalTheme.current.colors.brandMainDark,
    background: Color = LocalTheme.current.colors.tetrial,
    contentDescription: String? = null,
    onTap: ((Offset) -> Unit)? = null
) {
    Icon(
        modifier = modifier
            .zIndex(10f)
            .size(34.dp)
            .then(if(onTap != null) {
                Modifier.scalingClickable(onTap = onTap)
            }else Modifier)
            .background(
                color = background,
                shape = CircleShape
            )
            .border(
                width = 0.5.dp,
                color = tint,
                shape = CircleShape
            )
            .clip(CircleShape)
            .padding(5.dp),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint
    )
}

/**
 * Clickable basic Icon with vector image with minimalistic size
 */
@Composable
fun MinimalisticComponentIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    tint: Color = LocalTheme.current.colors.secondary,
    contentDescription: String? = null,
    onTap: ((Offset) -> Unit)? = null
) {
    Icon(
        modifier = modifier
            .size(34.dp)
            .then(if(onTap != null) {
                Modifier.scalingClickable(onTap = onTap)
            }else Modifier)
            .border(
                width = 0.5.dp,
                color = tint,
                shape = CircleShape
            )
            .clip(CircleShape)
            .padding(5.dp),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint
    )
}