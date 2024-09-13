package data.io.social.network

/** types of errors from responding to a circling request */
enum class CircleResponseErrorCode {
    /** the request does not exist anymore */
    NON_EXISTENT,

    /** User requesting the circle is blocked by the initiator */
    USER_BLOCKED
}