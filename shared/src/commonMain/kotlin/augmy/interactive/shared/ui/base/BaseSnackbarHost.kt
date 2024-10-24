package augmy.interactive.shared.ui.base

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors

/**
 * Themed snackbar host with custom snackbar and possibility to display in error version
 */
@Composable
fun BaseSnackbarHost(
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState
) {
    SnackbarHost(
        modifier = modifier,
        hostState = hostState,
        snackbar = { data ->
            Snackbar(
                data,
                shape = LocalTheme.current.shapes.componentShape,
                containerColor = if((data.visuals as? CustomSnackbarVisuals)?.isError == true) {
                    SharedColors.RED_ERROR
                }else LocalTheme.current.colors.brandMainDark,
                contentColor = if((data.visuals as? CustomSnackbarVisuals)?.isError == true) {
                    Color.White
                }else LocalTheme.current.colors.tetrial,
                actionColor = if((data.visuals as? CustomSnackbarVisuals)?.isError == true) {
                    Color.White
                }else LocalTheme.current.colors.tetrial,
                dismissActionContentColor = if((data.visuals as? CustomSnackbarVisuals)?.isError == true) {
                    Color.White
                }else LocalTheme.current.colors.tetrial,
            )
        }
    )
}

/**
 * Custom snackbar visuals for the purpose of sending custom information,
 * such as whether it is an error snackbar [isError]
 */
data class CustomSnackbarVisuals(
    override val actionLabel: String? = null,
    override val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
    override val message: String,
    val isError: Boolean = false,
    override val withDismissAction: Boolean = true
): SnackbarVisuals