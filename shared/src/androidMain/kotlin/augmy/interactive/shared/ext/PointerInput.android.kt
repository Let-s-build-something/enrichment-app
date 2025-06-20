package augmy.interactive.shared.ext

import androidx.compose.ui.input.pointer.PointerInputScope

internal actual suspend fun PointerInputScope.detectScrollWheel(
    onWheelScroll: (scrollDirection: Int, scrollAmount: Int) -> Unit
) {
}