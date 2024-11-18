package augmy.interactive.shared.ui.components.navigation

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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import augmy.interactive.shared.ui.components.input.AutoResizeText
import augmy.interactive.shared.ui.components.input.FontSizeRange
import augmy.interactive.shared.ui.theme.LocalTheme
import coil3.compose.AsyncImage

/**
 * Basic icon with text, which is mainly designed for action bar, but could be used practically anywhere.
 *
 * The layout is constrained to both width and height.
 *
 * @param text text to be displayed under the icon
 * @param imageVector icon to be displayed. If [imageUrl] isn't null, it will be used as a placeholder.
 * @param imageUrl url to remote image to be displayed, it is loaded with Coil and Ktor
 * @param onClick event upon clicking on the layout
 */
@Composable
fun ActionBarIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalTheme.current.colors.tetrial,
    enabled: Boolean = true,
    text: String? = null,
    imageVector: ImageVector? = null,
    imageUrl: String? = null,
    onClick: () -> Unit = {}
) {
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .padding(4.dp)
            .heightIn(max = 64.0.dp, min = 42.dp)
            .then(modifier)
            .widthIn(min = 42.dp, max = 100.dp)
            .clip(LocalTheme.current.shapes.rectangularActionShape)
            .clickable(enabled = enabled, onClick = onClick)
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
                val placeholder = if(imageVector != null) rememberVectorPainter(imageVector) else null

                AsyncImage(
                    modifier = Modifier
                        .clip(CircleShape)
                        .then(if(text == null) {
                            Modifier.size(22.dp + with(density) { 12.sp.toDp() })
                        }else Modifier.size(24.dp)),
                    model = imageUrl,
                    fallback = placeholder,
                    placeholder = placeholder,
                    contentDescription = text,
                    contentScale = ContentScale.Crop
                )
            }
            imageVector != null -> {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = imageVector,
                    contentDescription = text,
                    tint = tint
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