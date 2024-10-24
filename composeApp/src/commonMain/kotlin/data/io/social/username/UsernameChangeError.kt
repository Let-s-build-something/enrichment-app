package data.io.social.username

enum class UsernameChangeError {
    /** the requested username already exists in combination with */
    DUPLICATE,

    /**
     * display name has white space at the beginning or end,
     * contains disallowed characters,
     * has less than 6 characters,
     * or more than 256 characters
     */
    INVALID_FORMAT
}