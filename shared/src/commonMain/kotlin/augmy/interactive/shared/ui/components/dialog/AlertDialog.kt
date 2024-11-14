package augmy.interactive.shared.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    title: String? = null,
    message: String,
    confirmButtonState: ButtonState? = null,
    dismissButtonState: ButtonState? = null,
    additionalContent: @Composable (() -> Unit)? = null,
    properties: DialogProperties = dismissibleDialogProperties,
    onDismissRequest: () -> Unit,
    icon: ImageVector,
) {
    AlertDialog(
        icon = null,
        title = if(title == null) null else {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "",
                        tint = LocalTheme.current.colors.secondary
                    )
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = LocalTheme.current.colors.primary
                        )
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = LocalTheme.current.colors.primary
                    )
                )
                additionalContent?.invoke()
            }
        },
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