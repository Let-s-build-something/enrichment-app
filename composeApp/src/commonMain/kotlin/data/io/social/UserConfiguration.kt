package data.io.social

/** Configuration related to currently signed in user */
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