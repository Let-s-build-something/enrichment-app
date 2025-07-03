package ui.conversation.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.conversation_reply_heading
import augmy.composeapp.generated.resources.conversation_reply_prefix_self
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.social.network.conversation.message.FullConversationMessage
import database.dao.ConversationMessageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import ui.conversation.components.MediaElement

internal val replyModule = module {
    factory { ReplyRepository(get()) }

    factory { (fullMessage: FullConversationMessage) ->
        ReplyModel(fullMessage, get())
    }
    viewModel { (fullMessage: FullConversationMessage) ->
        ReplyModel(fullMessage, get())
    }
}

class ReplyModel(
    argument: FullConversationMessage,
    repository: ReplyRepository
): ViewModel() {
    private val _message = MutableStateFlow<FullConversationMessage?>(null)
    val message = _message.asStateFlow()

    init {
        viewModelScope.launch {
            _message.value = (if (argument.author == null) repository.getMessage(argument.id) else null) ?: argument
        }
    }
}

class ReplyRepository(private val conversationMessageDao: ConversationMessageDao) {

    suspend fun getMessage(id: String) = withContext(Dispatchers.IO) {
        conversationMessageDao.get(id)
    }
}

/**
 * Indication of a message that a user is replying to.
 * @param data data relevant to the original message
 * @param onClick on message click - the UI should scroll to the original message
 * @param onRemoveRequest whenever user attempts to remove the reply indication
 * @param removable
 */
@Composable
fun ReplyIndication(
    modifier: Modifier = Modifier,
    isCurrentUser: Boolean,
    data: FullConversationMessage,
    onClick: () -> Unit,
    onRemoveRequest: () -> Unit = {},
    removable: Boolean = false
) {
    loadKoinModules(replyModule)
    val model: ReplyModel = koinViewModel(
        key = data.toString(),
        parameters = {
            parametersOf(data)
        }
    )
    val message = model.message.collectAsState()

    Box {
        Row(
            modifier = modifier
                .clickable { onClick() }
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
                modifier = Modifier.padding(top = 8.dp, start = 6.dp)
            ) {
                Text(
                    text = if (isCurrentUser) {
                        stringResource(Res.string.conversation_reply_prefix_self)
                    }else message.value?.author?.displayName ?: data.author?.displayName ?: "sender",
                    style = LocalTheme.current.styles.title.copy(fontSize = 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                (message.value?.media?.firstOrNull() ?: data.media.firstOrNull())?.let { firstMedia ->
                    MediaElement(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .widthIn(max = 80.dp)
                            .alpha(.4f)
                            .align(Alignment.End),
                        media = firstMedia
                    )
                }

                (message.value?.data?.content ?: data.data.content)?.let {
                    Text(
                        modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                        text = it,
                        style = LocalTheme.current.styles.regular.copy(fontSize = 14.sp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
