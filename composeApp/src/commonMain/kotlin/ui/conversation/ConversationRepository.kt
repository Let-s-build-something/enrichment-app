package ui.conversation

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import augmy.interactive.shared.utils.DateUtils.localNow
import base.utils.sha256
import data.io.base.BaseResponse
import data.io.base.PaginationInfo
import data.io.social.network.conversation.MessageReactionRequest
import data.io.social.network.conversation.NetworkConversationIO
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.ConversationMessagesResponse
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageReactionIO
import data.io.social.network.conversation.message.MessageState
import data.shared.SharedDataManager
import data.shared.setPaging
import database.dao.ConversationMessageDao
import database.dao.PagingMetaDao
import database.file.FileAccess
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.baseName
import io.github.vinceglb.filekit.core.extension
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import korlibs.io.net.MimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import ui.conversation.components.audio.MediaHttpProgress
import ui.login.safeRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Class for calling APIs and remote work in general */
class ConversationRepository(
    private val httpClient: HttpClient,
    private val conversationMessageDao: ConversationMessageDao,
    private val pagingMetaDao: PagingMetaDao,
    private val fileAccess: FileAccess
) {
    private var currentPagingSource: ConversationRoomSource? = null
    val cachedFiles = hashMapOf<String, PlatformFile?>()

    /** Attempts to invalidate local PagingSource with conversation messages */
    private fun invalidateLocalSource() {
        currentPagingSource?.invalidate()
    }

    /** returns a list of network list */
    private suspend fun getMessages(
        page: Int,
        size: Int,
        conversationId: String?
    ): BaseResponse<ConversationMessagesResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<ConversationMessagesResponse> {
                get(
                    urlString = "/api/v1/social/conversation",
                    block =  {
                        setPaging(
                            size = size,
                            page = page
                        ) {
                            append("conversationId", conversationId.orEmpty())
                        }
                    }
                )
            }
        }
    }

    /** Returns a flow of conversation messages */
    @OptIn(ExperimentalPagingApi::class)
    fun getMessagesListFlow(
        config: PagingConfig,
        conversationId: String? = null,
    ): Pager<Int, ConversationMessageIO> {
        val scope = CoroutineScope(Dispatchers.Default)

        return Pager(
            config = config,
            pagingSourceFactory = {
                ConversationRoomSource(
                    getMessages = { page ->
                        val res = conversationMessageDao.getPaginated(
                            conversationId = conversationId,
                            limit = config.pageSize,
                            offset = page * config.pageSize
                        )

                        BaseResponse.Success(
                            ConversationMessagesResponse(
                                content = res,
                                pagination = PaginationInfo(
                                    page = page,
                                    size = res.size,
                                    totalItems = conversationMessageDao.getCount(conversationId)
                                )
                            )
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
                size = config.pageSize,
                conversationId = conversationId ?: "",
                getItems = { page ->
                    getMessages(page = page, size = config.pageSize, conversationId = conversationId)
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
    suspend fun getConversationDetail(conversationId: String): BaseResponse<NetworkConversationIO?> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<NetworkConversationIO> {
                get(
                    urlString = "/api/v1/social/conversation/detail",
                    block = {
                        parameter("conversationId", conversationId)
                    }
                )
            }
        }
    }

    /** Sends a message to a conversation */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun sendMessage(
        conversationId: String,
        message: ConversationMessageIO,
        audioByteArray: ByteArray? = null,
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
                authorPublicId = dataManager.currentUser.value?.publicId,
                media = mediaFiles.map { media ->
                    MediaIO(
                        url = (Uuid.random().toString() + ".${media.extension.lowercase()}").also { uuid ->
                            cachedFiles[uuid] = media
                            uuids.add(uuid)
                        }
                    )
                } + message.media.orEmpty(),
                audioUrl = if(audioByteArray?.isNotEmpty() == true) {
                    MESSAGE_AUDIO_URL_PLACEHOLDER
                }else null,
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
                        conversationId = conversationId
                    ).takeIf { !it.isNullOrBlank() }?.let { url ->
                        MediaIO(
                            url = url,
                            size = bytes.size,
                            name = media.baseName,
                            mimetype = MimeType.getByExtension(media.extension).mime
                        )
                    }
                } + message.media.orEmpty(),
                audioUrl = uploadMedia(
                    mediaByteArray = audioByteArray,
                    fileName = "${Uuid.random()}.wav",
                    conversationId = conversationId
                ).also { audioUrl ->
                    if(!audioUrl.isNullOrBlank()) {
                        msg = msg.copy(audioUrl = audioUrl)
                        audioByteArray?.let { data ->
                            fileAccess.saveFileToCache(data = data, fileName = sha256(audioUrl))
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
                            authorPublicId = dataManager.currentUser.value?.publicId
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

    /**
     * Uploads media to the server
     * @return the server location URL
     */
    private suspend fun uploadMedia(
        conversationId: String,
        mediaByteArray: ByteArray?,
        fileName: String
    ): String? {
        return try {
            if(mediaByteArray == null) null
            else uploadMediaToStorage(
                conversationId = conversationId,
                byteArray = mediaByteArray,
                fileName = fileName
            )
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
