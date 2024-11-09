package data.io.social.network.request

import kotlinx.serialization.Serializable

/** Response to the request for circling requests */
@Serializable
data class CirclingActionRequest(
	/** whether user wants to accept the request */
	val accept: Boolean,

	/** uid of the request */
	val uid: String,

	/** optional message associated with the response */
	val message: String? = null
)