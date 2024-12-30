@file:OptIn(ExperimentalSettingsApi::class)

package ui.conversation

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.russhwolf.settings.ExperimentalSettingsApi
import components.pull_refresh.RefreshableViewModel
import data.io.app.SettingsKeys
import data.io.social.network.conversation.ConversationMessageIO
import data.io.social.network.conversation.MessageReactionRequest
import data.io.social.network.conversation.NetworkConversationIO
import data.io.social.network.conversation.giphy.GifAsset
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.ConversationRepository.Companion.demoConversationDetail
import ui.conversation.components.KeyboardViewModel
import ui.conversation.components.audio.audioProcessorModule
import ui.conversation.components.emoji.EmojiUseCase
import ui.conversation.components.gif.GifUseCase
import ui.conversation.components.keyboardModule

internal val conversationModule = module {
    includes(keyboardModule)
    includes(audioProcessorModule)

    factory { ConversationRepository(get(), get(), get()) }
    factory {
        ConversationViewModel(
            get<ConversationRepository>(),
            get<String>(),
            get<EmojiUseCase>(),
            get<GifUseCase>()
        )
    }
    viewModelOf(::ConversationViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class ConversationViewModel(
    private val repository: ConversationRepository,
    private val conversationId: String,
    emojiUseCase: EmojiUseCase,
    gifUseCase: GifUseCase
): KeyboardViewModel(
    emojiUseCase = emojiUseCase,
    gifUseCase = gifUseCase,
    conversationId = conversationId
), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    private val _conversationDetail = MutableStateFlow<NetworkConversationIO?>(null)

    /** Detailed information about this conversation */
    val conversationDetail = _conversationDetail.asStateFlow()

    /** currently locally cached byte arrays */
    val cachedByteArrays
        get() = repository.cachedByteArrays

    /** flow of current messages */
    val conversationMessages: Flow<PagingData<ConversationMessageIO>> = repository.getMessagesListFlow(
        config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = true,
            initialLoadSize = 50
        ),
        conversationId = conversationId
    ).flow
        .cachedIn(viewModelScope)
        .combine(_conversationDetail) { messages, detail ->
            withContext(Dispatchers.Default) {
                messages.map {
                    it.apply {
                        user = detail?.users?.find { user -> user.publicId == authorPublicId }
                    }
                }
            }
        }

    /** Last saved message relevant to this conversation */
    val savedMessage = MutableStateFlow("")

    init {
        if(conversationId.isNotBlank() && _conversationDetail.value?.publicId != conversationId) {
            viewModelScope.launch {
                repository.getConversationDetail(conversationId = conversationId).success?.data?.let { data ->
                    _conversationDetail.value = data
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            savedMessage.value = settings.getStringOrNull("${SettingsKeys.KEY_LAST_MESSAGE}_$conversationId") ?: ""
        }
        // TODO remove demo data
        _conversationDetail.value = demoConversationDetail
    }



    // ==================== functions ===========================

    /** Saves content of a message */
    fun saveMessage(content: String?) {
        CoroutineScope(Job() + Dispatchers.IO).launch {
            val key = "${SettingsKeys.KEY_LAST_MESSAGE}_$conversationId"
            if(content != null) {
                settings.putString(key, content)
            }else settings.remove(key)
        }
    }

    /**
     * Makes a request to send a conversation message
     * @param content textual content of the message
     * @param mediaFiles list of urls in the format of: [Pair.first]: file name, [Pair.second]: [ByteArray] content of the image
     */
    fun sendMessage(
        content: String,
        anchorMessageId: String?,
        mediaFiles: List<PlatformFile>,
        gifAsset: GifAsset?
    ) {
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = conversationId,
                mediaFiles = mediaFiles,
                message = ConversationMessageIO(
                    content = content,
                    anchorMessageId = anchorMessageId,
                    gifAsset = gifAsset
                )
            )
        }
    }

    /** Makes a request to add or change reaction to a message */
    fun reactToMessage(messageId: String, content: String) {
        viewModelScope.launch {
            repository.reactToMessage(
                conversationId = conversationId,
                reaction = MessageReactionRequest(
                    content = content,
                    messageId = messageId
                )
            )
        }
    }

    /** Makes a request to send a conversation audio message */
    fun sendAudioMessage(byteArray: ByteArray) {
        viewModelScope.launch {
            repository.sendMessage(
                audioByteArray = byteArray,
                conversationId = conversationId,
                message = ConversationMessageIO()
            )
        }
    }
}
