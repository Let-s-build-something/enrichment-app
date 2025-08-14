package data.io.matrix.room

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.FullConversationMessage
import data.io.social.network.conversation.message.MediaIO
import data.io.user.NetworkItemIO
import data.io.user.UserIO.Companion.generateUserTag
import data.io.user.UserIO.Companion.initialsOf
import kotlinx.serialization.Transient
import net.folivo.trixnity.core.model.UserId

data class FullConversationRoom(
    @Embedded val data: ConversationRoomIO,

    @Relation(
        parentColumn = "id",
        entityColumn = "room_id",
        entity = ConversationRoomMember::class
    )
    val members: List<ConversationRoomMember> = emptyList()
) {

    @Transient
    @Ignore
    var messages: List<FullConversationMessage> = emptyList()

    @Transient
    @Ignore
    var lastMessage: FullConversationMessage? = null

    @Transient
    @Ignore
    val id = data.id

    @Transient
    @get:Ignore
    val isDirect
        get() = data.summary?.isDirect == true

    /** Either [RoomSummary.canonicalAlias] or a default based on [RoomSummary.heroes] */
    @Transient
    @get:Ignore
    val name: String
        get() = data.summary?.canonicalAlias ?: when {
            data.summary?.isDirect == true && members.isNotEmpty() -> {
                members.firstOrNull()?.displayName ?: members.firstOrNull()?.userId ?: ""
            }
            members.take(4).isNotEmpty() -> {
                members.joinToString(", ") {
                    initialsOf(it.displayName ?: UserId(it.userId).localpart)
                }.let {
                    if (members.size > 4) it.plus("...") else it
                }
            }
            !data.summary?.heroes.isNullOrEmpty() -> {
                data.summary.heroes.take(4).joinToString(", ") {
                    initialsOf(it.localpart)
                }.let {
                    if (data.summary.heroes.size > 4) it.plus("...") else it
                }
            }
            else -> "Room"
        }

    @Transient
    @get:Ignore
    val avatar: MediaIO?
        get() = data.summary?.avatar ?: (if (data.summary?.isDirect == true && members.isNotEmpty()) {
            members.firstOrNull()?.avatarUrl?.let {
                MediaIO(url = it)
            }
        } else null)

    @Transient
    @get:Ignore
    val tag: String?
        get() = if (data.summary?.isDirect == true && members.firstOrNull() != null) {
            members.firstOrNull()?.tag ?: UserId(id).generateUserTag()
        } else UserId(id).generateUserTag()

    /** Converts this item to a network item representation */
    @Ignore
    fun toNetworkItem(lastMessage: String? = null) = NetworkItemIO(
        publicId = data.id,
        userId = if (isDirect) members.firstOrNull()?.userId ?: data.id else data.id,
        displayName = name,
        avatar = avatar,
        lastMessage = lastMessage
    )
}