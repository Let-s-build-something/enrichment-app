package ui.network.connection

import kotlinx.serialization.Serializable

/** A request to change a social connection */
@Serializable
data class SocialConnectionRequest(
    /**
     * A decimal range between -1 and 10. -1 means blocked, 1 is muted,
     *  or just a far social circle, and 10 is the closest
     */
    val proximity: Float
)