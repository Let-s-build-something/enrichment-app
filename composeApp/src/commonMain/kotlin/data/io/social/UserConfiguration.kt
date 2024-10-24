package data.io.social

import kotlinx.serialization.Serializable

/** Configuration related to currently signed in user */
@Serializable
data class UserConfiguration(

	/** user selected visibility */
	val visibility: UserVisibility = UserVisibility.PRIVATE
)

/** visibility of the user */
enum class UserVisibility {
	/** not visible to outsiders */
	PRIVATE,
	/** visible to outsiders */
	PUBLIC
}