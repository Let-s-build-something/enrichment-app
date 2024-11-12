package components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import base.tagToColor

@Composable
fun UserProfileImage(
    modifier: Modifier = Modifier,
    model: Any?,
    tag: String?,
    animate: Boolean = false,
    contentDescription: String? = null
) {
    if(animate) {
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
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .scale(liveScaleBackground)
                    .background(
                        color = tagToColor(tag) ?: LocalTheme.current.colors.tetrial,
                        shape = CircleShape
                    )
            )
            AsyncSvgImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(
                        color = LocalTheme.current.colors.brandMain,
                        shape = CircleShape
                    ),
                model = model,
                contentDescription = null
            )
        }
    }else {
        Box(
            modifier = modifier
                .background(
                    color = tagToColor(tag) ?: LocalTheme.current.colors.tetrial,
                    shape = CircleShape
                )
                .height(IntrinsicSize.Max)
                .width(IntrinsicSize.Max)
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
}