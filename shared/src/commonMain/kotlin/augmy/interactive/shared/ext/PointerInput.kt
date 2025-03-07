package augmy.interactive.shared.ext

import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.absoluteValue

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
    onDrag: (dragged: Boolean) -> Unit = {}
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
 * Similarly to [PointerInputScope.detectTransformGestures], this event listener support pan movements and zooming.
 * However, it is limited behind two events (or whenever [isZoomed] is true), in order to consume the events.
 */
suspend fun PointerInputScope.detectTransformTwoDown(
    isZoomed: () -> Boolean = { false },
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)

        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled && (isZoomed() || event.changes.size > 1)) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(centroid, panChange, zoomChange)
                    }
                    event.changes.fastForEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
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

internal suspend fun PointerInputScope.detectDragGestures(
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

internal expect suspend fun PointerInputScope.detectScrollWheel(
    onWheelScroll: (scrollDirection: Int, scrollAmount: Int) -> Unit
)
