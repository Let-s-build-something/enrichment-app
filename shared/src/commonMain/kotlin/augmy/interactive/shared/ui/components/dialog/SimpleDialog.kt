package augmy.interactive.shared.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import augmy.interactive.shared.ui.base.MaxModalWidthDp
import augmy.interactive.shared.ui.components.OutlinedButton
import augmy.interactive.shared.ui.theme.LocalTheme

/**
 * Most basic type of a dialog
 */
@Composable
fun SimpleDialog(
    modifier: Modifier = Modifier,
    state: SimpleDialogState = rememberSimpleDialogState(),
    content: @Composable ColumnScope.() -> Unit = {},
    properties: DialogProperties = dismissibleDialogProperties,
    onDismissRequest: () -> Unit
) {
    DialogShell(
        properties = properties,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top = 24.dp,
                    start = 24.dp,
                    bottom = 8.dp,
                    end = 8.dp
                )
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            if(state.title != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if(state.icon != null) {
                        Icon(
                            imageVector = state.icon,
                            contentDescription = "",
                            tint = LocalTheme.current.colors.secondary
                        )
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(end = 16.dp),
                        text = state.title,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = LocalTheme.current.colors.primary
                        )
                    )
                }
            }
            if(state.message != null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(top = 8.dp, end = 16.dp),
                    text = state.message,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = LocalTheme.current.colors.primary
                    )
                )
            }
            content()
            if(state.negativeButtonState != null || state.positiveButtonState != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if(state.negativeButtonState != null) {
                        OutlinedButton(
                            text = state.negativeButtonState.text,
                            onClick = state.negativeButtonState.onClick,
                            activeColor = LocalTheme.current.colors.secondary
                        )
                    }
                    if( state.positiveButtonState != null) {
                        OutlinedButton(
                            text = state.positiveButtonState.text,
                            onClick = state.positiveButtonState.onClick,
                            activeColor = LocalTheme.current.colors.brandMainDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogShell(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = dismissibleDialogProperties,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Card(
            modifier = modifier
                .widthIn(max = MaxModalWidthDp.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = LocalTheme.current.colors.backgroundDark,
                contentColor = LocalTheme.current.colors.primary
            ),
            content = content
        )
    }
}

/**
 * Input information for [SimpleDialog] Compose Dialog window
 */
data class SimpleDialogState(

    /** Title or header of the dialog */
    val title: String? = null,

    /** Text content */
    val message: String? = null,

    /** Icon in the left top corner of the dialog */
    val icon: ImageVector? = null,

    /** Positive, bold, colorful button */
    val positiveButtonState: ButtonState? = null,

    /** Negative, thinner, not color button */
    val negativeButtonState: ButtonState? = null
)

/**
 * Remembers input information for the [SimpleDialog]
 *
 * @param title Title or header of the dialog
 * @param content Text content
 * @param positiveButtonState Positive, bold, colorful button
 * @param negativeButtonState Negative, thinner, not color button
 */
@Composable
fun rememberSimpleDialogState(
    title: String? = null,
    content: String? = null,
    icon: ImageVector? = null,
    positiveButtonState: ButtonState? = null,
    negativeButtonState: ButtonState? = null
): SimpleDialogState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) {
        SimpleDialogState(
            title = title,
            message = content,
            icon = icon,
            positiveButtonState = positiveButtonState,
            negativeButtonState = negativeButtonState
        )
    }
    return state
}