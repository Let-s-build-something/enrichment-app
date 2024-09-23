package chat.enrichment.shared.ui.components.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.enrichment.shared.ui.components.MinimalisticIcon
import chat.enrichment.shared.ui.theme.LocalTheme
import chat.enrichment.shared.ui.theme.SharedColors

/**
 * Generic input field for text input
 */
@Composable
fun EditFieldInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle = TextStyle(
        color = LocalTheme.current.colors.primary,
        fontSize = 16.sp,
        textAlign = TextAlign.Start
    ),
    paddingValues: PaddingValues = TextFieldDefaults.contentPaddingWithoutLabel(),
    maxLines: Int = 1,
    minLines: Int = 1,
    hint: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorText: String? = null,
    isCorrect: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isClearable: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val isFocused = remember(value) { mutableStateOf(false) }
    val text = remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }

    val controlColor by animateColorAsState(
        when {
            errorText != null -> SharedColors.RED_ERROR
            isCorrect -> SharedColors.GREEN_CORRECT
            isFocused.value -> LocalTheme.current.colors.primary
            else -> LocalTheme.current.colors.secondary
        },
        label = "controlColorChange"
    )

    Column {
        CustomTextField(
            modifier = modifier
                .border(
                    if (isFocused.value) 1.dp else 0.25.dp,
                    controlColor,
                    LocalTheme.current.shapes.componentShape
                )
                .onFocusChanged {
                    isFocused.value = it.isFocused
                },
            value = text.value,
            paddingValues = paddingValues,
            minLines = minLines,
            maxLines = maxLines,
            singleLine = maxLines == 1,
            visualTransformation = visualTransformation,
            textStyle = TextStyle(
                color = LocalTheme.current.colors.primary,
                fontSize = 16.sp,
                textAlign = TextAlign.Start
            ),
            placeholder = {
                if(hint != null) {
                    Text(
                        text = hint,
                        style = textStyle.copy(
                            color = LocalTheme.current.colors.brandMain
                        )
                    )
                }
            },
            colors = LocalTheme.current.styles.textFieldColors,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = errorText != null,
            shape = LocalTheme.current.shapes.circularActionShape,
            trailingIcon = if(isClearable) {
                {
                    MinimalisticIcon(
                        contentDescription = "Clear",
                        imageVector = Icons.Outlined.Clear,
                        tint = controlColor
                    ) {
                        onValueChange("")
                        text.value = TextFieldValue()
                    }
                }
            }else trailingIcon,
            onValueChange = { output ->
                onValueChange(output.text)
                text.value = output
            }
        )
        AnimatedVisibility(errorText.isNullOrBlank().not()) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = errorText ?: "",
                style = LocalTheme.current.styles.regular.copy(
                    color = SharedColors.RED_ERROR,
                    fontSize = 14.sp
                )
            )
        }
    }
}