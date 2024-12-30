package augmy.interactive.shared.ext

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import augmy.interactive.shared.ui.theme.LocalTheme
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.absoluteValue

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
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onPress: ((Offset, isPressed: Boolean) -> Unit)? = null,
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

        scale(scale.value)
            .hoverable(
                enabled = enabled,
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

private suspend fun PointerInputScope.detectDragGestures(
    onDragStart: () -> Unit,
    onDragCancel: () -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial
        )

        onDragStart.invoke()
        onDrag(initialDown, initialDown.position)
        drag(
            pointerId = initialDown.id,
            onDrag = {
                onDrag(it, it.positionChange())
                it.consume()
            }
        )
        onDragCancel()
    }
}

/**
 * Custom touch event listener which support onTap, dragging, and long press
 * Long press is the initial checked event, which is cancelled either by timeout, or dragging
 * On tap event is secondary, as it has no timeout, yet, it can be cancelled by dragging
 * Dragging is the default fallback callback
 */
suspend fun PointerInputScope.detectMessageInteraction(
    onLongPress: ((Offset) -> Unit)? = null,
    onTap: ((Offset) -> Unit)? = null,
    onDragChange: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDrag: (dragged: Boolean) -> Unit,
) = coroutineScope {
    // special signal to indicate to the sending side that it shouldn't intercept and consume
    // cancel/up events as we're only require down events
    val pressScope = PressGestureScopeImpl(this@detectMessageInteraction)

    awaitEachGesture {
        val initialDown = awaitFirstDown()
        initialDown.consume()
        launch {
            pressScope.reset()
        }
        val longPressTimeout = onLongPress?.let {
            viewConfiguration.longPressTimeoutMillis
        } ?: (Long.MAX_VALUE / 2)
        val upOrCancel: PointerInputChange?
        try {
            // wait for first tap up or long press
            upOrCancel = withTimeout(longPressTimeout) {
                waitForUpSwipeOrCancellation(
                    onInvalidInput = {
                        onDrag(false)
                        // stop the drag immediately if the drag is vertical
                        throw(CancellationException())
                    },
                    onDragChange = onDragChange
                )
            }
            if (upOrCancel == null) {
                launch {
                    pressScope.cancel()
                }
            } else {
                upOrCancel.consume()
                launch {
                    pressScope.release()
                }
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            onLongPress?.invoke(initialDown.position)
            consumeUntilUp()
            launch {
                pressScope.release()
            }
            onDrag(false)
            return@awaitEachGesture
        }

        if (upOrCancel != null) {
            // tap was successful.
            onTap?.invoke(upOrCancel.position)
            onDrag(false)
            return@awaitEachGesture
        }

        // if both tap and long press failed, the user is dragging
        onDrag(true)
        drag(
            pointerId = initialDown.id,
            onDrag = {
                onDragChange(it, it.positionChange())
                it.consume()
            }
        )
        onDrag(false)
    }
}

suspend fun AwaitPointerEventScope.waitForUpSwipeOrCancellation(
    pass: PointerEventPass = PointerEventPass.Main,
    onInvalidInput: () -> Unit,
    onDragChange: (change: PointerInputChange, dragAmount: Offset) -> Unit,
): PointerInputChange? {
    val xBoundsPx = 20f
    val yBoundsPx = 2f
    var sumX = 0f
    var sumY = 0f

    while (true) {
        val event = awaitPointerEvent(pass)
        if (event.changes.fastAll { it.changedToUp() }) {
            // All pointers are up
            return event.changes[0]
        }

        sumX += event.changes[0].positionChange().x
        onDragChange(
            event.changes[0],
            Offset(x = event.changes[0].positionChange().x, y = 0f)
        )

        if (sumX.absoluteValue > xBoundsPx || event.changes.fastAny {
                it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding)
            }
        ) {
            return null // Cancelled
        }

        sumY += event.changes[0].positionChange().y
        if (sumY.absoluteValue > yBoundsPx) {
            onInvalidInput()
            return null // Cancelled
        }

        // Check for cancel by position consumption. We can look on the Final pass of the
        // existing pointer event because it comes after the pass we checked above.
        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.fastAny { it.isConsumed }) {
            return null
        }
    }
}

/**
 * Consumes all pointer events until nothing is pressed and then returns. This method assumes
 * that something is currently pressed.
 */
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.fastForEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}

internal class PressGestureScopeImpl(
    density: Density
) : PressGestureScope, Density by density {
    private var isReleased = false
    private var isCanceled = false
    private val mutex = Mutex(locked = false)

    /**
     * Called when a gesture has been canceled.
     */
    fun cancel() {
        isCanceled = true
        mutex.unlock()
    }

    /**
     * Called when all pointers are up.
     */
    fun release() {
        isReleased = true
        mutex.unlock()
    }

    /**
     * Called when a new gesture has started.
     */
    suspend fun reset() {
        mutex.lock()
        isReleased = false
        isCanceled = false
    }

    override suspend fun awaitRelease() {
        if (!tryAwaitRelease()) {
            throw GestureCancellationException("The press gesture was canceled.")
        }
    }

    override suspend fun tryAwaitRelease(): Boolean {
        if (!isReleased && !isCanceled) {
            mutex.lock()
            mutex.unlock()
        }
        return isReleased
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