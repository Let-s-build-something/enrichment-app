package ui.conversation.message

import androidx.lifecycle.viewModelScope
import data.io.social.network.conversation.message.ConversationMessageIO
import data.shared.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val messageDetailModule = module {
    factory { MessageDetailRepository(get(), get()) }
    factory {
        MessageDetailViewModel(
            get<String>(),
            get<MessageDetailRepository>()
        )
    }
    viewModelOf(::MessageDetailViewModel)
}

class MessageDetailViewModel(
    private val messageId: String?,
    private val repository: MessageDetailRepository
): SharedViewModel() {

    private val _message = MutableStateFlow<ConversationMessageIO?>(null)

    /** Locally retrieved information */
    val message = _message.asStateFlow()

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