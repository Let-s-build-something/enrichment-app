package ui.conversation

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import augmy.interactive.shared.utils.DateUtils.localNow
import augmy.interactive.shared.utils.PersistentListData
import base.utils.getUrlExtension
import base.utils.sha256
import components.pull_refresh.RefreshableViewModel
import data.io.app.SettingsKeys
import data.io.matrix.media.MediaRepositoryConfig
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.event.ConversationTypingIndicator
import data.io.social.network.conversation.MessageReactionRequest
import data.io.social.network.conversation.giphy.GifAsset
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageState
import database.file.FileAccess
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.extension
import korlibs.io.net.MimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
            get<FileAccess>(),
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
    private val gravityUseCase: GravityUseCase,
    private val fileAccess: FileAccess
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

        viewModelScope.launch(Dispatchers.Default) {
            sharedDataManager.typingIndicators.collectLatest { indicators ->
                _typingIndicators.value = indicators.second[conversationId]?.userIds?.mapNotNull { userId ->
                    if(userId.full != matrixUserId) {
                        ConversationTypingIndicator().apply {
                            user = _conversationDetail.value?.users?.find { user ->
                                user.userMatrixId == userId.full
                            }
                        }
                    }else null
                }.let {
                    // hashcode to enforce recomposition
                    it.hashCode() to it?.takeLast(MAX_TYPING_INDICATORS).orEmpty()
                }
            }
        }
    }

    override fun onCleared() {
        gravityUseCase.kill()
        super.onCleared()
    }

    // ==================== functions ===========================

    /** Experimental typing services */
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

    fun updateTypingStatus(content: CharSequence) {
        viewModelScope.launch {
            repository.updateTypingIndicator(
                conversationId = conversationId,
                indicator = ConversationTypingIndicator(content = content.toString())
            )
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
            sendMessage(
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

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun sendMessage(
        conversationId: String,
        homeserver: String,
        message: ConversationMessageIO,
        audioByteArray: ByteArray? = null,
        gifAsset: GifAsset? = null,
        onProgressChange: ((MediaHttpProgress) -> Unit)? = null,
        mediaFiles: List<PlatformFile> = listOf()
    ) {
        val uuids = mutableListOf<String>()
        val msgId = "temporary_${Uuid.random()}"

        // placeholder/loading message preview
        var msg = message.copy(
            id = msgId,
            conversationId = conversationId,
            sentAt = localNow,
            authorPublicId = currentUser.value?.matrixUserId,
            media = withContext(Dispatchers.Default) {
                mediaFiles.map { media ->
                    MediaIO(
                        url = (Uuid.random().toString() + ".${media.extension.lowercase()}").also { uuid ->
                            repository.cachedFiles.update { prev ->
                                prev.apply { this[uuid] = media }
                            }
                            uuids.add(uuid)
                        }
                    )
                }.toMutableList().apply {
                    addAll(message.media.orEmpty())
                    if(audioByteArray != null) {
                        add(MediaIO(url = MESSAGE_AUDIO_URL_PLACEHOLDER))
                    }
                    add(
                        MediaIO(
                            url = gifAsset?.original,
                            mimetype = MimeType.IMAGE_GIF.mime,
                            name = gifAsset?.description
                        )
                    )
                }.filter { !it.isEmpty }
            },
            state = MessageState.Pending
        )
        repository.cacheMessage(msg)
        repository.invalidateLocalSource()

        // real message
        msg = msg.copy(
            media = withContext(Dispatchers.Default) {
                mediaFiles.mapNotNull { media ->
                    val bytes = media.readBytes()
                    val fileName = "${Uuid.random()}.${media.extension.lowercase()}"
                    if(bytes.isNotEmpty()) {
                        repository.uploadMedia(
                            mediaByteArray = bytes,
                            fileName = fileName,
                            homeserver = homeserver,
                            mimetype = MimeType.getByExtension(media.extension).mime
                        )?.success?.data?.contentUri.takeIf { !it.isNullOrBlank() }?.let { url ->
                            MediaIO(
                                url = url,
                                size = bytes.size.toLong(),
                                name = fileName,
                                mimetype = MimeType.getByExtension(media.extension).mime
                            )
                        }
                    }else null
                }.toMutableList().apply {
                    // existing media
                    addAll(message.media.orEmpty().toMutableList().filter { !it.isEmpty })

                    // audio file
                    if(audioByteArray != null) {
                        val size = audioByteArray.size.toLong()
                        val fileName = "${Uuid.random()}.wav"
                        val mimetype = MimeType.getByExtension("wav").mime

                        repository.uploadMedia(
                            mediaByteArray = audioByteArray,
                            fileName = fileName,
                            homeserver = homeserver,
                            mimetype = mimetype
                        )?.success?.data?.contentUri.let { audioUrl ->
                            if(!audioUrl.isNullOrBlank()) {
                                fileAccess.saveFileToCache(
                                    data = audioByteArray,
                                    fileName = sha256(audioUrl)
                                )?.let {
                                    remove(MediaIO(url = MESSAGE_AUDIO_URL_PLACEHOLDER))
                                    add(
                                        MediaIO(
                                            url = audioUrl,
                                            size = size,
                                            name = fileName,
                                            mimetype = mimetype,
                                            path = it.toString()
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // GIPHY asset
                    if(!gifAsset?.original.isNullOrBlank()) {
                        MediaIO(
                            url = gifAsset?.original,
                            mimetype = MimeType.IMAGE_GIF.mime,
                            name = gifAsset?.description
                        )
                    }
                }
            },
            state = if(sharedDataManager.matrixClient.value != null) MessageState.Pending else MessageState.Failed
        )

        if(sharedDataManager.matrixClient.value != null) {
            repository.sendMessage(
                client = sharedDataManager.matrixClient.value,
                conversationId = conversationId,
                message = msg
            )?.let { result ->
                repository.cacheMessage(
                    msg.copy(
                        id = result.getOrNull()?.full ?: "",
                        state = if(result.isSuccess) MessageState.Sent else MessageState.Failed
                    )
                )
                // we don't need the local message anymore
                repository.removeMessage(msg.id)
                if(result.isSuccess) {
                    repository.cachedFiles.update { prev ->
                        prev.apply {
                            uuids.forEachIndexed { index, s ->
                                msg.media?.getOrNull(index)?.url?.let { remove(it) }
                                remove(s)
                            }
                        }
                    }
                }
            }
        }
        println("kostka_test, new message sent: $msg")
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
            sendMessage(
                audioByteArray = byteArray,
                conversationId = conversationId,
                homeserver = currentUser.value?.matrixHomeserver ?: AUGMY_HOME_SERVER,
                message = ConversationMessageIO()
            )
        }
    }
}

private const val MAX_TYPING_INDICATORS = 3
