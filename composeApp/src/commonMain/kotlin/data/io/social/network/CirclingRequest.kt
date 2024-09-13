package data.io.social.network

/**
 * Information about current circling request
 * Every request is pending. Rejected or accepted requests are removed immediately
 */
data class CirclingRequest(
	/** display name of the user being requested */
	val displayName: String,

	/** token of the user being requested */
	val token: String,

	/** URL of profile picture of the requested user */
	val pictureUrl: String
)