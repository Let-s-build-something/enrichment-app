package ui.conversation.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.conversation_reply_heading
import augmy.composeapp.generated.resources.conversation_reply_prefix_self
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.social.network.conversation.message.ConversationAnchorMessageIO
import data.io.social.network.conversation.message.ConversationMessageIO
import org.jetbrains.compose.resources.stringResource

/**
 * Indication of a message that a user is replying to.
 * @param data data relevant to the original message - [ConversationMessageIO.anchorMessage]
 * @param onClick on message click - the UI should scroll to the original message
 * @param onRemoveRequest whenever user attempts to remove the reply indication
 * @param removable
 */
@Composable
fun ReplyIndication(
    modifier: Modifier = Modifier,
    data: ConversationAnchorMessageIO,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
    onRemoveRequest: () -> Unit = {},
    removable: Boolean = false
) {
    Box {
        Row(
            modifier = modifier
                .clickable {
                    onClick()
                }
                .background(
                    color = LocalTheme.current.colors.backgroundContrast,
                    shape = RoundedCornerShape(
                        topStart = LocalTheme.current.shapes.componentCornerRadius,
                        topEnd = LocalTheme.current.shapes.componentCornerRadius
                    )
                )
                .padding(top = 2.dp, bottom = 10.dp, start = 16.dp, end = 8.dp)
        ) {
            if(removable) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(Res.string.conversation_reply_heading),
                    style = LocalTheme.current.styles.regular
                )
            }
            Column(
                modifier = Modifier
                    .padding(top = 8.dp, start = 6.dp)
                //.weight(1f)
                //.width(IntrinsicSize.Min)
            ) {
                Text(
                    //modifier = Modifier.fillMaxWidth(),
                    text = if(isCurrentUser) {
                        stringResource(Res.string.conversation_reply_prefix_self)
                    }else data.user?.name?.plus(":") ?: "",
                    style = LocalTheme.current.styles.title.copy(fontSize = 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                    text = data.content ?: "",
                    style = LocalTheme.current.styles.regular.copy(fontSize = 14.sp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if(removable) {
            MinimalisticIcon(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .align(Alignment.TopEnd),
                imageVector = Icons.Outlined.Close,
                tint = LocalTheme.current.colors.secondary,
                onTap = {
                    onRemoveRequest()
                }
            )
        }
    }
}