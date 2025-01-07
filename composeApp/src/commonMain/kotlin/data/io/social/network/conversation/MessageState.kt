package data.io.social.network.conversation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_message_status_failed
import augmy.composeapp.generated.resources.accessibility_message_status_loading
import augmy.composeapp.generated.resources.accessibility_message_status_read
import augmy.composeapp.generated.resources.accessibility_message_status_received
import augmy.composeapp.generated.resources.accessibility_message_status_sent
import org.jetbrains.compose.resources.stringResource

/** State of a message */
enum class MessageState {
    /** Message is being sent to the server */
    Pending,

    /** There was a problem with sending this message */
    Failed,

    /** Successfully uploaded to the server */
    Sent,

    /** Received by the other party, in other words, server attempted to notify the receiver */
    Received,

    /** Message was read by the recipient */
    Read;

    /** Vector representation of the state */
    val imageVector: ImageVector?
        get() = when(this) {
            Pending -> null
            Failed -> Icons.Outlined.Warning
            Sent -> Icons.Outlined.Check
            Received -> Icons.Outlined.DoneAll
            Read -> Icons.Outlined.Visibility
        }

    /** Textual description of the state */
    val description: String
        @Composable get() = stringResource(when(this) {
            Pending -> Res.string.accessibility_message_status_loading
            Failed -> Res.string.accessibility_message_status_failed
            Sent -> Res.string.accessibility_message_status_sent
            Received -> Res.string.accessibility_message_status_received
            Read -> Res.string.accessibility_message_status_read
        })
}