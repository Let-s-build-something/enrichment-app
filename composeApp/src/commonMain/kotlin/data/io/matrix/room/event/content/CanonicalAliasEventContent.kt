package data.io.matrix.room.event.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomAliasId

/**
 * @see <a href="https://spec.matrix.org/v1.10/client-server-api/#mroomcanonical_alias">matrix spec</a>
 */
@Serializable
data class CanonicalAliasEventContent(
    @SerialName("alias")
    val alias: RoomAliasId? = null,
    @SerialName("alt_aliases")
    val aliases: Set<RoomAliasId>? = null,
    @SerialName("external_url")
    override val externalUrl: String? = null,
): StateEventContent
