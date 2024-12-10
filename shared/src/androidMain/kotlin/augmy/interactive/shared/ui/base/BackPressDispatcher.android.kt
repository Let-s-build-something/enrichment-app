package augmy.interactive.shared.ui.base

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BackHandlerOverride(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
}