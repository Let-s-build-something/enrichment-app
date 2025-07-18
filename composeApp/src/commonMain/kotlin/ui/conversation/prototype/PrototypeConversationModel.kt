package ui.conversation.prototype

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import database.file.FileAccess
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.ConversationDataManager
import ui.conversation.ConversationModel
import ui.conversation.ConversationRepository
import ui.conversation.components.emoji.EmojiUseCase
import ui.conversation.components.experimental.gravity.GravityUseCase
import ui.conversation.components.experimental.pacing.PacingUseCase
import ui.conversation.components.gif.GifUseCase

internal val prototypeConversationModule = module {
    factory {
        PrototypeConversationModel(
            get<String>(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModelOf(::PrototypeConversationModel)
}

class PrototypeConversationModel(
    conversationId: String?,
    dataManager: ConversationDataManager,
    repository: ConversationRepository,
    emojiUseCase: EmojiUseCase,
    gifUseCase: GifUseCase,
    pacingUseCase: PacingUseCase,
    gravityUseCase: GravityUseCase,
    fileAccess: FileAccess
): ConversationModel(
    conversationId = MutableStateFlow(conversationId ?: ""),
    repository = repository,
    emojiUseCase = emojiUseCase,
    gifUseCase = gifUseCase,
    pacingUseCase = pacingUseCase,
    gravityUseCase = gravityUseCase,
    fileAccess = fileAccess,
    dataManager = dataManager
) {

    val messages = repository.getMessagesListFlow(
        config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = true,
            initialLoadSize = 50
        ),
        homeserver = { homeserverAddress },
        conversationId = conversationId
    ).flow.cachedIn(viewModelScope)
}