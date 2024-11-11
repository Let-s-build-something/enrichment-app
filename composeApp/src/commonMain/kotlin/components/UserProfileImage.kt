package components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import base.tagToColor

@Composable
fun UserProfileImage(
    modifier: Modifier = Modifier,
    model: Any?,
    tag: String?,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .then(
                tagToColor(tag)?.let { tagColor ->
                    Modifier.background(color = tagColor, shape = CircleShape)
                } ?: Modifier
            )
            .height(IntrinsicSize.Max)
    ) {
        AsyncSvgImage(
            modifier = Modifier
                .padding(2.dp)
                .background(
                    color = LocalTheme.current.colors.brandMain,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .aspectRatio(1f),
            contentDescription = contentDescription,
            model = model
        )
    }
}