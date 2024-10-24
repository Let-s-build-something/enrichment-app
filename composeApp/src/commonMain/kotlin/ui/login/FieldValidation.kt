package ui.login

/** Data class helping with password validation and user engagement */
data class FieldValidation(
    /** whether this validation is valid */
    val isValid: Boolean,
    
    /** message of the validation */
    val message: String,
    
    /** whether it is a necessary to be valid */
    val isRequired: Boolean = true,

    /** whether validation should be visible even when correct */
    val isVisibleCorrect: Boolean = true,
)