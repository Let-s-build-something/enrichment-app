package lets.build.chatenrichment.data.shared.io.user

/** Firebase DB object for all user's information */
data class UserProfile(
    /** user's username */
    val username: String? = null,
    
    /** photo path to user's picture */
    val photoUrl: String? = null
)