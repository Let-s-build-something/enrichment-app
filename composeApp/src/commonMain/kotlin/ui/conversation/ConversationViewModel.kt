package ui.conversation

import androidx.compose.animation.core.Animatable
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import augmy.composeapp.generated.resources.Res
import components.pull_refresh.RefreshableViewModel
import data.io.app.SettingsKeys
import data.io.social.network.conversation.ConversationMessageIO
import data.io.social.network.conversation.EmojiData
import data.io.social.network.conversation.MessageReactionRequest
import data.io.social.network.conversation.NetworkConversationIO
import data.shared.SharedViewModel
import data.shared.fromByteArrayToData
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.ConversationRepository.Companion.demoConversationDetail
import ui.conversation.components.emoji.EmojiUseCase
import ui.conversation.components.giphy.GiphyUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val conversationModule = module {
    factory { EmojiDataManager() }
    single { EmojiDataManager() }
    factory { ConversationRepository(get()) }
    factory { EmojiUseCase(get(), get()) }
    factory { GiphyUseCase() }
    factory {
        ConversationViewModel(get<ConversationRepository>(), get(), get(), get())
    }
    viewModelOf(::ConversationViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class ConversationViewModel(
    private val repository: ConversationRepository,
    private val conversationId: String,
    private val emojiUseCase: EmojiUseCase,
    private val giphyUseCase: GiphyUseCase
): SharedViewModel(), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    private val _conversationDetail = MutableStateFlow<NetworkConversationIO?>(null)

    val additionalBottomPadding = Animatable(0f)

    /** Detailed information about this conversation */
    val conversationDetail = _conversationDetail.asStateFlow()

    /** Preferred emojis of an individual conversation */
    val preferredEmojis = emojiUseCase.preferredEmojis.asStateFlow()

    /** List of all available emojis */
    val emojis = emojiUseCase.emojis

    /** Whether there is emoji filter */
    val areEmojisFiltered = emojiUseCase.areEmojisFiltered.asStateFlow()


    /** flow of current messages */
    val conversationMessages: Flow<PagingData<ConversationMessageIO>> = repository.getMessagesListFlow(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true,
            initialLoadSize = 20
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
    var savedMessage: String = settings.getStringOrNull("${SettingsKeys.KEY_LAST_MESSAGE}_$conversationId") ?: ""
        set(value) {
            field = value
            settings.putString("${SettingsKeys.KEY_LAST_MESSAGE}_$conversationId", value)
        }

    /** Last height of soft keyboard */
    var keyboardHeight: Int = settings.getIntOrNull(SettingsKeys.KEY_KEYBOARD_HEIGHT) ?: 0
        set(value) {
            field = value
            settings.putInt(SettingsKeys.KEY_KEYBOARD_HEIGHT, value)
        }

    /** Whether hint about emoji preference should be displayed */
    var showEmojiPreferenceHint: Boolean = settings.getBooleanOrNull(SettingsKeys.KEY_SHOW_EMOJI_PREFERENCE_HINT) ?: true
        set(value) {
            field = value
            settings.putBoolean(SettingsKeys.KEY_SHOW_EMOJI_PREFERENCE_HINT, value)
        }

    init {
        if(conversationId.isNotBlank() && _conversationDetail.value?.publicId != conversationId) {
            viewModelScope.launch {
                repository.getConversationDetail(conversationId = conversationId).success?.data?.let { data ->
                    _conversationDetail.value = data
                }
            }
        }
        viewModelScope.launch {
            emojiUseCase.initialize()
        }
        // TODO remove demo data
        _conversationDetail.value = demoConversationDetail
    }



    // ==================== functions ===========================

    /** Filters emojis */
    fun filterEmojis(query: String) {
        viewModelScope.launch {
            emojiUseCase.filterEmojis(query)
        }
    }

    /** Updates preferred emojis */
    suspend fun updatePreferredEmojiSet(list: List<EmojiData>) {
        viewModelScope.launch {
            emojiUseCase.updatePreferredEmojiSet(list)
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
        mediaFiles: MutableList<PlatformFile>
    ) {
        viewModelScope.launch {
            val mediaUrls = mediaFiles.mapNotNull { media ->
                requestMediaUpload(
                    mediaByteArray = media.readBytes(),
                    fileName = media.name
                ).takeIf { !it.isNullOrBlank() }
            }

            repository.sendMessage(
                conversationId = conversationId,
                message = ConversationMessageIO(
                    content = content,
                    mediaUrls = mediaUrls,
                    anchorMessageId = anchorMessageId
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
            // TODO change local data and refresh
        }
    }

    /** Makes a request to send a conversation audio message */
    @OptIn(ExperimentalUuidApi::class)
    fun sendAudioMessage(byteArray: ByteArray) {
        viewModelScope.launch {
            val audioUrl = requestMediaUpload(
                mediaByteArray = byteArray,
                fileName = Uuid.random().toString()
            ).takeIf { !it.isNullOrBlank() }

            repository.sendMessage(
                conversationId = conversationId,
                message = ConversationMessageIO(
                    audioUrl = audioUrl
                )
            )
        }
    }

    /** Makes a request to change user's profile picture */
    private suspend fun requestMediaUpload(
        mediaByteArray: ByteArray?,
        fileName: String
    ): String? {
        return try {
            if(mediaByteArray == null) null
            else uploadPictureStorage(
                byteArray = mediaByteArray,
                fileName = fileName
            )
        }catch (e: Exception) {
            null
        }
    }

    /** @return if true, it was successful, if false, it failed */
    private suspend fun uploadPictureStorage(
        byteArray: ByteArray,
        fileName: String
    ): String {
        return withContext(Dispatchers.IO) {
            val reference = Firebase.storage.reference.child("conversations/$conversationId/$fileName")

            reference.putData(fromByteArrayToData(byteArray))
            reference.getDownloadUrl()
        }
    }
}
