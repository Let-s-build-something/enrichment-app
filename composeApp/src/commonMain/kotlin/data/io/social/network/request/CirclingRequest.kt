package data.io.social.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Information about current circling request
 * Every request is pending. Rejected or accepted requests are removed immediately
 */
@Serializable
data class CirclingRequest(
	/** display name of the initiating user */
	@SerialName("display_name")
	val displayName: String? = null,

	/** token of the initiating user */
	val tag: String? = null,

	/** identifier of the request */
	@SerialName("public_id")
	val publicId: String? = null,

	/** date, when request was created */
	@SerialName("created_at")
	val createdAt: Long? = null,

	/** URL of profile picture of the initiating user */
	@SerialName("photo_url")
	val photoUrl: String? = null,

	/**
	 * A decimal range between -1 and 10. -1 means blocked, 1 is muted,
	 *  or just a far social circle, and 10 is the closest
	 */
	val proximity: Float? = null
)