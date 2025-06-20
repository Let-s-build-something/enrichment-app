package data.io.social.network.request

import androidx.room.Ignore
import data.io.social.network.conversation.message.MediaIO
import data.io.user.UserIO.Companion.generateUserTag
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId

/**
 * Information about current circling request
 * Every request is pending. Rejected or accepted requests are removed immediately
 */
@Serializable
data class CirclingRequest(
	/** display name of the initiating user */
	val displayName: String? = null,

	/** identifier of the request */
	val publicId: String? = null,

	/** date, when request was created */
	val createdAt: Long? = null,

	/** URL of profile picture of the initiating user */
	val avatar: MediaIO? = null,

	/**
	 * A decimal range between -1 and 10. -1 means blocked, 1 is muted,
	 *  or just a far social circle, and 10 is the closest
	 */
	val proximity: Float? = null
) {
	val tag: String?
		@Ignore
		get() = displayName?.let { UserId(it).generateUserTag() }
}
