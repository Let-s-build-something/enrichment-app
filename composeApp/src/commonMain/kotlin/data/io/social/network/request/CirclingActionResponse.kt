package data.io.social.network.request

import kotlinx.serialization.Serializable

/** Response to circling action */
@Serializable
data class CirclingActionResponse(

	/** Identifier of the new connection */
	val publicId: String?
)