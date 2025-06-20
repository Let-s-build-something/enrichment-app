package data.io.social.network.request

import kotlinx.serialization.Serializable

/** Response to the request for circling requests */
@Serializable
data class CirclingActionRequest(

	/** Accepts the request if not null */
	val proximity: Float? = null
)