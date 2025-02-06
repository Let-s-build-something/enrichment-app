package data.io.social.network.conversation.message

import androidx.room.Ignore
import data.io.user.NetworkItemIO
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** A singular reaction to a message */
@Serializable
data class MessageReactionIO(
    /** message content */
    val content: String? = null,

    /** Public id of the author of this message */
    val authorPublicId: String? = null
) {
    /** Author user information */
    @Ignore
    @Transient
    var user: NetworkItemIO? = null
}
