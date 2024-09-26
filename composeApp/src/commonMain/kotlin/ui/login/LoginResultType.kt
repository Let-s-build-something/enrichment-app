package ui.login

/** Type of result that can be received by the sign in services */
enum class LoginResultType {

    /** general error, the request failed */
    FAILURE,

    /** sign in request cancelled */
    CANCELLED,

    /** the nonce don't match, security check */
    AUTH_SECURITY,

    /** email already exists */
    EMAIL_EXISTS,

    /** the UI is missing window, iOS specific */
    NO_WINDOW,

    /** There are no credentials on the device, Android specific */
    NO_GOOGLE_CREDENTIALS,

    /** The supplied auth credential is incorrect, malformed or has expired. */
    INVALID_CREDENTIAL,

    /** successful request, user is signed in */
    SUCCESS
}