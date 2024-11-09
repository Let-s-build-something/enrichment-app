package data.io

/** Unified enum for all API error codes */
enum class ApiErrorCode {

    /** the requested entity already exists */
    DUPLICATE,

    /** the requested entity does not exist */
    NON_EXISTENT,

    /**
     * In case the token is not valid - 6 integers only,
     * or display name contains spaces, or is shorter than 6 characters or longer than 256 characters
     */
    INCORRECT_FORMAT,

    /** User requesting the circle is blocked by the initiator */
    USER_BLOCKED,

    /** A field within the request body is missing or empty */
    MISSING_FIELD
}