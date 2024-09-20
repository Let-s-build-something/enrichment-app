package data.io.user

/** user object specific to our database saved in custom DB */
data class UserIO(
    /** username of the current user */
    val username: String? = null,

    /** tag of the current user */
    val tag: String? = null,

    /** current token which should be active */
    val token: String? = null,

    /** token expiration in milliseconds */
    val tokenExpirationMs: Long? = null
)