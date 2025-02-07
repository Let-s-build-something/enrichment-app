package data.io.matrix.room

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatrixEventRelation(
    @SerialName("m.in_reply_to")
    val inReplyTo: MatrixReplyEvent? = null,

    val relType: String? = null,
    val eventId: String? = null,
    /** A flag to denote that this is a thread with reply fallback */
    val isFallingBack: Boolean? = null
)