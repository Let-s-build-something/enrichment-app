package data.io.matrix.room.event.content.key

import data.io.matrix.room.event.content.Mentions
import data.io.matrix.room.event.content.RelatesTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @see <a href="https://spec.matrix.org/unstable/client-server-api/#mkeyverificationdone">matrix spec</a>
 */
@Serializable
data class VerificationDoneEventContent(
    @SerialName("m.relates_to")
    override val relatesTo: RelatesTo.Reference?,
    @SerialName("transaction_id")
    override val transactionId: String?,
) : VerificationStep {
    override val externalUrl: String? = null
    override val mentions: Mentions? = null
}