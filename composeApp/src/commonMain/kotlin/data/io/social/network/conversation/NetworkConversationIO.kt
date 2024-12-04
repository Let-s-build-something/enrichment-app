package data.io.social.network.conversation

import data.io.user.NetworkItemIO
import kotlinx.serialization.Serializable

/** Detailed information about a conversation */
@Serializable
data class NetworkConversationIO(

    /** Public identifier of this conversation */
    val publicId: String? = null,

    /** Url of the picture of the conversation */
    val pictureUrl: String? = null,

    /** Name of the conversation */
    val name: String? = null,

    /** Initially estimated proximity of the conversation, which can be changed */
    val proximity: Float? = null,

    // ============================= Empty in some instances =================================

    /** Tag of this conversation */
    val tag: String? = null,

    /** Last message within this conversation */
    val lastMessage: String? = null,

    /** Users participating in the conversation */
    val users: List<NetworkItemIO>? = null
)