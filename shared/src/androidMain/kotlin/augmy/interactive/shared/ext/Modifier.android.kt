package augmy.interactive.shared.ext

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.contentReceiver(onUriSelected: (uri: String) -> Unit): Modifier {
    return this.then(
        Modifier
            .contentReceiver(
                receiveContentListener = { content ->
                    content.consume {
                        content.platformTransferableContent?.linkUri?.let {
                            onUriSelected(it.toString())
                            true
                        } ?: false
                    }
                }
            )
    )
}
