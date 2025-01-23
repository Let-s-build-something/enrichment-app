package data.shared

import augmy.interactive.shared.DateUtils.localNow
import augmy.interactive.shared.DateUtils.now
import data.io.social.network.conversation.NetworkConversationIO
import data.io.social.network.conversation.matrix.ConversationRoomIO
import data.io.social.network.conversation.matrix.MatrixEventContent
import data.io.social.network.conversation.matrix.RoomNotificationsCount
import data.io.social.network.conversation.matrix.RoomSummary
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MessageReactionIO
import data.io.social.network.conversation.message.MessageState
import data.io.user.NetworkItemIO
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.ExperimentalUuidApi

internal object DemoData {
    val demoRooms = listOf(
        ConversationRoomIO(
            id = "1",
            summary = RoomSummary(
                heroes = listOf("1"),
                lastMessage = MatrixEventContent.RoomMessageEvent(body = "Hey, what's up?"),
                joinedMemberCount = 2
            ),
            proximity = 5f
        ),
        ConversationRoomIO(
            id = "2",
            unreadNotifications = RoomNotificationsCount(highlightCount = 2),
            summary = RoomSummary(
                heroes = listOf("2"),
                canonicalAlias = "Gamer's room",
                lastMessage = MatrixEventContent.RoomMessageEvent(body = "That's terrible:D"),
                joinedMemberCount = 2
            ),
            proximity = 2f
        )
    )


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