package data.io.social.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response to circling action */
@Serializable
data class CirclingActionResponse(

	/** Identifier of the new connection */
	@SerialName("public_id")
	val publicId: String?
)