package ui.login

/** Type of result that can be received by the sign in services */
enum class LoginResultType {

    /** general error, the request failed */
    FAILURE,

    /** the nonce don't match, security check */
    AUTH_SECURITY,

    /** email already exists */
    EMAIL_EXISTS,

    /** username already exists */
    USERNAME_EXISTS,

    /** The supplied auth credential is incorrect, malformed or has expired. */
    INVALID_CREDENTIAL,

    /** successful request, user is signed in */
    SUCCESS
}