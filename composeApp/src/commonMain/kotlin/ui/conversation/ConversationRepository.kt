package ui.conversation

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import base.utils.MediaType
import base.utils.getMediaType
import base.utils.sha256
import data.io.base.BaseResponse
import data.io.matrix.media.FileList
import data.io.matrix.media.MediaRepositoryConfig
import data.io.matrix.media.MediaUploadResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.event.ConversationTypingIndicator
import data.io.social.network.conversation.MessageReactionRequest
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.ConversationMessagesResponse
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageReactionIO
import data.shared.SharedDataManager
import data.shared.sync.DataSyncHandler
import data.shared.sync.MessageProcessor
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.NetworkItemDao
import database.file.FileAccess
import io.github.vinceglb.filekit.core.PlatformFile
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.encrypt
import net.folivo.trixnity.client.roomEventEncryptionServices
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.AudioInfo
import net.folivo.trixnity.core.model.events.m.room.FileInfo
import net.folivo.trixnity.core.model.events.m.room.ImageInfo
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.mp.KoinPlatform
import ui.conversation.components.audio.MediaProcessorDataManager
import ui.login.safeRequest

/** Class for calling APIs and remote work in general */
open class ConversationRepository(
    private val httpClient: HttpClient,
    private val conversationMessageDao: ConversationMessageDao,
    internal val conversationRoomDao: ConversationRoomDao,
    private val networkItemDao: NetworkItemDao,
    private val mediaDataManager: MediaProcessorDataManager,
    private val fileAccess: FileAccess
) {
    private val sharedDataManager by lazy { KoinPlatform.getKoin().get<SharedDataManager>() }

    private val dataSyncHandler = DataSyncHandler()
    protected var currentPagingSource: PagingSource<*, *>? = null
    val cachedFiles = MutableStateFlow(hashMapOf<String, PlatformFile?>())
    protected var certainMessageCount: Int? = null

    /** Attempts to invalidate local PagingSource with conversation messages */
    fun invalidateLocalSource() {
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

                            conversationMessageDao.getPaginated(
                                conversationId = conversationId,
                                limit = config.pageSize,
                                offset = page * config.pageSize
                            ).let { res ->
                                if(res.isNotEmpty()) {
                                    GetMessagesResponse(
                                        data = res,
                                        hasNext = res.size == config.pageSize || prevBatch != null
                                    )
                                }else null
                            } ?: if(prevBatch != null) {
                                getAndStoreNewMessages(
                                    limit = config.pageSize,
                                    conversationId = conversationId,
                                    fromBatch = prevBatch,
                                    homeserver = homeserver()
                                )?.let { res ->
                                    certainMessageCount = certainMessageCount?.plus(res.messages.size)
                                    GetMessagesResponse(
                                        data = res.messages,
                                        hasNext = res.prevBatch != null && res.messages.isNotEmpty()
                                    ).also {
                                        val newPrevBatch = if(res.messages.isEmpty()
                                            && res.events == 0
                                            && res.members == 0
                                        )null else res.prevBatch

                                        conversationRoomDao.setPrevBatch(
                                            id = conversationId,
                                            prevBatch = newPrevBatch
                                        )
                                        if(newPrevBatch == null) {
                                            // we just downloaded empty page, let's refresh UI to end paging
                                            invalidateLocalSource()
                                        }
                                    }
                                }
                            }else GetMessagesResponse(data = emptyList(), hasNext = false)
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
    ): ConversationRoomIO? = withContext(Dispatchers.IO) {
        conversationRoomDao.getItem(conversationId, ownerPublicId = owner)?.apply {
            summary?.members = networkItemDao.getItems(
                userPublicIds = summary?.heroes?.map { it.full },
                ownerPublicId = ownerPublicId
            )
        }
    }

    /** Returns a detailed information about a conversation */
    suspend fun updateTypingIndicator(
        conversationId: String,
        indicator: ConversationTypingIndicator
    ): BaseResponse<Any> = withContext(Dispatchers.IO) {
        val userId = sharedDataManager.currentUser.value?.matrixUserId
        val homeserver = sharedDataManager.currentUser.value?.matrixHomeserver

        httpClient.safeRequest<Any> {
            put("https://${homeserver}/_matrix/client/v3/rooms/${conversationId}/typing/${userId}") {
                setBody(indicator)
            }
        }
    }

    suspend fun sendMessage(
        client: MatrixClient?,
        conversationId: String,
        message: ConversationMessageIO
    ): Result<EventId>? = withContext(Dispatchers.IO) {
        val roomId = RoomId(conversationId)
        val eventContent = when {
            message.media?.size == 1 -> {
                val media = message.media.firstOrNull()
                when(getMediaType(media?.mimetype ?: "")) {
                    MediaType.AUDIO -> RoomMessageEventContent.FileBased.Audio(
                        body = message.content ?: "",
                        url = media?.url,
                        fileName = media?.name,
                        info = AudioInfo(
                            mimeType = media?.mimetype,
                            size = media?.size
                        ),
                        relatesTo = message.relatesTo()
                    )
                    MediaType.GIF, MediaType.IMAGE -> RoomMessageEventContent.FileBased.Image(
                        body = message.content ?: "",
                        url = media?.url,
                        fileName = media?.name,
                        info = ImageInfo(
                            mimeType = media?.mimetype,
                            size = media?.size
                        ),
                        relatesTo = message.relatesTo()
                    )
                    else -> RoomMessageEventContent.FileBased.File(
                        body = message.content ?: "",
                        url = media?.url,
                        fileName = media?.name,
                        info = FileInfo(
                            mimeType = media?.mimetype,
                            size = media?.size
                        ),
                        relatesTo = message.relatesTo()
                    )
                }
            }
            (message.media?.size ?: 0) > 1 -> FileList(
                body = message.content ?: "",
                urls = message.media?.mapNotNull { it.url },
                fileName = message.media?.firstOrNull()?.name,
                infos = message.media?.map { media ->
                    FileInfo(
                        mimeType = media.mimetype,
                        size = media.size
                    )
                },
                relatesTo = message.relatesTo()
            )
            else -> RoomMessageEventContent.TextBased.Text(
                body = message.content ?: "",
                relatesTo = message.relatesTo()
            )
        }

        client?.api?.room?.sendMessageEvent(
            roomId = roomId,
            eventContent = client.roomEventEncryptionServices.encrypt(
                content = eventContent,
                roomId = roomId
            )?.getOrNull() ?: eventContent
        )
    }

    suspend fun cacheMessage(message: ConversationMessageIO) = withContext(Dispatchers.IO) {
        conversationMessageDao.insertReplace(message)
    }

    suspend fun removeMessage(id: String) = withContext(Dispatchers.IO) {
        conversationMessageDao.remove(id)
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

                conversationMessageDao.insertReplace(message.copy(
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
    suspend fun uploadMedia(
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
