package ui.conversation.message

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import data.io.social.network.conversation.message.FullConversationMessage
import database.file.FileAccess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.ConversationDataManager
import ui.conversation.ConversationModel
import ui.conversation.components.emoji.EmojiUseCase
import ui.conversation.components.experimental.gravity.GravityUseCase
import ui.conversation.components.experimental.pacing.PacingUseCase
import ui.conversation.components.gif.GifUseCase

internal val messageDetailModule = module {
    factory {
        MessageDetailRepository(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
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
    conversationId = MutableStateFlow(conversationId ?: ""),
    enableMessages = false,
    repository = repository,
    emojiUseCase = emojiUseCase,
    gifUseCase = gifUseCase,
    pacingUseCase = pacingUseCase,
    gravityUseCase = gravityUseCase,
    fileAccess = fileAccess,
    dataManager = dataManager
) {

    private val _message = MutableStateFlow<FullConversationMessage?>(null)

    /** Locally retrieved information */
    val message = _message.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val replies = super.conversationId.flatMapLatest { conversationId ->
        repository.getMessagesListFlow(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = true,
                initialLoadSize = 50
            ),
            anchorMessageId = messageId,
            conversationId = conversationId
        ).flow.cachedIn(viewModelScope)
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
