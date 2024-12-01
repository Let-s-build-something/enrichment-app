package data.io.social

import kotlinx.serialization.Serializable

/** Configuration related to currently signed-in user */
@Serializable
data class UserConfiguration(

	/** user selected privacy */
	val privacy: UserPrivacy = UserPrivacy.Public,

	/** user selected visibility */
	val visibility: UserVisibility = UserVisibility.Online
)

/** privacy of the user */
enum class UserPrivacy {
	/** not visible to outsiders */
	Private,
	/** visible to outsiders */
	Public
}

/** Visibility of the user */
enum class UserVisibility {
	/** connected to the app */
	Online,

	/** not visible to anyone */
	Invisible,

	/** disconnected from the app */
	Offline
}