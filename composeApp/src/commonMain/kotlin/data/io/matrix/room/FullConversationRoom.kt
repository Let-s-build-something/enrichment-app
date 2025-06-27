package data.io.matrix.room

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.MediaIO
import data.io.user.NetworkItemIO
import data.io.user.UserIO.Companion.initialsOf
import net.folivo.trixnity.core.model.UserId

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

    /** Either [RoomSummary.canonicalAlias] or a default based on [RoomSummary.heroes] */
    @get:Ignore
    val name: String
        get() = data.summary?.canonicalAlias ?: data.summary?.heroes?.joinToString(", ") ?: when {
            members.take(4).isNotEmpty() -> {
                members.joinToString(", ") {
                    initialsOf(it.displayName ?: UserId(it.userId).localpart)
                }.let {
                    if (members.size > 4) it.plus("...") else it
                }
            }
            !data.summary?.heroes.isNullOrEmpty() -> {
                data.summary.heroes.joinToString(", ") {
                    initialsOf(it.localpart)
                }.let {
                    if (data.summary.heroes.size > 4) it.plus("...") else it
                }
            }
            else -> "Room"
        }

    @get:Ignore
    val avatar: MediaIO?
        get() = data.summary?.avatar ?: (if (data.summary?.isDirect == true && members.isNotEmpty()) {
            members.firstOrNull()?.avatarUrl?.let {
                MediaIO(url = it)
            }
        } else null)

    /** Converts this item to a network item representation */
    @Ignore
    fun toNetworkItem() = NetworkItemIO(
        publicId = data.id,
        userId = if (data.summary?.isDirect == true) {
            members.firstOrNull()?.userId ?: data.id
        } else data.id,
        displayName = name,
        avatar = avatar,
        lastMessage = data.summary?.lastMessage?.content
    )
}