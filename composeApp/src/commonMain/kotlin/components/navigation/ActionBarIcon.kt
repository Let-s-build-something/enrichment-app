package components.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import components.AutoResizeText
import components.FontSizeRange
import future_shared_module.theme.LocalTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Basic icon with text, which is mainly designed for action bar, but could be used practically anywhere.
 *
 * The layout is constrained to both width and height.
 *
 * @param text text to be displayed under the icon
 * @param imageVector icon to be displayed
 * @param imageUrl url to remote image to be displayed, it is loaded with Coil and Ktor
 * @param onClick event upon clicking on the layout
 */
@Composable
fun ActionBarIcon(
    modifier: Modifier = Modifier,
    text: String? = null,
    imageVector: ImageVector? = null,
    imageUrl: String? = null,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(4.dp)
            .heightIn(max = 64.0.dp, min = 42.dp)
            .then(modifier)
            .widthIn(min = 42.dp, max = 100.dp)
            .clip(LocalTheme.current.shapes.rectangularActionShape)
            .clickable {
                onClick.invoke()
            }
            .padding(
                vertical = if(text == null) 8.dp else 4.dp,
                horizontal = if(text == null) 8.dp else 6.dp
            )
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            imageUrl.isNullOrBlank().not() -> {
                AsyncImage(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(24.dp),
                    model = imageUrl,
                    contentDescription = text
                )
            }
            imageVector != null -> {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = imageVector,
                    contentDescription = text,
                    tint = LocalTheme.current.colors.tetrial
                )
            }
        }
        AnimatedVisibility(visible = text != null) {
            AutoResizeText(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(top = 2.dp),
                text = text ?: "",
                color = LocalTheme.current.colors.tetrial,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                // some users tune up font size so high we can draw it otherwise
                fontSizeRange = FontSizeRange(
                    min = 9.5.sp,
                    max = 14.sp
                )
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ActionBarIcon(
        text = "test action",
        imageVector = Icons.Outlined.Dashboard
    )
}