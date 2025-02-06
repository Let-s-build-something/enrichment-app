package data.shared

import augmy.interactive.shared.utils.DateUtils.localNow
import augmy.interactive.shared.utils.DateUtils.now
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.MatrixEventContent
import data.io.matrix.room.RoomNotificationsCount
import data.io.matrix.room.RoomSummary
import data.io.social.network.conversation.NetworkConversationIO
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
import kotlin.uuid.Uuid

internal object DemoData {
    @OptIn(ExperimentalUuidApi::class)
    val newRoomId: String
        get() = Uuid.random().toString()

    val demoRooms = listOf(
        ConversationRoomIO(
            id = "1",
            summary = RoomSummary(
                heroes = listOf("1"),
                canonicalAlias = "Jack'omygod",
                lastMessage = MatrixEventContent(body = "Hey, what's up?"),
                joinedMemberCount = 2,
                avatarUrl = "https://wallpapers.com/images/hd/cool-xbox-profile-pictures-9dtcc745il694rjs.jpg",
                isDirect = true
            ),
            proximity = 5f
        ),
        ConversationRoomIO(
            id = "2",
            unreadNotifications = RoomNotificationsCount(highlightCount = 2),
            summary = RoomSummary(
                heroes = listOf("2"),
                canonicalAlias = "Gamer's room",
                lastMessage = MatrixEventContent(body = "That's terrible:D"),
                joinedMemberCount = 2,
                avatarUrl = "https://sguru.org/wp-content/uploads/2017/06/cool-gaming-profile-pictures-youtube_profile.jpg",
                isDirect = false
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

    @OptIn(ExperimentalUuidApi::class)
    private val family = listOf(
        NetworkItemIO(proximity = 10.9f, name = "Sister", photoUrl = "https://picsum.photos/102", tag = "2098d6", userPublicId = Uuid.random().toString(), userMatrixId = "1"),
        NetworkItemIO(proximity = 10.8f, name = "Son", photoUrl = "https://picsum.photos/104", tag = "2098d6", userPublicId = Uuid.random().toString(), userMatrixId = "2"),
        NetworkItemIO(proximity = 10.7f, name = "Mom", photoUrl = "https://picsum.photos/101", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 10.4f, name = "Brother", photoUrl = "https://picsum.photos/103", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 10.2f, name = "Grandma", photoUrl = "https://picsum.photos/105", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 10.15f, name = "Dad", photoUrl = "https://picsum.photos/100", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 10.1f, name = "Grandpa", photoUrl = "https://picsum.photos/106", tag = "2098d6", userPublicId = Uuid.random().toString())
    )

    @OptIn(ExperimentalUuidApi::class)
    private val friends = listOf(
        NetworkItemIO(proximity = 9.9f, name = "Jack", photoUrl = "https://picsum.photos/107", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 9.3f, name = "Peter", photoUrl = "https://picsum.photos/108", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 9.2f, name = "James", photoUrl = "https://picsum.photos/109", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 9.6f, name = "Mark", photoUrl = "https://picsum.photos/110", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 9.8f, name = "Carl", photoUrl = "https://picsum.photos/111", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 9.1f, name = "Arnold", photoUrl = "https://picsum.photos/112", tag = "2098d6", userPublicId = Uuid.random().toString()),
    )

    @OptIn(ExperimentalUuidApi::class)
    private val acquaintances = listOf(
        NetworkItemIO(proximity = 8.5f, name = "Jack", photoUrl = "https://picsum.photos/113", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.3f, name = "Peter", photoUrl = "https://picsum.photos/114", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.77f, name = "James", photoUrl = "https://picsum.photos/115", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.7f, name = "Mark", photoUrl = "https://picsum.photos/116", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.8f, name = "Carl", photoUrl = "https://picsum.photos/117", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.2f, name = "Arnold", photoUrl = "https://picsum.photos/118", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.9f, name = "Helen", photoUrl = "https://picsum.photos/119", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.4f, name = "Linda", photoUrl = "https://picsum.photos/120", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.6f, name = "Susan", photoUrl = "https://picsum.photos/121", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.75f, name = "Betty", photoUrl = "https://picsum.photos/122", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.65f, name = "Nancy", photoUrl = "https://picsum.photos/123", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.45f, name = "George", photoUrl = "https://picsum.photos/124", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.5f, name = "Paul", photoUrl = "https://picsum.photos/125", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.55f, name = "Ruth", photoUrl = "https://picsum.photos/126", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.6f, name = "Tom", photoUrl = "https://picsum.photos/127", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.68f, name = "Eve", photoUrl = "https://picsum.photos/128", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.58f, name = "Chris", photoUrl = "https://picsum.photos/129", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.52f, name = "Steve", photoUrl = "https://picsum.photos/130", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.9f, name = "Sarah", photoUrl = "https://picsum.photos/131", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.78f, name = "Laura", photoUrl = "https://picsum.photos/132", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.72f, name = "Michael", photoUrl = "https://picsum.photos/133", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.76f, name = "Jessica", photoUrl = "https://picsum.photos/134", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.64f, name = "Daniel", photoUrl = "https://picsum.photos/135", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.66f, name = "Emma", photoUrl = "https://picsum.photos/136", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.8f, name = "Olivia", photoUrl = "https://picsum.photos/137", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.5f, name = "Liam", photoUrl = "https://picsum.photos/138", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.63f, name = "Sophia", photoUrl = "https://picsum.photos/139", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.53f, name = "Alexander", photoUrl = "https://picsum.photos/140", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.88f, name = "Isabella", photoUrl = "https://picsum.photos/141", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.7f, name = "Elijah", photoUrl = "https://picsum.photos/142", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.8f, name = "Mason", photoUrl = "https://picsum.photos/143", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.68f, name = "Logan", photoUrl = "https://picsum.photos/144", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.67f, name = "Lucas", photoUrl = "https://picsum.photos/145", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.7f, name = "Henry", photoUrl = "https://picsum.photos/146", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.65f, name = "Aiden", photoUrl = "https://picsum.photos/147", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.75f, name = "Charlotte", photoUrl = "https://picsum.photos/148", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.62f, name = "Amelia", photoUrl = "https://picsum.photos/149", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.7f, name = "Harper", photoUrl = "https://picsum.photos/150", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.8f, name = "Evelyn", photoUrl = "https://picsum.photos/151", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.78f, name = "Abigail", photoUrl = "https://picsum.photos/152", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.64f, name = "Ella", photoUrl = "https://picsum.photos/153", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.85f, name = "Mia", photoUrl = "https://picsum.photos/154", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.9f, name = "Scarlett", photoUrl = "https://picsum.photos/155", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.72f, name = "Emily", photoUrl = "https://picsum.photos/156", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.69f, name = "Madison", photoUrl = "https://picsum.photos/157", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.58f, name = "Layla", photoUrl = "https://picsum.photos/158", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.77f, name = "Luna", photoUrl = "https://picsum.photos/159", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.66f, name = "Zoe", photoUrl = "https://picsum.photos/160", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.82f, name = "Grace", photoUrl = "https://picsum.photos/161", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.5f, name = "Chloe", photoUrl = "https://picsum.photos/162", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.75f, name = "Avery", photoUrl = "https://picsum.photos/163", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.74f, name = "Mila", photoUrl = "https://picsum.photos/164", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.71f, name = "Aria", photoUrl = "https://picsum.photos/165", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.79f, name = "Isla", photoUrl = "https://picsum.photos/166", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.81f, name = "Ellie", photoUrl = "https://picsum.photos/167", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.7f, name = "Lily", photoUrl = "https://picsum.photos/168", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.6f, name = "Aurora", photoUrl = "https://picsum.photos/169", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = 8.63f, name = "Hazel", photoUrl = "https://picsum.photos/170", tag = "2098d6", userPublicId = Uuid.random().toString())
    )

    @OptIn(ExperimentalUuidApi::class)
    private val demoData = listOf(
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 1", photoUrl = "https://picsum.photos/100", tag = "2098d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 2", photoUrl = "https://picsum.photos/101", tag = "ae8880", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 3", photoUrl = "https://picsum.photos/102", tag = "45dd5d", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 4", photoUrl = "https://picsum.photos/103", tag = "30e76a", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 5", photoUrl = "https://picsum.photos/104", tag = "7e3531", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 6", photoUrl = "https://picsum.photos/105", tag = "7b8557", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 7", photoUrl = "https://picsum.photos/106", tag = "7cdf84", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 8", photoUrl = "https://picsum.photos/107", tag = "553ef0", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 9", photoUrl = "https://picsum.photos/108", tag = "2cf172", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 10", photoUrl = "https://picsum.photos/109", tag = "bcbc1e", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 11", photoUrl = "https://picsum.photos/110", tag = "f0f2d6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 12", photoUrl = "https://picsum.photos/111", tag = "40099c", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 13", photoUrl = "https://picsum.photos/112", tag = "aac355", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 14", photoUrl = "https://picsum.photos/113", tag = "8b1046", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 15", photoUrl = "https://picsum.photos/114", tag = "aa5711", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 16", photoUrl = "https://picsum.photos/115", tag = "e53008", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 17", photoUrl = "https://picsum.photos/116", tag = "3519eb", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 18", photoUrl = "https://picsum.photos/117", tag = "5021bb", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 19", photoUrl = "https://picsum.photos/118", tag = "86ac59", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 20", photoUrl = "https://picsum.photos/119", tag = "421554", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 21", photoUrl = "https://picsum.photos/120", tag = "ae470e", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 22", photoUrl = "https://picsum.photos/121", tag = "563936", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 23", photoUrl = "https://picsum.photos/122", tag = "c25304", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 24", photoUrl = "https://picsum.photos/123", tag = "bb7a2c", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 25", photoUrl = "https://picsum.photos/124", tag = "0a347f", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 26", photoUrl = "https://picsum.photos/125", tag = "c7751f", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 27", photoUrl = "https://picsum.photos/126", tag = "43e028", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 28", photoUrl = "https://picsum.photos/127", tag = "0492f6", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 29", photoUrl = "https://picsum.photos/128", tag = "295dad", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 30", photoUrl = "https://picsum.photos/129", tag = "837e55", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 31", photoUrl = "https://picsum.photos/130", tag = "316378", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 32", photoUrl = "https://picsum.photos/131", tag = "c5fb82", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 33", photoUrl = "https://picsum.photos/132", tag = "c1b3c3", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 34", photoUrl = "https://picsum.photos/133", tag = "3d6942", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 35", photoUrl = "https://picsum.photos/134", tag = "40cbca", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 36", photoUrl = "https://picsum.photos/135", tag = "2e4ee8", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 37", photoUrl = "https://picsum.photos/136", tag = "9b84cc", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 38", photoUrl = "https://picsum.photos/137", tag = "01bb31", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 39", photoUrl = "https://picsum.photos/138", tag = "644da4", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 40", photoUrl = "https://picsum.photos/139", tag = "3b806c", userPublicId = Uuid.random().toString()),
        NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 41", photoUrl = "https://picsum.photos/140", tag = "3dbecc", userPublicId = Uuid.random().toString()),
    )

    @OptIn(ExperimentalUuidApi::class)
    private val community = demoData.map { it.copy(proximity = (40..70).random().div(10f), userPublicId = Uuid.random().toString()) }

    private val strangers = demoData

    val proximityDemoData = (family + friends + acquaintances + community + strangers).sortedByDescending { it.proximity }
}