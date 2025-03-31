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
import ui.conversation.ConversationModel
import ui.conversation.components.emoji.EmojiUseCase
import ui.conversation.components.experimental.gravity.GravityUseCase
import ui.conversation.components.experimental.pacing.PacingUseCase
import ui.conversation.components.gif.GifUseCase

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
            get()
        )
    }
    viewModelOf(::MessageDetailModel)
}

class MessageDetailModel(
    private val messageId: String?,
    conversationId: String?,
    private val repository: MessageDetailRepository,
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
    fileAccess = fileAccess
) {

    private val _message = MutableStateFlow<ConversationMessageIO?>(null)

    /** Locally retrieved information */
    val message = _message
        .combine(conversationDetail) { message, detail ->
            withContext(Dispatchers.Default) {
                message?.apply {
                    user = detail?.users?.find { user -> user.publicId == authorPublicId }
                    anchorMessage?.user = detail?.users?.find { user -> user.publicId == anchorMessage?.authorPublicId }
                    reactions?.forEach { reaction ->
                        reaction.user = detail?.users?.find { user -> user.publicId == reaction.authorPublicId }
                    }
                }
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
        .combine(conversationDetail) { messages, detail ->
            withContext(Dispatchers.Default) {
                messages.map {
                    it.apply {
                        user = detail?.users?.find { user -> user.publicId == authorPublicId }
                        anchorMessage?.user = detail?.users?.find { user -> user.publicId == anchorMessage?.authorPublicId }
                        reactions?.forEach { reaction ->
                            reaction.user = detail?.users?.find { user -> user.publicId == reaction.authorPublicId }
                        }
                    }
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
        _message.value = repository.getMessage(
            id = messageId,
            ownerPublicId = matrixUserId
        )
    }
}