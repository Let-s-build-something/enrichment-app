package ui.conversation

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import augmy.interactive.shared.DateUtils.localNow
import augmy.interactive.shared.DateUtils.now
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
import data.io.user.NetworkItemIO
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.koin.mp.KoinPlatform
import ui.conversation.components.audio.MediaHttpProgress
import ui.login.safeRequest
import kotlin.math.min
import kotlin.math.roundToInt
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

            val dataManager = KoinPlatform.getKoin().get<SharedDataManager>()
            if(page <= demoMessages.size/size) {
                BaseResponse.Success(
                    ConversationMessagesResponse(
                        content = demoMessages.subList(
                            page * size,
                            min(
                                (page + 1) * size,
                                page * size + (demoMessages.size - page * size)
                            ).coerceAtMost(demoMessages.size)
                        ).map { it.copy(authorPublicId = if(it.authorPublicId == "me") dataManager.currentUser.value?.publicId else it.authorPublicId) },
                        pagination = PaginationInfo(
                            page = page,
                            size = size,
                            totalPages = (demoMessages.size/size.toFloat()).roundToInt()
                        )
                    )
                )
            }else BaseResponse.Error()
        }
    }

    /** Returns a flow of conversation messages */
    @OptIn(ExperimentalPagingApi::class)
    fun getMessagesListFlow(
        config: PagingConfig,
        conversationId: String? = null
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

            // upload the real message
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

    internal companion object {
        @OptIn(ExperimentalUuidApi::class)
        val demoMessages = mutableListOf(
            ConversationMessageIO(
                content = "Did you catch the latest episode? 🤔",
                id = "0",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Received
            ),
            ConversationMessageIO(
                content = "Yes! It was so intense! 😱",
                id = "1",
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(content = "❤️", authorPublicId = "me")
                ),
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "I couldn't believe the twist at the end! 🤯",
                id = "2",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Sent
            ),
            ConversationMessageIO(
                content = "Me neither! Any theories for next week?",
                id = "3",
                authorPublicId = "1",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "I think the protagonist might switch sides... 😮",
                id = "3",
                authorPublicId = "me",
                reactions = listOf(
                    MessageReactionIO(content = "👍", authorPublicId = "1"),
                    MessageReactionIO(content = "🔥", authorPublicId = "1")
                ),
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Received
            ),
            ConversationMessageIO(
                content = "That would be wild! I can't wait! 🚀",
                id = "4",
                authorPublicId = "1",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "By the way, are we still on for dinner tomorrow? 🍲",
                id = "5",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Sent
            ),
            ConversationMessageIO(
                content = "Absolutely! Looking forward to it! 😊",
                id = "6",
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(content = "❤️", authorPublicId = "me")
                ),
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "Do you think we should invite more friends? 🤔",
                id = "7",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Sent
            ),
            ConversationMessageIO(
                content = "Sure! The more, the merrier! 😄",
                id = "8",
                authorPublicId = "1",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "I'll check with Sarah and Jake. 🌟",
                id = "9",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Sent
            ),
            ConversationMessageIO(
                content = "Sounds great. Let me know what they say! 📞",
                id = "10",
                authorPublicId = "1",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "Sarah is in, but Jake is busy. 🤷‍♂️",
                id = "11",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Sent
            ),
            ConversationMessageIO(
                content = "Got it! I'll plan accordingly. 😊",
                id = "12",
                authorPublicId = "1",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "Any food preferences for tomorrow? 🍝",
                id = "13",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Sent
            ),
            ConversationMessageIO(
                content = "I'm good with anything! Just no peanuts, please. 🥜",
                id = "14",
                authorPublicId = "1",
                sentAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "Yoddaaa \uD83D\uDFE2\uD83D\uDFE2",
                id = "15",
                authorPublicId = "me",
                state = MessageState.Read,
                sentAt = LocalDateTime.parse(
                    now.minus(25, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
            ),
            ConversationMessageIO(
                content = "Did you catch the latest episode? 🤔",
                id = "16",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(24, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Received
            ),
            ConversationMessageIO(
                content = "Yes! It was so intense! 😱",
                id = "17",
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(content = "❤️", authorPublicId = "me")
                ),
                sentAt = LocalDateTime.parse(
                    now.minus(23, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "I couldn't believe the twist at the end! 🤯",
                id = "18",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(22, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Sent
            ),
            ConversationMessageIO(
                content = "Me neither! Any theories for next week?",
                id = "19",
                authorPublicId = "1",
                sentAt = LocalDateTime.parse(
                    now.minus(20, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "I think the protagonist might switch sides... 😮",
                id = "20",
                authorPublicId = "me",
                reactions = listOf(
                    MessageReactionIO(content = "👍", authorPublicId = "1"),
                    MessageReactionIO(content = "🔥", authorPublicId = "1")
                ),
                sentAt = LocalDateTime.parse(
                    now.minus(18, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Received
            ),
            ConversationMessageIO(
                content = "That would be wild! I can't wait! 🚀",
                id = "21",
                authorPublicId = "1",
                sentAt = LocalDateTime.parse(
                    now.minus(17, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "By the way, are we still on for dinner tomorrow? 🍲",
                id = "22",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(16, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Sent
            ),
            ConversationMessageIO(
                content = "Absolutely! Looking forward to it! 😊",
                id = "23",
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(content = "❤️", authorPublicId = "me")
                ),
                sentAt = LocalDateTime.parse(
                    now.minus(15, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "How are you? \uD83D\uDC40",
                id = "24",
                state = MessageState.Failed,
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(8, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
            ),
            ConversationMessageIO(
                content = "You are visibly excited!",
                id = "25",
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(
                        content = "\uD83D\uDE2E", authorPublicId = "1"
                    )
                ),
                sentAt = LocalDateTime.parse(
                    now.minus(386, DateTimeUnit.SECOND)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "Maybe a success of sorts? ☺",
                id = "26",
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(
                        content = "\uD83D\uDC40",
                        authorPublicId = "me"
                    ),
                    MessageReactionIO(content = "\uD83C\uDFC6", authorPublicId = "1"),
                    MessageReactionIO(content = "\uD83D\uDC40", authorPublicId = "1"),
                    MessageReactionIO(content = "\uD83E\uDD73", authorPublicId = "1"),
                    MessageReactionIO(content = "⚽", authorPublicId = "1"),
                    MessageReactionIO(content = "\uD83E\uDD73", authorPublicId = "1"),
                    MessageReactionIO(content = "\uD83E\uDD73", authorPublicId = "1"),
                    MessageReactionIO(content = "⚽", authorPublicId = "1"),
                    MessageReactionIO(content = "\uD83C\uDFC5", authorPublicId = "1"),
                    MessageReactionIO(content = "\uD83E\uDD73", authorPublicId = "1"),
                ),
                sentAt = LocalDateTime.parse(
                    now.minus(4, DateTimeUnit.MINUTE)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "I'm great. What about yourself?" +
                        "\nLook at this! https://github.com/fleeksoft/ksoup",
                id = "28",
                authorPublicId = "1",
                sentAt = LocalDateTime.parse(
                    now.minus(3, DateTimeUnit.MINUTE)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Read
            ),
            ConversationMessageIO(
                content = "You bet! We've just won! ⚽⚽\uD83C\uDFC6\uD83C\uDFC5",
                id = "29",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    now.minus(2, DateTimeUnit.MINUTE)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Received,
                reactions = listOf(
                    MessageReactionIO(
                        content = "\uD83E\uDD73",
                        authorPublicId = "1"
                    )
                ),
            ),
            ConversationMessageIO(
                content = "That's amazing, I'm so excited for you! \uD83E\uDD73",
                id = "30",
                authorPublicId = "1",
                sentAt = LocalDateTime.parse(
                    now.minus(1, DateTimeUnit.MINUTE)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Failed
            ),
            ConversationMessageIO(
                content = "I can tell! Thank you ❤\uFE0F",
                id = "31",
                authorPublicId = "me",
                sentAt = LocalDateTime.parse(
                    localNow.format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.Sent
            )
        ).apply {
            repeat(100) { index ->
                add(
                    ConversationMessageIO(
                        content = "Yo n.$index",
                        id = "32$index",
                        authorPublicId = "1",
                        sentAt = LocalDateTime.parse("2023-12-10T22:19:44")
                    )
                )
            }
        }.sortedByDescending { it.sentAt }

        val demoConversationDetail = NetworkConversationIO(
            pictureUrl = "https://picsum.photos/102",
            publicId = "public_id",
            tag = "65f681",
            lastMessage = "Last message",
            users = listOf(
                NetworkItemIO(
                    publicId = "1",
                    name = "John Doe",
                    photoUrl = "https://picsum.photos/106"
                ),
                NetworkItemIO(
                    publicId = "me",
                    name = "Hey! That's me:o",
                    photoUrl = "https://picsum.photos/101"
                ),
            )
        )
    }
}

const val MESSAGE_AUDIO_URL_PLACEHOLDER = "audio_placeholder"

/** Attempts to upload a file to Firebase storage, and returns the download URL of the uploaded file. */
expect suspend fun uploadMediaToStorage(
    conversationId: String,
    byteArray: ByteArray,
    fileName: String
): String
