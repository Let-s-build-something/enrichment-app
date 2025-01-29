package augmy.interactive.shared.ext

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.PointerInputScope
import java.awt.event.MouseWheelEvent

internal actual suspend fun PointerInputScope.detectScrollWheel(
    onWheelScroll: (scrollDirection: Int, scrollAmount: Int) -> Unit
) {
    awaitEachGesture {
        val event = awaitPointerEvent()
        (event.nativeEvent as? MouseWheelEvent)?.let {
            if(it.scrollAmount != 0) {
                onWheelScroll(it.wheelRotation * -1, it.scrollAmount)
            }
        }
    }
}