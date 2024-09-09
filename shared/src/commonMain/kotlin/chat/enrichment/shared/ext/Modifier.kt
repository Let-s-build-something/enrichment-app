package future_shared_module.ext

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import chat.enrichment.shared.ui.theme.LocalTheme

/** Pseudo shimmer effect, animating a brush around an element */
@Composable
fun Modifier.shimmerEffect(
    stripeColor: Color,
    startEndColor: Color,
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val startOffsetX by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = (-1.5 * size.width).toFloat(),
        targetValue = (1.5 * size.width).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(1000)),
        label = ""
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                startEndColor,
                stripeColor,
                startEndColor
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        ),
        shape = shape
    ).onGloballyPositioned {
        size = it.size
    }
}

/** shimmer loading effect via background color based on Brand colors */
@Composable
fun Modifier.brandShimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier = composed {
    shimmerEffect(
        stripeColor = LocalTheme.current.colors.overShimmer,
        startEndColor = LocalTheme.current.colors.shimmer,
        shape = shape
    )
}

/**
 * Clickable modifier, which enables clickable to be scaled based on the presses
 */
@Composable
fun Modifier.scalingClickable(
    onTap: ((Offset) -> Unit)? = null,
    enabled: Boolean = true,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onPress: ((Offset, isPressed: Boolean) -> Unit)? = null,
    scaleInto: Float = 0.85f
): Modifier = composed {
    val isPressed = remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        if (isPressed.value && enabled) scaleInto else 1f,
        label = "scalingClickableAnimation"
    )

    scale(scale.value)
        .pointerInput(enabled) {
            detectTapGestures(
                onPress = {
                    if(enabled) onPress?.invoke(it, true)
                    isPressed.value = true
                    tryAwaitRelease()
                    if(enabled) onPress?.invoke(it, false)
                    isPressed.value = false
                },
                onTap = onTap,
                onDoubleTap = onDoubleTap,
                onLongPress = onLongPress
            )
        }
}

/**
 * [Modifier] that takes up all the available width inside the [TabRow], and then animates
 * the offset of the indicator it is applied to, depending on the [currentTabPosition].
 *
 * @param currentTabPosition [TabPosition] of the currently selected tab. This is used to
 * calculate the offset of the indicator this modifier is applied to, as well as its width.
 */
fun Modifier.customTabIndicatorOffset(
    currentTabPosition: TabPosition,
    horizontalPadding: Dp = 4.dp
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "tabIndicatorOffset"
        value = currentTabPosition
    }
) {
    val currentTabWidth by animateDpAsState(
        targetValue = currentTabPosition.width,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "tabWidthAnimation"
    )
    val indicatorOffset by animateDpAsState(
        targetValue = currentTabPosition.left,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "tabOffsetAnimation"
    )
    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset.plus(horizontalPadding))
        .width(currentTabWidth.minus(horizontalPadding.times(2)))
}