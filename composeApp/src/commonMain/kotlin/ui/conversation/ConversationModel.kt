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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val conversationModule = module {
    includes(keyboardModule)

    single { ConversationDataManager() }
    factory { ConversationRepository(get(), get(), get(), get(), get(), get<FileAccess>()) }
    factory {
        ConversationModel(
            get<String>(),
            get<Boolean>(),
            get<ConversationRepository>(),
            get<ConversationDataManager>(),
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
    private val dataManager: ConversationDataManager,
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


    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {
        if((conversationId.isNotBlank() && dataManager.conversations.value.second[conversationId] == null)
            || isSpecial
        ) {
            withContext(Dispatchers.IO) {
                repository.getConversationDetail(
                    conversationId = conversationId,
                    owner = matrixUserId
                )?.let { data ->
                    dataManager.updateConversations { it.apply { this[conversationId] = data } }
                    dataManager.conversations.value.second[conversationId] = data
                }
            }
        }
        withContext(Dispatchers.IO) {
            savedMessage.value = settings.getStringOrNull(
                "${SettingsKeys.KEY_LAST_MESSAGE}_$conversationId"
            )?.take(messageMaxLength) ?: ""

            if(dataManager.repositoryConfig.value == null || isSpecial) {
                currentUser.value?.matrixHomeserver?.let { homeserver ->
                    dataManager.repositoryConfig.value = repository.getMediaConfig(homeserver = homeserver).success?.data
                }
            }
        }

        if(isSpecial) {
            repository.invalidateLocalSource()
        }
    }

    private val _uploadProgress = MutableStateFlow<List<MediaHttpProgress>>(emptyList())

    // firstVisibleItemIndex to firstVisibleItemScrollOffset
    var persistentPositionData: PersistentListData? = null


    /** Detailed information about this conversation */
    val conversation = dataManager.conversations.map { it.second[conversationId] }

    /** Current typing indicators, indicating typing statuses of other users */
    val typingIndicators = sharedDataManager.typingIndicators.map { indicators ->
        withContext(Dispatchers.Default) {
            indicators.second[conversationId]?.userIds?.mapNotNull { userId ->
                if(userId.full != matrixUserId) {
                    ConversationTypingIndicator().apply {
                        user = dataManager.conversations.value.second[conversationId]?.members?.find { user ->
                            user.userId == userId.full
                        }
                    }
                }else null
            }.let {
                // hashcode to enforce recomposition
                it.hashCode() to it?.takeLast(MAX_TYPING_INDICATORS).orEmpty()
            }
        }
    }

    /** Current configuration of media repository */
    val repositoryConfig = dataManager.repositoryConfig.asStateFlow()

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
            homeserver = { homeserver },
            conversationId = conversationId
        ).flow
            .cachedIn(viewModelScope)
            .let {
                if(dataManager.conversations.value.second[conversationId] != null) {
                    it.combine(conversation) { messages, detail ->
                        withContext(Dispatchers.Default) {
                            messages.map { message ->
                                message.apply {
                                    user = detail?.members?.find { user -> user.userId == authorPublicId }
                                    anchorMessage?.user = detail?.members?.find { user -> user.userId == anchorMessage?.authorPublicId }
                                    reactions?.forEach { reaction ->
                                        reaction.user = detail?.members?.find { user -> user.userId == reaction.authorPublicId }
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
        viewModelScope.launch {
            onDataRequest(isSpecial = false, isPullRefresh = false)
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
                homeserver = homeserver,
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
                mediaFiles.mapNotNull { file ->
                    val bytes = file.readBytes()
                    if(bytes.isNotEmpty()) {
                        repository.uploadMedia(
                            mediaByteArray = bytes,
                            fileName = file.name,
                            homeserver = homeserver,
                            mimetype = MimeType.getByExtension(file.extension).mime
                        )?.success?.data?.contentUri.takeIf { !it.isNullOrBlank() }?.let { url ->
                            MediaIO(
                                url = url,
                                size = bytes.size.toLong(),
                                name = file.name,
                                mimetype = MimeType.getByExtension(file.extension).mime
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

    fun acceptUserVerification(eventId: String?) {
        if(eventId == null) throw Exception("Event id is null")
        println("kostka_test, acceptUserVerification, client: ${sharedDataManager.matrixClient.value}, eventId: $eventId, roomId: $conversationId")
        viewModelScope.launch {
            sharedDataManager.matrixClient.value?.verification?.getActiveUserVerification(
                roomId = RoomId(conversationId),
                eventId = EventId(eventId)
            )?.state.also {
                println("kostka_test, active user verification state: $it")
            }?.collectLatest { state ->
                println("kostka_test, active user verification state: $state")
                when(state) {
                    is ActiveVerificationState.AcceptedByOtherDevice -> {

                    }
                    is ActiveVerificationState.Cancel -> {

                    }
                    is ActiveVerificationState.Done -> {

                    }
                    is ActiveVerificationState.OwnRequest -> {

                    }
                    is ActiveVerificationState.Ready -> {
                        state.start(VerificationMethod.Sas)
                    }
                    is ActiveVerificationState.Start -> {

                    }
                    is ActiveVerificationState.TheirRequest -> {
                        state.ready()
                    }
                    is ActiveVerificationState.WaitForDone -> {

                    }
                    is ActiveVerificationState.Undefined -> {

                    }
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
            sendMessage(
                audioByteArray = byteArray,
                conversationId = conversationId,
                homeserver = homeserver,
                message = ConversationMessageIO()
            )
        }
    }
}

private const val MAX_TYPING_INDICATORS = 3
