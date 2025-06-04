package ui.conversation.message

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import data.io.social.network.conversation.message.ConversationMessageIO
import database.file.FileAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.ConversationDataManager
import ui.conversation.ConversationModel
import ui.conversation.components.emoji.EmojiUseCase
import ui.conversation.components.experimental.gravity.GravityUseCase
import ui.conversation.components.experimental.pacing.PacingUseCase
import ui.conversation.components.gif.GifUseCase
import kotlin.collections.map
import kotlin.collections.orEmpty

internal val messageDetailModule = module {
    factory { MessageDetailRepository(get(), get(), get(), get(), get(), get()) }
    factory {
        MessageDetailModel(
            get<String>(),
            get<String>(),
            get<MessageDetailRepository>(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModelOf(::MessageDetailModel)
}

class MessageDetailModel(
    private val messageId: String?,
    conversationId: String?,
    private val repository: MessageDetailRepository,
    dataManager: ConversationDataManager,
    emojiUseCase: EmojiUseCase,
    gifUseCase: GifUseCase,
    pacingUseCase: PacingUseCase,
    gravityUseCase: GravityUseCase,
    fileAccess: FileAccess
): ConversationModel(
    conversationId = conversationId ?: "",
    enableMessages = false,
    repository = repository,
    emojiUseCase = emojiUseCase,
    gifUseCase = gifUseCase,
    pacingUseCase = pacingUseCase,
    gravityUseCase = gravityUseCase,
    fileAccess = fileAccess,
    dataManager = dataManager
) {

    private val _message = MutableStateFlow<ConversationMessageIO?>(null)

    /** Locally retrieved information */
    val message = _message
        .combine(conversation) { message, detail ->
            withContext(Dispatchers.Default) {
                message?.copy(
                    user = detail?.summary?.members?.find { user -> user.userId == message.authorPublicId },
                    anchorMessage = message.anchorMessage?.copy(
                        user = detail?.summary?.members?.find { user -> user.userId == message.anchorMessage.authorPublicId }
                    ),
                    reactions = message.reactions?.map { reaction ->
                        reaction.copy(
                            user = detail?.summary?.members?.find { user -> user.userId == reaction.authorPublicId }
                        )
                    }?.toList().orEmpty()
                )
            }
        }

    val replies = repository.getMessagesListFlow(
        config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = true,
            initialLoadSize = 50
        ),
        anchorMessageId = messageId,
        conversationId = conversationId
    ).flow
        .cachedIn(viewModelScope)
        .combine(conversation) { messages, detail ->
            withContext(Dispatchers.Default) {
                messages.map { message ->
                    message.copy(
                        user = detail?.summary?.members?.find { user -> user.userId == message.authorPublicId },
                        anchorMessage = message.anchorMessage?.copy(
                            user = detail?.summary?.members?.find { user -> user.userId == message.anchorMessage.authorPublicId }
                        ),
                        reactions = message.reactions?.map { reaction ->
                            reaction.copy(
                                user = detail?.summary?.members?.find { user -> user.userId == reaction.authorPublicId }
                            )
                        }?.toList().orEmpty()
                    )
                }
            }
        }

    init {
        viewModelScope.launch {
            requestMessage()
        }
    }

    private suspend fun requestMessage() {
        if(messageId == null) return
        _message.value = repository.getMessage(id = messageId)
    }
}