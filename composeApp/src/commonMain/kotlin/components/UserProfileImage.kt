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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ui.theme.LocalTheme
import base.utils.tagToColor
import data.io.social.network.conversation.message.MediaIO
import ui.conversation.components.MediaElement

@Composable
fun UserProfileImage(
    modifier: Modifier = Modifier,
    media: MediaIO?,
    tag: String?,
    animate: Boolean = false,
    contentDescription: String? = null
) {
    Crossfade(
        modifier = modifier,
        targetState = media != null
    ) { hasImage ->
        if(hasImage) {
            ContentLayout(
                media = media,
                tag = tag,
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
            MediaElement(
                modifier = Modifier
                    .padding(
                        avatarSize.value.dp * .15f / 2f
                    )
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(
                        color = LocalTheme.current.colors.brandMain,
                        shape = CircleShape
                    )
                    .onSizeChanged {
                        if(avatarSize.value == 0f) {
                            avatarSize.value = with(density) {
                                it.width.toDp().value
                            }
                        }
                    },
                media = media,
                contentScale = ContentScale.Crop,
                contentDescription = null
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
            MediaElement(
                modifier = Modifier
                    .scale(0.95f)
                    .background(
                        color = LocalTheme.current.colors.brandMain,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .aspectRatio(1f),
                contentDescription = contentDescription,
                media = media,
                contentScale = ContentScale.Crop
            )
        }
    }else {
        MediaElement(
            modifier = modifier
                .background(
                    color = LocalTheme.current.colors.brandMain,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .aspectRatio(1f),
            contentDescription = contentDescription,
            media = media,
            contentScale = ContentScale.Crop
        )
    }
}
