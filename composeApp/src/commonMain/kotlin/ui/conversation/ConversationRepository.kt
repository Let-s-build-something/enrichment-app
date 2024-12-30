package ui.conversation

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import augmy.interactive.shared.DateUtils.localNow
import augmy.interactive.shared.DateUtils.now
import data.io.base.BaseResponse
import data.io.base.PaginationInfo
import data.io.social.network.conversation.ConversationMessageIO
import data.io.social.network.conversation.ConversationMessagesResponse
import data.io.social.network.conversation.MessageReactionIO
import data.io.social.network.conversation.MessageReactionRequest
import data.io.social.network.conversation.MessageState
import data.io.social.network.conversation.NetworkConversationIO
import data.io.user.NetworkItemIO
import data.shared.SharedDataManager
import data.shared.setPaging
import database.dao.ConversationMessageDao
import database.dao.PagingMetaDao
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.extension
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
import ui.login.safeRequest
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Class for calling APIs and remote work in general */
class ConversationRepository(
    private val httpClient: HttpClient,
    private val conversationMessageDao: ConversationMessageDao,
    private val pagingMetaDao: PagingMetaDao
) {
    private var currentPagingSource: ConversationRoomSource? = null
    val cachedByteArrays = hashMapOf<String, PlatformFile>()

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

            if(page <= demoMessages.size/size) {
                BaseResponse.Success(
                    ConversationMessagesResponse(
                        content = demoMessages.subList(
                            page * size,
                            kotlin.math.min(
                                (page + 1) * size,
                                page * size + (demoMessages.size - page * size)
                            ).coerceAtMost(demoMessages.size)
                        ),
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
            remoteMediator = ConversationRemoteMediator(
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
        mediaFiles: List<PlatformFile> = listOf(),
    ): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            val dataManager = KoinPlatform.getKoin().get<SharedDataManager>()
            val uuids = mutableListOf<String>()
            var msg = message.copy(
                conversationId = conversationId,
                createdAt = localNow,
                authorPublicId = dataManager.currentUser.value?.publicId,
                mediaUrls = if(mediaFiles.isNotEmpty()) {
                    mediaFiles.map { media ->
                        (Uuid.random().toString() + ".${media.extension.lowercase()}").also { uuid ->
                            cachedByteArrays[uuid] = media
                            uuids.add(uuid)
                        }
                    }
                }else message.mediaUrls
            )
            // loading message preview
            conversationMessageDao.insert(msg)
            invalidateLocalSource()

            // real message preview
            msg = msg.copy(
                mediaUrls = mediaFiles.mapNotNull { media ->
                    val bytes = media.readBytes()
                    requestMediaUpload(
                        mediaByteArray = bytes,
                        fileName = "${Uuid.random()}.${media.extension.lowercase()}",
                        conversationId = conversationId
                    ).takeIf { !it.isNullOrBlank() }
                },
                audioUrl = requestMediaUpload(
                    mediaByteArray = audioByteArray,
                    fileName = "${Uuid.random()}.wav",
                    conversationId = conversationId
                )
            )
            uuids.forEach {
                cachedByteArrays.remove(it)
            }
            conversationMessageDao.insert(msg)
            invalidateLocalSource()

            // upload the real message
            httpClient.safeRequest<Any> {
                post(
                    urlString = "/api/v1/social/conversation/send",
                    block = {
                        parameter("conversationId", conversationId)
                        setBody(msg)
                    }
                )
            }
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
                        add(MessageReactionIO(
                            content = reaction.content,
                            authorPublicId = dataManager.currentUser.value?.publicId
                        ))
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

    /** Makes a request to change user's profile picture */
    private suspend fun requestMediaUpload(
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
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.RECEIVED
            ),
            ConversationMessageIO(
                content = "Yes! It was so intense! 😱",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(content = "❤️", authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0")
                ),
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "I couldn't believe the twist at the end! 🤯",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.SENT
            ),
            ConversationMessageIO(
                content = "Me neither! Any theories for next week?",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "I think the protagonist might switch sides... 😮",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                reactions = listOf(
                    MessageReactionIO(content = "👍", authorPublicId = "1"),
                    MessageReactionIO(content = "🔥", authorPublicId = "1")
                ),
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.RECEIVED
            ),
            ConversationMessageIO(
                content = "That would be wild! I can't wait! 🚀",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "By the way, are we still on for dinner tomorrow? 🍲",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.SENT
            ),
            ConversationMessageIO(
                content = "Absolutely! Looking forward to it! 😊",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(content = "❤️", authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0")
                ),
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "Do you think we should invite more friends? 🤔",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.SENT
            ),
            ConversationMessageIO(
                content = "Sure! The more, the merrier! 😄",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "I'll check with Sarah and Jake. 🌟",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.SENT
            ),
            ConversationMessageIO(
                content = "Sounds great. Let me know what they say! 📞",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "Sarah is in, but Jake is busy. 🤷‍♂️",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.SENT
            ),
            ConversationMessageIO(
                content = "Got it! I'll plan accordingly. 😊",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "Any food preferences for tomorrow? 🍝",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.SENT
            ),
            ConversationMessageIO(
                content = "I'm good with anything! Just no peanuts, please. 🥜",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse(
                    now.minus(26, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "Yoddaaa \uD83D\uDFE2\uD83D\uDFE2",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(25, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
            ),
            ConversationMessageIO(
                content = "Did you catch the latest episode? 🤔",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(24, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.RECEIVED
            ),
            ConversationMessageIO(
                content = "Yes! It was so intense! 😱",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(content = "❤️", authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0")
                ),
                createdAt = LocalDateTime.parse(
                    now.minus(23, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "I couldn't believe the twist at the end! 🤯",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(22, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.SENT
            ),
            ConversationMessageIO(
                content = "Me neither! Any theories for next week?",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse(
                    now.minus(20, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "I think the protagonist might switch sides... 😮",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                reactions = listOf(
                    MessageReactionIO(content = "👍", authorPublicId = "1"),
                    MessageReactionIO(content = "🔥", authorPublicId = "1")
                ),
                createdAt = LocalDateTime.parse(
                    now.minus(18, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.RECEIVED
            ),
            ConversationMessageIO(
                content = "That would be wild! I can't wait! 🚀",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse(
                    now.minus(17, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "By the way, are we still on for dinner tomorrow? 🍲",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(16, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.SENT
            ),
            ConversationMessageIO(
                content = "Absolutely! Looking forward to it! 😊",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(content = "❤️", authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0")
                ),
                createdAt = LocalDateTime.parse(
                    now.minus(15, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "How are you? \uD83D\uDC40",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(8, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
            ),
            ConversationMessageIO(
                content = "You are visibly excited!",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(
                        content = "\uD83D\uDE2E", authorPublicId = "1"
                    )
                ),
                createdAt = LocalDateTime.parse(
                    now.minus(386, DateTimeUnit.SECOND)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "Maybe a success of sorts? ☺",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                reactions = listOf(
                    MessageReactionIO(
                        content = "\uD83D\uDC40",
                        authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0"
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
                createdAt = LocalDateTime.parse(
                    now.minus(4, DateTimeUnit.MINUTE)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "I'm great. What about yourself?",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse(
                    now.minus(3, DateTimeUnit.MINUTE)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.READ
            ),
            ConversationMessageIO(
                content = "You bet! We've just won! ⚽⚽\uD83C\uDFC6\uD83C\uDFC5",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(2, DateTimeUnit.MINUTE)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.RECEIVED,
                reactions = listOf(
                    MessageReactionIO(
                        content = "\uD83E\uDD73",
                        authorPublicId = "1"
                    )
                ),
            ),
            ConversationMessageIO(
                content = "That's amazing, I'm so excited for you! \uD83E\uDD73",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse(
                    now.minus(1, DateTimeUnit.MINUTE)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.FAILED
            ),
            ConversationMessageIO(
                content = "I can tell! Thank you ❤\uFE0F",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    localNow.format(LocalDateTime.Formats.ISO)
                ),
                state = MessageState.SENT
            )
        ).apply {
            repeat(100) { index ->
                add(
                    ConversationMessageIO(
                        content = "Yo n.$index",
                        id = Uuid.random().toString(),
                        authorPublicId = "1",
                        createdAt = LocalDateTime.parse("2023-12-10T22:19:44")
                    )
                )
            }
        }.sortedByDescending { it.createdAt }

        val demoConversationDetail = NetworkConversationIO(
            pictureUrl = "https://picsum.photos/102",
            publicId = "public_id",
            tag = "65f681",
            lastMessage = "Last message",
            users = listOf(
                NetworkItemIO(
                    publicId = "1",
                    displayName = "John Doe",
                    photoUrl = "https://picsum.photos/106"
                ),
                NetworkItemIO(
                    publicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                    displayName = "Hey! That's me:o",
                    photoUrl = "https://picsum.photos/101"
                ),
            )
        )
    }
}

/** Attempts to upload a file to Firebase storage, and returns the download URL of the uploaded file. */
expect suspend fun uploadMediaToStorage(
    conversationId: String,
    byteArray: ByteArray,
    fileName: String
): String
