package augmy.interactive.shared.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import augmy.interactive.shared.ui.components.OutlinedButton
import augmy.interactive.shared.ui.theme.LocalTheme

/** Dismissible properties for dialog */
val dismissibleDialogProperties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)

/**
 * Themed basic dialog
 */
@Composable
fun AlertDialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    message: AnnotatedString? = null,
    confirmButtonState: ButtonState? = null,
    dismissButtonState: ButtonState? = null,
    intrinsicContent: Boolean = true,
    additionalContent: @Composable (ColumnScope.() -> Unit)? = null,
    properties: DialogProperties = dismissibleDialogProperties,
    onDismissRequest: () -> Unit,
    icon: ImageVector? = null,
) {
    AlertDialog(
        icon = null,
        title = if(title == null && icon == null) null else {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if(icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "",
                            tint = LocalTheme.current.colors.secondary
                        )
                    }
                    if(title != null) {
                        Text(
                            text = title,
                            style = LocalTheme.current.styles.title
                        )
                    }
                }
            }
        },
        text = if(message != null || additionalContent != null) {
            {
                Column(
                    modifier = if (intrinsicContent) {
                        modifier
                            .height(IntrinsicSize.Max)
                            .width(IntrinsicSize.Max)
                    }else modifier
                ) {
                    if(message != null) {
                        Text(
                            text = message,
                            style = LocalTheme.current.styles.regular
                        )
                    }
                    additionalContent?.invoke(this)
                }
            }
        }else null,
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            if(confirmButtonState != null) {
                OutlinedButton(
                    text = confirmButtonState.text,
                    onClick = {
                        confirmButtonState.onClick()
                        onDismissRequest()
                    },
                    enabled = confirmButtonState.enabled,
                    activeColor = LocalTheme.current.colors.brandMain
                )
            }
        },
        dismissButton = if(dismissButtonState == null) null else {
            {
                OutlinedButton(
                    text = dismissButtonState.text,
                    onClick = {
                        dismissButtonState.onClick()
                        onDismissRequest()
                    },
                    enabled = dismissButtonState.enabled,
                    activeColor = LocalTheme.current.colors.secondary
                )
            }
        },
        containerColor = LocalTheme.current.colors.backgroundDark,
        properties = properties
    )
}