package augmy.interactive.shared.ext

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.base.LocalIsMouseUser
import augmy.interactive.shared.ui.theme.LocalTheme
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

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
    enabled: Boolean = true,
    hoverEnabled: Boolean = true,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onPress: ((Offset, isPressed: Boolean) -> Unit)? = null,
    onHover: ((isHovered: Boolean) -> Unit)? = null,
    scaleInto: Float = 0.85f,
    onTap: ((Offset) -> Unit)? = null
): Modifier = composed {
    if(enabled) {
        val hoverInteractionSource = remember { MutableInteractionSource() }
        val isHovered = hoverInteractionSource.collectIsHoveredAsState()
        val isPressed = remember { mutableStateOf(false) }
        val scale = animateFloatAsState(
            if ((isPressed.value || isHovered.value) && enabled) scaleInto else 1f,
            label = "scalingClickableAnimation"
        )
        onHover?.invoke(isHovered.value)

        scale(scale.value)
            .hoverable(
                enabled = enabled && hoverEnabled,
                interactionSource = hoverInteractionSource
            )
            .pointerInput(Unit) {
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
    }else this
}

/** Adds horizontal draggable modifier to a [HorizontalPager] to support mouse interactions */
@Composable
fun Modifier.mouseDraggable(
    state: PagerState,
    onChange: (Int) -> Unit
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    return this.draggable(
        orientation = Orientation.Horizontal,
        state = rememberDraggableState { delta ->
            coroutineScope.coroutineContext.cancelChildren()
            coroutineScope.launch {
                state.scrollBy(-delta)
            }
        },
        onDragStopped = {
            coroutineScope.launch {
                val currentPage = state.currentPage
                state.getOffsetDistanceInPages(currentPage).let { pageOffset ->
                    val newPage = if(pageOffset > 1.5f) currentPage + 1
                    else if(pageOffset < -1.5f) currentPage - 1
                    else currentPage

                    state.animateScrollToPage(newPage)
                    onChange(newPage)
                }
            }
        }
    )
}

/**
 * Draggable modifier, which enables the draggable to be scaled based on the state of the drag
 */
@Composable
fun Modifier.scalingDraggable(
    enabled: Boolean = true,
    onDragChange: ((change: PointerInputChange, dragAmount: Offset) -> Unit)? = null,
    onDrag: ((dragged: Boolean) -> Unit)? = null,
    scaleInto: Float = 0.85f
): Modifier = composed {
    val isPressed = remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        if (isPressed.value && enabled) scaleInto else 1f,
        label = "scalingDraggableAnimation"
    )

    scale(scale.value)
        .pointerInput(enabled) {
            detectDragGestures(
                onDragStart = {
                    onDrag?.invoke(true)
                    isPressed.value = true
                },
                onDragCancel = {
                    onDrag?.invoke(false)
                    isPressed.value = false
                },
                onDrag = { change, offset ->
                    onDragChange?.invoke(change, offset)
                }
            )
        }
}

/** Detects mouse scroll wheel events */
fun Modifier.onMouseScroll(
    enabled: Boolean = true,
    onWheelScroll: (scrollDirection: Int, scrollAmount: Int) -> Unit
): Modifier {
    return this.pointerInput(enabled) {
        detectScrollWheel(onWheelScroll = onWheelScroll)
    }
}

/** Makes a horizontally scrollable layout draggable for desktop */
fun Modifier.draggable(
    state: ScrollState,
    orientation: Orientation = Orientation.Vertical
) = composed {
    if(LocalIsMouseUser.current) {
        val coroutineScope = rememberCoroutineScope()

        draggable(
            orientation = orientation,
            state = rememberDraggableState { delta ->
                coroutineScope.launch {
                    state.scrollBy(delta.times(if(orientation == Orientation.Horizontal) -1 else 1))
                }
            }
        )
    }else Modifier
}

/** Makes a vertically scrollable layout draggable for desktop */
fun Modifier.draggable(
    listState: LazyListState,
    orientation: Orientation = Orientation.Vertical
) = composed {
    if(LocalIsMouseUser.current) {
        val coroutineScope = rememberCoroutineScope()

        draggable(
            orientation = orientation,
            state = rememberDraggableState { delta ->
                coroutineScope.launch {
                    listState.scrollBy(delta.times(if(orientation == Orientation.Horizontal) -1 else 1))
                }
            },
        )
    }else Modifier
}

expect fun Modifier.contentReceiver(onUriSelected: (uri: String) -> Unit): Modifier
