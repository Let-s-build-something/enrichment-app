package augmy.interactive.shared.ui.base

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.shared.generated.resources.Res
import augmy.shared.generated.resources.button_dismiss
import org.jetbrains.compose.resources.stringResource

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
            val textColor = if((data.visuals as? CustomSnackbarVisuals)?.isError == true) {
                Color.White
            }else LocalTheme.current.colors.tetrial


            val actionLabel = data.visuals.actionLabel
            val actionComposable: (@Composable () -> Unit)? =
                if (actionLabel != null) {
                    @Composable {
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(contentColor = textColor),
                            onClick = { data.performAction() },
                            content = { Text(actionLabel) }
                        )
                    }
                } else {
                    null
                }
            val dismissActionComposable: (@Composable () -> Unit)? =
                if (data.visuals.withDismissAction) {
                    @Composable {
                        IconButton(
                            onClick = { data.dismiss() },
                            content = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(Res.string.button_dismiss),
                                )
                            }
                        )
                    }
                } else {
                    null
                }
            Snackbar(
                modifier = modifier.padding(12.dp),
                action = actionComposable,
                dismissAction = dismissActionComposable,
                actionOnNewLine = false,
                shape = LocalTheme.current.shapes.componentShape,
                containerColor = if((data.visuals as? CustomSnackbarVisuals)?.isError == true) {
                    SharedColors.RED_ERROR
                }else LocalTheme.current.colors.brandMainDark,
                contentColor = textColor,
                actionContentColor = textColor,
                dismissActionContentColor = textColor,
                content = {
                    Text(
                        text = data.visuals.message,
                        style = LocalTheme.current.styles.regular.copy(color = textColor)
                    )
                }
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