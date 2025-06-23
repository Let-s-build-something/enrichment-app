package ui.conversation.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import org.koin.core.context.loadKoinModules

@Composable
fun ConversationDetailDialog(
    onDismissRequest: () -> Unit,
    conversationId: String,
) {
    loadKoinModules(conversationSettingsModule)

    AlertDialog(
        intrinsicContent = false,
        additionalContent = {
            ConversationSettingsContent(
                modifier = Modifier.animateContentSize(),
                conversationId = conversationId
            )
        },
        onDismissRequest = onDismissRequest
    )
}