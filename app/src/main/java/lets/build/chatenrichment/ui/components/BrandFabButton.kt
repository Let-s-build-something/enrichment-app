package lets.build.chatenrichment.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.squadris.squadris.compose.theme.LocalTheme
import com.squadris.squadris.ext.scalingClickable

/** Brand floating action button */
@Composable
fun BrandFabButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    isEnabled: Boolean = true,
    containerColor: Color = LocalTheme.current.colors.tetrial,
    contentColor: Color = LocalTheme.current.colors.brandMain,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .scalingClickable(
                onTap = {
                    if (isEnabled) onClick()
                }
            )
            .background(
                color = containerColor,
                shape = LocalTheme.current.shapes.circularActionShape
            )
            .size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .size(32.dp),
            imageVector = imageVector,
            tint = contentColor,
            contentDescription = "confirm"
        )
    }
}

@Preview
@Composable
private fun PreviewBrandFabButton() {
    BrandFabButton(imageVector = Icons.Outlined.Check)
}