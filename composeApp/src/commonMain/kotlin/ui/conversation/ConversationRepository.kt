package ui.conversation

import androidx.paging.Pager
import androidx.paging.PagingConfig
import augmy.interactive.shared.DateUtils.localNow
import augmy.interactive.shared.DateUtils.now
import data.io.base.BaseResponse
import data.io.base.PaginationInfo
import data.io.social.network.conversation.ConversationListResponse
import data.io.social.network.conversation.ConversationMessageIO
import data.io.social.network.conversation.MessageReactionIO
import data.io.social.network.conversation.MessageReactionRequest
import data.io.social.network.conversation.MessageState
import data.io.social.network.conversation.NetworkConversationIO
import data.io.user.NetworkItemIO
import data.shared.setPaging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import ui.login.safeRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Class for calling APIs and remote work in general */
class ConversationRepository(
    private val httpClient: HttpClient,
    val json: Json,
) {

    /** returns a list of network list */
    private suspend fun getMessages(
        page: Int,
        size: Int,
        conversationId: String?
    ): BaseResponse<ConversationListResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<ConversationListResponse> {
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
                BaseResponse.Success(ConversationListResponse(
                    content = demoMessages.subList(
                        page * size,
                        kotlin.math.min((page + 1) * size - 1, demoMessages.size - page * size)
                    ),
                    pagination = PaginationInfo(
                        page = page,
                        size = size,
                        totalPages = demoMessages.size/size
                    )
                ))
            }else BaseResponse.Error()
        }
    }

    /** Returns a flow of conversation messages */
    fun getMessagesListFlow(
        config: PagingConfig,
        conversationId: String? = null
    ): Pager<Int, ConversationMessageIO> {
        return Pager(config) {
            ConversationSource(
                getMessages = { page, size ->
                    getMessages(
                        page = page,
                        size = size,
                        conversationId = conversationId
                    )
                },
                size = config.pageSize
            )
        }
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
    suspend fun sendMessage(
        conversationId: String,
        message: ConversationMessageIO
    ): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<Any> {
                post(
                    urlString = "/api/v1/social/conversation/send",
                    block = {
                        parameter("conversationId", conversationId)
                        setBody(message)
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

    internal companion object {
        @OptIn(ExperimentalUuidApi::class)
        val demoMessages = listOf(
            ConversationMessageIO(
                content = "Yo",
                id = Uuid.random().toString(),
                authorPublicId = "1",
                createdAt = LocalDateTime.parse("2010-06-01T22:19:44")
            ),
            ConversationMessageIO(
                content = "Yoddaaa \uD83D\uDFE2\uD83D\uDFE2",
                id = Uuid.random().toString(),
                authorPublicId = "2de6d3d4-606b-4100-8e2a-8611ceb30db0",
                createdAt = LocalDateTime.parse(
                    now.minus(24, DateTimeUnit.HOUR)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(LocalDateTime.Formats.ISO)
                ),
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
                content = "You are visibly excited! You are visibly excited!",
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
        ).sortedByDescending { it.createdAt }

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