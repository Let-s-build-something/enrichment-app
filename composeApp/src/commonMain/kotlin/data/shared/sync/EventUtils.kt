package data.shared.sync

import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.conversation_detail_you
import augmy.composeapp.generated.resources.message_room_ban
import augmy.composeapp.generated.resources.message_room_invite
import augmy.composeapp.generated.resources.message_room_join
import augmy.composeapp.generated.resources.message_room_knock
import augmy.composeapp.generated.resources.message_room_leave
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.jetbrains.compose.resources.getString

object EventUtils {
    suspend fun Membership.asMessage(isSelf: Boolean, displayName: String): String {
        val name = if(isSelf) getString(Res.string.conversation_detail_you) else displayName

        return "$name ${getString(when(this) {
            Membership.INVITE -> Res.string.message_room_invite
            Membership.JOIN -> Res.string.message_room_join
            Membership.KNOCK -> Res.string.message_room_knock
            Membership.LEAVE -> Res.string.message_room_leave
            Membership.BAN -> Res.string.message_room_ban
        })}"
    }
}