package data.io.social.network.conversation.matrix

import kotlinx.serialization.Serializable

/** Typing notification and read receipt events */
@Serializable
data class RoomAccountData(
    val events: List<MatrixEvent>? = null
)