package data.io.matrix.room.event

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import data.io.matrix.room.ConversationRoomIO
import data.io.social.network.conversation.message.MediaIO
import data.io.user.NetworkItemIO
import data.io.user.UserIO
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId

@Serializable
data class FullConversationRoom(
    @Embedded val data: ConversationRoomIO,

    @Relation(
        parentColumn = "id",
        entityColumn = "room_id",
        entity = ConversationRoomMember::class
    )
    val members: List<ConversationRoomMember> = emptyList(),
) {

    @Ignore
    val id = data.id

    /** Either [data.io.matrix.room.RoomSummary.canonicalAlias] or a default based on [data.io.matrix.room.RoomSummary.heroes] */
    @get:Ignore
    val name: String
        get() = data.summary?.canonicalAlias ?: data.summary?.heroes?.joinToString(", ") ?: when {
            !data.summary?.heroes.isNullOrEmpty() -> {
                data.summary.heroes.joinToString(", ") {
                    UserIO.initialsOf(it.localpart)
                }
            }
            members.isNotEmpty() -> {
                members.joinToString(", ") {
                    UserIO.initialsOf(UserId(it.userId).localpart)
                }
            }
            else -> "Room"
        }

    @get:Ignore
    val avatar: MediaIO?
        get() = data.summary?.avatar ?: if (data.summary?.isDirect == true && members.isNotEmpty()) {
            members.firstOrNull()?.content?.avatarUrl?.let { MediaIO(url = it) }
        } else null

    /** Converts this item to a network item representation */
    @Ignore
    fun toNetworkItem() = NetworkItemIO(
        publicId = data.id,
        userId = data.id,
        displayName = name,
        avatar = avatar,
        lastMessage = data.summary?.lastMessage?.content
    )
}
