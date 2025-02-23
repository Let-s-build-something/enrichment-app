package ui.conversation

import androidx.paging.ExperimentalPagingApi
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
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.NetworkItemDao
import database.dao.matrix.MatrixPagingMetaDao
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import ui.conversation.MessagesRemoteMediator.Companion.INITIAL_BATCH
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
    private val pagingMetaDao: MatrixPagingMetaDao,
    private val networkItemDao: NetworkItemDao,
    private val mediaDataManager: MediaProcessorDataManager,
    private val fileAccess: FileAccess
) {
    protected var currentPagingSource: PagingSource<*, *>? = null
    val cachedFiles = hashMapOf<String, PlatformFile?>()

    /** Attempts to invalidate local PagingSource with conversation messages */
    private fun invalidateLocalSource() {
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
            if(conversationId == null || fromBatch.takeIf { it != INITIAL_BATCH } == null) {
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

    /** Returns a flow of conversation messages */
    @OptIn(ExperimentalPagingApi::class)
    fun getMessagesListFlow(
        homeserver: () -> String,
        config: PagingConfig,
        conversationId: String? = null
    ): Pager<String, ConversationMessageIO> {
        val scope = CoroutineScope(Dispatchers.Default)

        return Pager(
            config = config,
            pagingSourceFactory = {
                ConversationRoomSource(
                    getMessages = { batch ->
                        println("kostka_test, source getItems, batch: $batch, count: ${
                            conversationMessageDao.getCount(conversationId = conversationId, batch = batch)
                        }")

                        conversationMessageDao.getBatched(
                            conversationId = conversationId,
                            batch = batch
                        )
                    },
                    size = config.pageSize
                ).also { pagingSource ->
                    currentPagingSource = pagingSource
                }
            },
            remoteMediator = MessagesRemoteMediator(
                pagingMetaDao = pagingMetaDao,
                conversationMessageDao = conversationMessageDao,
                conversationId = conversationId ?: "",
                getItems = { batch ->
                    getMessages(
                        limit = config.pageSize,
                        conversationId = conversationId,
                        fromBatch = batch,
                        homeserver = homeserver()
                    )
                },
                prevBatch = {
                    conversationRoomDao.getPrevBatch(id = conversationId)
                },
                invalidatePagingSource = {
                    scope.coroutineContext.cancelChildren()
                    scope.launch {
                        delay(200)
                        currentPagingSource?.invalidate()
                    }
                }
            )
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
                    userPublicIds = summary?.heroes,
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
                media = mediaFiles.map { media ->
                    MediaIO(
                        url = (Uuid.random().toString() + ".${media.extension.lowercase()}").also { uuid ->
                            cachedFiles[uuid] = media
                            uuids.add(uuid)
                        }
                    )
                } + message.media.orEmpty() + MediaIO(
                    url = MESSAGE_AUDIO_URL_PLACEHOLDER
                ) + MediaIO(
                    url = gifAsset?.original,
                    mimetype = MimeType.IMAGE_GIF.mime,
                    name = gifAsset?.description
                ),
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
                media = mediaFiles.mapNotNull { media ->
                    val bytes = media.readBytes()
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
                }.plus(
                    // existing media and audio media
                    message.media.orEmpty().plus(
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
                                        listOf(
                                            MediaIO(
                                                url = audioUrl,
                                                size = size,
                                                name = fileName,
                                                mimetype = mimetype,
                                                path = it.toString()
                                            )
                                        )
                                    }
                                }else listOf()
                            } ?: listOf()
                        } else listOf()
                    )
                ).plus(
                    MediaIO(
                        url = gifAsset?.original,
                        mimetype = MimeType.IMAGE_GIF.mime,
                        name = gifAsset?.description
                    )
                ),
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
    ): BaseResponse<MediaUploadResponse>? {
        return try {
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
