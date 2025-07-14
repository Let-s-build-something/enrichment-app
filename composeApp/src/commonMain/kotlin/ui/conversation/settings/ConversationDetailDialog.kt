package ui.conversation.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import net.folivo.trixnity.clientserverapi.model.rooms.GetPublicRoomsResponse
import org.koin.core.context.loadKoinModules

@Composable
fun ConversationDetailDialog(
    conversationId: String,
    publicRoom: GetPublicRoomsResponse.PublicRoomsChunk? = null,
    onDismissRequest: () -> Unit
) {
    loadKoinModules(conversationSettingsModule)

    AlertDialog(
        intrinsicContent = false,
        additionalContent = {
            ConversationSettingsContent(
                modifier = Modifier.animateContentSize(),
                publicRoom = publicRoom,
                conversationId = conversationId
            )
        },
        onDismissRequest = onDismissRequest
    )
}