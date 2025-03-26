package ui.conversation

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import augmy.interactive.shared.utils.DateUtils.localNow
import base.utils.sha256
import data.io.base.BaseResponse
import data.io.matrix.media.MediaRepositoryConfig
import data.io.matrix.media.MediaUploadResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.social.network.conversation.MessageReactionRequest
import data.io.social.network.conversation.giphy.GifAsset
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.ConversationMessagesResponse
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageReactionIO
import data.io.social.network.conversation.message.MessageState
import data.shared.SharedDataManager
import data.shared.sync.DataSyncHandler
import data.shared.sync.MessageProcessor
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.NetworkItemDao
import database.file.FileAccess
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.baseName
import io.github.vinceglb.filekit.core.extension
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import korlibs.io.net.MimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import ui.conversation.components.audio.MediaHttpProgress
import ui.conversation.components.audio.MediaProcessorDataManager
import ui.login.safeRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Class for calling APIs and remote work in general */
open class ConversationRepository(
    private val httpClient: HttpClient,
    private val conversationMessageDao: ConversationMessageDao,
    internal val conversationRoomDao: ConversationRoomDao,
    private val networkItemDao: NetworkItemDao,
    private val mediaDataManager: MediaProcessorDataManager,
    private val fileAccess: FileAccess
) {
    private val dataSyncHandler = DataSyncHandler()
    protected var currentPagingSource: PagingSource<*, *>? = null
    val cachedFiles = hashMapOf<String, PlatformFile?>()
    protected var certainMessageCount: Int? = null

    /** Attempts to invalidate local PagingSource with conversation messages */
    protected fun invalidateLocalSource() {
        certainMessageCount = null
        currentPagingSource?.invalidate()
    }

    /**
     * returns a list of network list
     * @param dir - direction of the list
     * @param fromBatch - The token to start returning events from, usually the "prev_batch" of the timeline.
     */
    private suspend fun getMessages(
        dir: String = "b",
        homeserver: String,
        fromBatch: String? = null,
        limit: Int,
        conversationId: String?
    ): BaseResponse<ConversationMessagesResponse> {
        return withContext(Dispatchers.IO) {
            if(conversationId == null) {
                return@withContext BaseResponse.Error()
            }

            httpClient.safeRequest<ConversationMessagesResponse> {
                get(
                    urlString = "https://${homeserver}/_matrix/client/v3/rooms/$conversationId/messages",
                    block =  {
                        parameter("limit", limit)
                        parameter("dir", dir)
                        parameter("from", fromBatch)
                    }
                )
            }
        }
    }

    private suspend fun getAndStoreNewMessages(
        homeserver: String,
        fromBatch: String?,
        limit: Int,
        conversationId: String?
    ): MessageProcessor.SaveEventsResult? {
        return if(conversationId != null) {
            getMessages(
                limit = limit,
                conversationId = conversationId,
                fromBatch = fromBatch,
                homeserver = homeserver
            ).success?.data?.let { data ->
                dataSyncHandler.saveEvents(
                    events = data.chunk.orEmpty() + data.state.orEmpty(),
                    roomId = conversationId,
                    prevBatch = data.end
                )
            }
        }else null
    }

    /** Returns a flow of conversation messages */
    fun getMessagesListFlow(
        homeserver: () -> String,
        config: PagingConfig,
        conversationId: String? = null
    ): Pager<Int, ConversationMessageIO> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                ConversationRoomSource(
                    getMessages = { page ->
                        if(conversationId == null) return@ConversationRoomSource GetMessagesResponse(
                            data = listOf(), hasNext = false
                        )

                        withContext(Dispatchers.IO) {
                            val prevBatch = conversationRoomDao.get(conversationId)?.prevBatch

                            (conversationMessageDao.getPaginated(
                                conversationId = conversationId,
                                limit = config.pageSize,
                                offset = page * config.pageSize
                            ).let { res ->
                                if(res.isNotEmpty()) {
                                    GetMessagesResponse(
                                        data = res,
                                        hasNext = res.size == config.pageSize
                                                || (prevBatch != null && res.isNotEmpty())
                                    ).also {
                                        println("kostka_test, page: $page, hasNext: ${it.hasNext}, size: ${it.data.size}")
                                    }
                                }else null
                            } ?: getAndStoreNewMessages(
                                limit = config.pageSize,
                                conversationId = conversationId,
                                fromBatch = prevBatch,
                                homeserver = homeserver()
                            )?.let { res ->
                                certainMessageCount = null
                                conversationRoomDao.setPrevBatch(
                                    id = conversationId,
                                    prevBatch = res.prevBatch
                                )
                                GetMessagesResponse(
                                    data = res.messages,
                                    hasNext = res.prevBatch != null && res.messages.isNotEmpty()
                                )
                            })?.also {
                                // we just downloaded empty page, let's refresh UI to end paging
                                if(it.data.isEmpty()) {
                                    //TODO invalidateLocalSource()
                                }
                            }
                        }
                    },
                    getCount = {
                        certainMessageCount ?: conversationMessageDao.getCount(conversationId = conversationId).also {
                            certainMessageCount = it
                        }
                    },
                    size = config.pageSize
                ).also { pagingSource ->
                    currentPagingSource = pagingSource
                }
            }
        )
    }

    /** Returns a detailed information about a conversation */
    suspend fun getConversationDetail(
        conversationId: String,
        owner: String?
    ): ConversationRoomIO? {
        return withContext(Dispatchers.IO) {
            conversationRoomDao.getItem(conversationId, ownerPublicId = owner)?.apply {
                summary?.members = networkItemDao.getItems(
                    userPublicIds = summary?.heroes?.map { it.full },
                    ownerPublicId = ownerPublicId
                )
            }
        }
    }

    /** Sends a message to a conversation */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun sendMessage(
        conversationId: String,
        homeserver: String,
        message: ConversationMessageIO,
        audioByteArray: ByteArray? = null,
        gifAsset: GifAsset? = null,
        // TODO upload progress
        onProgressChange: ((MediaHttpProgress) -> Unit)? = null,
        mediaFiles: List<PlatformFile> = listOf(),
    ): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            val dataManager = KoinPlatform.getKoin().get<SharedDataManager>()
            val uuids = mutableListOf<String>()

            // placeholder/loading message preview
            var msg = message.copy(
                conversationId = conversationId,
                sentAt = localNow,
                authorPublicId = dataManager.currentUser.value?.matrixUserId,
                media = withContext(Dispatchers.Default) {
                    mediaFiles.map { media ->
                        MediaIO(
                            url = (Uuid.random().toString() + ".${media.extension.lowercase()}").also { uuid ->
                                cachedFiles[uuid] = media
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
            conversationMessageDao.insert(msg)
            invalidateLocalSource()

            // upload the final message
            val response = httpClient.safeRequest<Any> {
                post(
                    urlString = "/api/v1/social/conversation/send",
                    block = {
                        parameter("conversationId", conversationId)
                        setBody(msg)
                    }
                )
            }

            // real message
            msg = msg.copy(
                media = withContext(Dispatchers.Default) {
                    mediaFiles.mapNotNull { media ->
                        val bytes = media.readBytes()
                        if(bytes.isNotEmpty()) {
                            uploadMedia(
                                mediaByteArray = bytes,
                                fileName = "${Uuid.random()}.${media.extension.lowercase()}",
                                homeserver = homeserver,
                                mimetype = MimeType.getByExtension(media.extension).mime
                            )?.success?.data?.contentUri.takeIf { !it.isNullOrBlank() }?.let { url ->
                                MediaIO(
                                    url = url,
                                    size = bytes.size.toLong(),
                                    name = media.baseName,
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

                            uploadMedia(
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
                state = if(response is BaseResponse.Success) MessageState.Sent else MessageState.Failed
            )
            uuids.forEachIndexed { index, s ->
                msg.media?.getOrNull(index)?.url?.let {
                    cachedFiles[it] = cachedFiles[s]
                }
                cachedFiles.remove(s)
            }
            conversationMessageDao.insert(msg)
            invalidateLocalSource()

            response
        }
    }

    /** Marks a message as transcribed */
    suspend fun markMessageAsTranscribed(id: String) {
        withContext(Dispatchers.IO) {
            conversationMessageDao.transcribe(messageId = id, transcribed = true)
        }
    }

    /** Adds or changes reaction on a message */
    suspend fun reactToMessage(
        conversationId: String,
        reaction: MessageReactionRequest
    ): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            conversationMessageDao.get(reaction.messageId)?.let { message ->
                val dataManager = KoinPlatform.getKoin().get<SharedDataManager>()

                conversationMessageDao.insert(message.copy(
                    reactions = message.reactions.orEmpty().toMutableList().apply {
                        add(
                            MessageReactionIO(
                                content = reaction.content,
                                authorPublicId = dataManager.currentUser.value?.matrixUserId
                            )
                        )
                    }
                ))
            }
            invalidateLocalSource()

            httpClient.safeRequest<Any> {
                post(
                    urlString = "/api/v1/social/conversation/react",
                    block = {
                        parameter("conversationId", conversationId)
                        setBody(reaction)
                    }
                )
            }
        }
    }

    /** Returns configuration of the homeserver */
    suspend fun getMediaConfig(homeserver: String): BaseResponse<MediaRepositoryConfig> {
        return httpClient.safeRequest<MediaRepositoryConfig> {
            get(url = Url("https://$homeserver/_matrix/client/v1/media/config"))
        }
    }

    /**
     * Uploads media to the server
     * @return the server location URL
     */
    private suspend fun uploadMedia(
        mediaByteArray: ByteArray?,
        mimetype: String,
        homeserver: String,
        fileName: String
    ): BaseResponse<MediaUploadResponse>? = withContext(Dispatchers.IO) {
        try {
            if(mediaByteArray == null) null
            else {
                httpClient.safeRequest<MediaUploadResponse> {
                    post(url = Url("https://$homeserver/_matrix/media/v3/upload")) {
                        header(HttpHeaders.ContentType, mimetype)
                        parameter("filename", fileName)
                        setBody(mediaByteArray)
                    }
                }.also {
                    it.success?.success?.data?.contentUri?.let { uri ->
                        fileAccess.saveFileToCache(
                            data = mediaByteArray,
                            fileName = sha256(uri)
                        )?.let { path ->
                            mediaDataManager.cachedFiles.value = mediaDataManager.cachedFiles.value.toMutableMap().apply {
                                put(
                                    path.toString(),
                                    MediaIO(
                                        url = uri,
                                        mimetype = mimetype,
                                        size = mediaByteArray.size.toLong(),
                                        name = fileName
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

const val MESSAGE_AUDIO_URL_PLACEHOLDER = "audio_placeholder"

/** Attempts to upload a file to Firebase storage, and returns the download URL of the uploaded file. */
expect suspend fun uploadMediaToStorage(
    conversationId: String,
    byteArray: ByteArray,
    fileName: String
): String
