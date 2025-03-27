package ui.conversation

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import augmy.interactive.shared.utils.PersistentListData
import base.utils.getUrlExtension
import components.pull_refresh.RefreshableViewModel
import data.io.app.SettingsKeys
import data.io.matrix.media.MediaRepositoryConfig
import data.io.matrix.room.ConversationRoomIO
import data.io.social.network.conversation.ConversationTypingIndicator
import data.io.social.network.conversation.MessageReactionRequest
import data.io.social.network.conversation.giphy.GifAsset
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import database.file.FileAccess
import io.github.vinceglb.filekit.core.PlatformFile
import korlibs.io.net.MimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.components.KeyboardModel
import ui.conversation.components.audio.MediaHttpProgress
import ui.conversation.components.emoji.EmojiUseCase
import ui.conversation.components.experimental.gravity.GravityData
import ui.conversation.components.experimental.gravity.GravityUseCase
import ui.conversation.components.experimental.gravity.GravityUseCase.Companion.TICK_MILLIS
import ui.conversation.components.experimental.gravity.GravityValue
import ui.conversation.components.experimental.pacing.PacingUseCase
import ui.conversation.components.experimental.pacing.PacingUseCase.Companion.WAVES_PER_PIXEL
import ui.conversation.components.gif.GifUseCase
import ui.conversation.components.keyboardModule
import ui.login.AUGMY_HOME_SERVER

internal val conversationModule = module {
    includes(keyboardModule)

    factory { ConversationRepository(get(), get(), get(), get(), get(), get<FileAccess>()) }
    factory {
        ConversationModel(
            get<String>(),
            get<Boolean>(),
            get<ConversationRepository>(),
            get<EmojiUseCase>(),
            get<GifUseCase>(),
            get<PacingUseCase>(),
            get<GravityUseCase>(),
        )
    }
    viewModelOf(::ConversationModel)
}

/** Communication between the UI, the control layers, and control and data layers */
open class ConversationModel(
    private val conversationId: String,
    enableMessages: Boolean,
    private val repository: ConversationRepository,
    emojiUseCase: EmojiUseCase,
    gifUseCase: GifUseCase,
    // experimental
    private val pacingUseCase: PacingUseCase,
    private val gravityUseCase: GravityUseCase
): KeyboardModel(
    emojiUseCase = emojiUseCase,
    gifUseCase = gifUseCase,
    conversationId = conversationId
), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    private val _conversationDetail = MutableStateFlow<ConversationRoomIO?>(null)
    private val _typingIndicators = MutableStateFlow<Pair<Int, List<ConversationTypingIndicator>>>(-1 to listOf())
    private val _uploadProgress = MutableStateFlow<List<MediaHttpProgress>>(emptyList())
    private val _repositoryConfig = MutableStateFlow<MediaRepositoryConfig?>(null)

    // firstVisibleItemIndex to firstVisibleItemScrollOffset
    var persistentPositionData: PersistentListData = PersistentListData()


    /** Detailed information about this conversation */
    val conversationDetail = _conversationDetail.asStateFlow()

    /** Current typing indicators, indicating typing statuses of other users */
    val typingIndicators = _typingIndicators.asStateFlow()

    /** Current configuration of media repository */
    val repositoryConfig = _repositoryConfig.asStateFlow()

    /** Progress of the current upload */
    val uploadProgress = _uploadProgress.asStateFlow()

    /** currently locally cached byte arrays */
    val temporaryFiles
        get() = repository.cachedFiles

    /** flow of current messages */
    val conversationMessages: Flow<PagingData<ConversationMessageIO>> = if(enableMessages) {
        repository.getMessagesListFlow(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = true,
                initialLoadSize = 30
            ),
            homeserver = {
                currentUser.value?.matrixHomeserver ?: AUGMY_HOME_SERVER
            },
            conversationId = conversationId
        ).flow
            .cachedIn(viewModelScope)
            .let {
                if(_conversationDetail.value != null) {
                    it.combine(_conversationDetail) { messages, detail ->
                        withContext(Dispatchers.Default) {
                            messages.map { message ->
                                message.apply {
                                    user = detail?.users?.find { user -> user.publicId == authorPublicId }
                                    anchorMessage?.user = detail?.users?.find { user -> user.publicId == anchorMessage?.authorPublicId }
                                    reactions?.forEach { reaction ->
                                        reaction.user = detail?.users?.find { user -> user.publicId == reaction.authorPublicId }
                                    }
                                }
                            }
                        }
                    }
                }else it
            }
    }else flow { PagingData.empty<ConversationMessageIO>() }

    /** Last saved message relevant to this conversation */
    val savedMessage = MutableStateFlow<String?>(null)
    val timingSensor = pacingUseCase.timingSensor
    val gravityValues = gravityUseCase.gravityValues.asStateFlow()

    private val messageMaxLength = 5000

    init {
        if(conversationId.isNotBlank() && _conversationDetail.value?.id != conversationId) {
            viewModelScope.launch {
                repository.getConversationDetail(
                    conversationId = conversationId,
                    owner = matrixUserId
                )?.let { data ->
                    _conversationDetail.value = data
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            savedMessage.value = settings
                .getStringOrNull("${SettingsKeys.KEY_LAST_MESSAGE}_$conversationId")
                ?.take(messageMaxLength)
                ?: ""

            currentUser.value?.matrixHomeserver?.let { homeserver ->
                _repositoryConfig.value = repository.getMediaConfig(homeserver = homeserver).success?.data
            }
        }

        // TODO socket listening to typing indicators
        //appendTypingIndicator()
    }

    override fun onCleared() {
        gravityUseCase.kill()
        super.onCleared()
    }

    // ==================== functions ===========================

    fun startTypingServices() {
        if(!timingSensor.value.isRunning && !timingSensor.value.isLocked) {
            viewModelScope.launch {
                timingSensor.value.start()
                gravityUseCase.start()
            }
        }
    }

    fun stopTypingServices() {
        if(timingSensor.value.isRunning) {
            timingSensor.value.pause()
            gravityUseCase.kill()
        }
    }

    fun onKeyPressed(char: Char, timingMs: Long) {
        viewModelScope.launch {
            pacingUseCase.onKeyPressed(char, timingMs)
        }
    }

    fun initPacing(widthPx: Float) {
        if(pacingUseCase.isInitialized) return
        viewModelScope.launch {
            gravityUseCase.init(conversationId)
            pacingUseCase.init(
                maxWaves = (WAVES_PER_PIXEL * widthPx).toInt(),
                conversationId = conversationId,
                savedMessage = savedMessage.value ?: ""
            )
            pacingUseCase.timingSensor.value.onTick(ms = TICK_MILLIS) {
                viewModelScope.launch { gravityUseCase.onTick() }
            }
            if(!savedMessage.value.isNullOrBlank()) startTypingServices()
        }
    }

    /** Saves the content of a message */
    fun cache(content: String?) {
        CoroutineScope(Job() + Dispatchers.IO).launch {
            val key = "${SettingsKeys.KEY_LAST_MESSAGE}_$conversationId"
            if(content != null) {
                settings.putString(key, content)
            }else settings.remove(key)

            if(content != null) {
                pacingUseCase.cache(conversationId)
            }else {
                pacingUseCase.clearCache(conversationId)
                gravityUseCase.clearCache(conversationId)
            }
            savedMessage.value = content
        }
    }

    /** Appends or updates a typing indicator */
    private fun appendTypingIndicator(indicator: ConversationTypingIndicator) {
        viewModelScope.launch(Dispatchers.Default) {
            _typingIndicators.update { previous ->
                val res = previous.second.toMutableList().apply {
                    // update existing indicator or add a new one
                    find { it.authorPublicId == indicator.authorPublicId }?.apply {
                        content = indicator.content
                    } ?: add(indicator.also {
                        // find relevant user for new indicator
                        it.user = _conversationDetail.value?.users?.find { user -> user.publicId == indicator.authorPublicId }
                    })

                    // remove irrelevant indicator - better than just filtering them
                    removeAll { it.content.isNullOrBlank() }
                }
                res.hashCode() to res.takeLast(MAX_TYPING_INDICATORS)
            }
        }
    }

    /** Marks a single message as transcribed */
    fun markMessageAsTranscribed(id: String?) {
        if(id == null) return
        viewModelScope.launch {
            repository.markMessageAsTranscribed(id = id)
        }
    }

    /**
     * Makes a request to send a conversation message
     * @param content textual content of the message
     * @param mediaFiles list of urls in the format of: [Pair.first]: file name, [Pair.second]: [ByteArray] content of the image
     */
    fun sendMessage(
        content: String,
        anchorMessage: ConversationMessageIO?,
        mediaFiles: List<PlatformFile>,
        mediaUrls: List<String>,
        timings: List<Long>,
        gravityValues: List<GravityValue>,
        gifAsset: GifAsset?,
        showPreview: Boolean
    ) {
        CoroutineScope(Job()).launch {
            var progressId = ""
            repository.sendMessage(
                conversationId = conversationId,
                homeserver = currentUser.value?.matrixHomeserver ?: AUGMY_HOME_SERVER,
                mediaFiles = mediaFiles,
                onProgressChange = { progress ->
                    _uploadProgress.update {
                        it.toMutableList().apply {
                            add(progress)
                            progressId = progress.id
                        }
                    }
                },
                gifAsset = gifAsset,
                message = ConversationMessageIO(
                    content = content,
                    gravityData = GravityData(
                        values = gravityValues.map { it.copy(conversationId = null) },
                        tickMs = TICK_MILLIS
                    ),
                    anchorMessage = anchorMessage?.toAnchorMessage(),
                    media = mediaUrls.mapNotNull { url ->
                        MediaIO(
                            url = url,
                            mimetype = MimeType.getByExtension(getUrlExtension(url)).mime
                        ).takeIf { url.isNotBlank() }
                    },
                    showPreview = showPreview,
                    timings = timings
                )
            )
            _uploadProgress.update { previous ->
                previous.toMutableList().apply {
                    removeAll { it.id == progressId }
                }
            }
        }
    }

    /** Makes a request to add or change reaction to a message */
    fun reactToMessage(messageId: String?, content: String) {
        if(messageId == null) return
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
                homeserver = currentUser.value?.matrixHomeserver ?: AUGMY_HOME_SERVER,
                message = ConversationMessageIO()
            )
        }
    }
}

private const val MAX_TYPING_INDICATORS = 3
