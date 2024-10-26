package data.io.social

import kotlinx.serialization.Serializable

/** Configuration related to currently signed in user */
@Serializable
data class UserConfiguration(

	/** user selected privacy */
	val privacy: UserPrivacy = UserPrivacy.PUBLIC,

	/** user selected visibility */
	val visibility: UserVisibility = UserVisibility.ONLINE
)

/** privacy of the user */
enum class UserPrivacy {
	/** not visible to outsiders */
	PRIVATE,
	/** visible to outsiders */
	PUBLIC
}

/** Visibility of the user */
enum class UserVisibility {
	/** connected to the app */
	ONLINE,

	/** not visible to anyone */
	INVISIBLE,

	/** disconnected from the app */
	OFFLINE
}