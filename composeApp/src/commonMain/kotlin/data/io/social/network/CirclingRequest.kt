package data.io.social.network

import kotlinx.serialization.Serializable

/**
 * Information about current circling request
 * Every request is pending. Rejected or accepted requests are removed immediately
 */
@Serializable
data class CirclingRequest(
	/** display name of the initiating user */
	val displayName: String? = null,

	/** token of the initiating user */
	val tag: String? = null,

	/** identifier of the request */
	val uid: String? = null,

	/** date, when request was created */
	val date: Long? = null,

	/** URL of profile picture of the initiating user */
	val photoUrl: String? = null
)