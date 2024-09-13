package data.io.social.network

/** Response to the request for circling requests */
data class CirclingResponseRequest(
	/** whether user wants to accept the request */
	val accept: Boolean,

	/** optional message associated with the response */
	val message: String? = null
)