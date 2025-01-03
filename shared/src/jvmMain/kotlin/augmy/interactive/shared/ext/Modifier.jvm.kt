package augmy.interactive.shared.ext

import androidx.compose.ui.Modifier

actual fun Modifier.contentReceiver(onUriSelected: (uri: String) -> Unit): Modifier {
    return this
}