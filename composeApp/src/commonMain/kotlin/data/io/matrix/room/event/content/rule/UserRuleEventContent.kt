package data.io.matrix.room.event.content.rule

import data.io.matrix.room.event.content.StateEventContent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @see <a href="https://spec.matrix.org/v1.10/client-server-api/#mpolicyruleuser">matrix spec</a>
 */
@Serializable
data class UserRuleEventContent(
    @SerialName("entity")
    val entity: String,
    @SerialName("reason")
    val reason: String,
    @SerialName("recommendation")
    val recommendation: String,
    @SerialName("external_url")
    override val externalUrl: String? = null,
) : StateEventContent