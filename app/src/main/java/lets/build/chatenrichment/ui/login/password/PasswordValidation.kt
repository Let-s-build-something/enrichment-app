package lets.build.chatenrichment.ui.login.password

/** Data class helping with password validation and user engagement */
data class PasswordValidation(
    /** whether this validation is valid */
    val isValid: Boolean,
    
    /** message of the validation */
    val message: String,
    
    /** whether it is a necessary to be valid */
    val isRequired: Boolean = false
)