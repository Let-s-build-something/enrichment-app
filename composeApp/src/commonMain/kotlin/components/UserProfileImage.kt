package components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ui.components.input.AutoResizeText
import augmy.interactive.shared.ui.components.input.FontSizeRange
import augmy.interactive.shared.ui.theme.LocalTheme
import base.theme.Colors
import base.utils.tagToColor
import data.io.social.network.conversation.message.MediaIO
import data.io.user.UserIO
import ui.conversation.components.MediaElement

@Composable
fun AvatarImage(
    modifier: Modifier = Modifier,
    media: MediaIO?,
    name: String?,
    tag: String?,
    animate: Boolean = false,
    contentDescription: String? = null
) {
    Crossfade(
        modifier = modifier,
        targetState = media != null || name != null
    ) { hasImage ->
        if(hasImage) {
            ContentLayout(
                media = media,
                tag = tag,
                name = name,
                animate = animate && tag != null,
                contentDescription = contentDescription
            )
        }else {
            ShimmerLayout()
        }
    }
}

@Composable
private fun ShimmerLayout(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .brandShimmerEffect(shape = CircleShape)
    )
}

@Composable
private fun ContentLayout(
    modifier: Modifier = Modifier,
    media: MediaIO?,
    tag: String?,
    name: String?,
    animate: Boolean = false,
    contentDescription: String? = null
) {
    if(animate) {
        val density = LocalDensity.current
        val avatarSize = remember(media) {
            mutableStateOf(0f)
        }
        val infiniteTransition = rememberInfiniteTransition(label = "infiniteScaleBackground")
        val liveScaleBackground by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 7000
                    1.15f at 2500 using LinearEasing // Takes 2.5 seconds to reach 1.15f
                    1f at 7000 using LinearEasing // Takes 4.5 seconds to return to 1f
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "liveScaleBackground"
        )

        Box(
            modifier = modifier
                .height(IntrinsicSize.Max)
                .width(IntrinsicSize.Max)
                .animateContentSize()
        ) {
            Box(
                Modifier
                    .padding(
                        avatarSize.value.dp * .15f / 2f
                    )
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .scale(liveScaleBackground)
                    .background(
                        color = tagToColor(tag) ?: LocalTheme.current.colors.tetrial,
                        shape = CircleShape
                    )
            )
            ContentElement(
                modifier = modifier
                    .padding(
                        avatarSize.value.dp * .15f / 2f
                    )
                    .fillMaxWidth()
                    .onSizeChanged {
                        if(avatarSize.value == 0f) {
                            avatarSize.value = with(density) {
                                it.width.toDp().value
                            }
                        }
                    },
                contentDescription = contentDescription,
                media = media,
                name = name,
                tag = tag
            )
        }
    }else if(tag != null) {
        Box(
            modifier = modifier
                .background(
                    color = tagToColor(tag) ?: LocalTheme.current.colors.tetrial,
                    shape = CircleShape
                )
                .height(IntrinsicSize.Max)
                .width(IntrinsicSize.Max)
        ) {
            ContentElement(
                modifier = modifier.scale(0.95f),
                contentDescription = contentDescription,
                media = media,
                name = name,
                tag = tag
            )
        }
    }else {
        ContentElement(
            modifier = modifier,
            contentDescription = contentDescription,
            media = media,
            name = name,
            tag = tag
        )
    }
}

@Composable
private fun ContentElement(
    modifier: Modifier = Modifier,
    contentDescription: String?,
    media: MediaIO?,
    tag: String?,
    name: String?
) {
    Crossfade(media?.isEmpty == false) { isValid ->
        if(isValid) {
            MediaElement(
                modifier = modifier
                    .clip(CircleShape)
                    .background(
                        color = LocalTheme.current.colors.backgroundDark,
                        shape = CircleShape
                    )
                    .aspectRatio(1f),
                contentDescription = contentDescription,
                media = media,
                contentScale = ContentScale.Crop
            )
        }else if(name != null || tag != null) {
            val backgroundColor = tagToColor(tag) ?: LocalTheme.current.colors.tetrial
            val textColor = if(backgroundColor.luminance() > .5f) Colors.Coffee else Colors.GrayLight

            Box(
                modifier = modifier
                    .aspectRatio(1f)
                    .background(
                        color = backgroundColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AutoResizeText(
                    modifier = Modifier.padding(vertical = 6.dp),
                    text = UserIO.initialsOf(name),
                    style = LocalTheme.current.styles.subheading.copy(color = textColor),
                    fontSizeRange = FontSizeRange(
                        min = 6.sp,
                        max = LocalTheme.current.styles.subheading.fontSize * 1.5f
                    )
                )
            }
        }
    }
}
