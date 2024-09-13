package data.io.social.network

/** types of errors from requesting circling */
enum class RequestCircleErrorCode {
    /** the requested user already exists in the initiator's circle or request exists */
    DUPLICATE,

    /** the requested user does not exist */
    NON_EXISTENT,

    /** 
     * In case the token is not valid - 6 integers only,
     * or display name contains spaces, or is shorter than 6 characters or longer than 256 characters
     */
    INCORRECT_FORMAT,
    
    /** User requesting the circle is blocked by the initiator */
    USER_BLOCKED
}