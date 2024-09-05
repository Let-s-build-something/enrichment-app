package base

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

data class CustomSnackbarVisuals(
    override val actionLabel: String?,
    override val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
    override val message: String,
    val isError: Boolean = false,
    override val withDismissAction: Boolean = true
): SnackbarVisuals