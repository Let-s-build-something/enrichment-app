package data.io.matrix.room.event

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import data.io.social.network.conversation.message.MediaIO
import data.io.user.NetworkItemIO
import data.io.user.UserIO.Companion.generateUserTag
import database.AppRoomDatabase.Companion.TABLE_ROOM_MEMBER
import kotlinx.serialization.SerialName
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent.Invite
import net.folivo.trixnity.core.model.events.m.room.Membership

@Entity(TABLE_ROOM_MEMBER)
data class ConversationRoomMember(

    val userId: String,

    @ColumnInfo("display_name")
    val displayName: String?,

    @ColumnInfo("room_id")
    val roomId: String,

    val timestamp: Long?,

    val sender: UserId?,

    val proximity: Float? = null,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    val membership: Membership,

    @ColumnInfo("is_direct")
    @SerialName("is_direct")
    val isDirect: Boolean? = null,

    val joinAuthorisedViaUsersServer: UserId? = null,

    val thirdPartyInvite: Invite? = null,

    val reason: String? = null,

    val externalUrl: String? = null,

    @PrimaryKey
    val id: String = "${roomId}_$userId",
) {
    val tag: String?
        @Ignore
        get() = UserId(userId).generateUserTag()

    fun toNetworkItem() = NetworkItemIO(
        publicId = id,
        userId = userId,
        displayName = displayName ?: UserId(userId).localpart,
        avatar = MediaIO(url = avatarUrl)
    )
}
