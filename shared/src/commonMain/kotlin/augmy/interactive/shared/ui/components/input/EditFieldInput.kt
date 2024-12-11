package augmy.interactive.shared.ui.components.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.shared.generated.resources.Res
import augmy.shared.generated.resources.accessibility_clear
import org.jetbrains.compose.resources.stringResource

/**
 * Generic input field for text input
 */
@Composable
fun EditFieldInput(
    modifier: Modifier = Modifier,
    value: String,
    textValue: TextFieldValue? = null,
    textStyle: TextStyle = TextStyle(
        color = LocalTheme.current.colors.primary,
        fontSize = 16.sp,
        textAlign = TextAlign.Start
    ),
    colors: TextFieldColors = LocalTheme.current.styles.textFieldColors,
    enabled: Boolean = true,
    paddingValues: PaddingValues = TextFieldDefaults.contentPaddingWithoutLabel(),
    maxLines: Int = 1,
    minLines: Int = 1,
    maxCharacters: Int = -1,
    hint: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    shape: Shape = LocalTheme.current.shapes.rectangularActionShape,
    errorText: String? = null,
    suggestText: String? = null,
    minHeight: Dp = TextFieldDefaults.MinHeight,
    isCorrect: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isClearable: Boolean = false,
    onClear: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onValueChange: (TextFieldValue) -> Unit,
) {
    val isFocused = remember(value) { mutableStateOf(false) }
    val text = if(textValue == null) {
        remember(value) {
            mutableStateOf(TextFieldValue(value, TextRange(value.length)))
        }
    }else null
    val textContent = text?.value?.text ?: textValue?.text ?: ""

    val controlColor by animateColorAsState(
        when {
            errorText != null -> colors.errorTextColor
            isCorrect -> SharedColors.GREEN_CORRECT
            isFocused.value -> colors.focusedTextColor
            !enabled -> colors.disabledTextColor
            else -> colors.unfocusedTextColor
        },
        label = "controlColorChange"
    )

    Column(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .animateContentSize()
    ) {
        CustomTextField(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isFocused.value) 1.dp else 0.25.dp,
                    color = controlColor,
                    shape = shape
                )
                .onFocusChanged {
                    isFocused.value = it.isFocused
                },
            shape = shape,
            value = text?.value ?: textValue ?: TextFieldValue(),
            minHeight = minHeight,
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
            colors = colors,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = errorText != null,
            enabled = enabled,
            trailingIcon = trailingIcon ?: if((isClearable && textContent.isNotEmpty()) || maxCharacters > 0) {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if(maxCharacters > 0) {
                            Text(
                                text = "${textContent.length}/$maxCharacters",
                                style = LocalTheme.current.styles.regular.copy(
                                    color = if(textContent.length > maxCharacters) {
                                        SharedColors.RED_ERROR
                                    }else LocalTheme.current.colors.disabled
                                )
                            )
                        }
                        AnimatedVisibility(enabled && isClearable && textContent.isNotEmpty()) {
                            MinimalisticIcon(
                                contentDescription = stringResource(Res.string.accessibility_clear),
                                imageVector = Icons.Outlined.Clear,
                                tint = LocalTheme.current.colors.secondary
                            ) {
                                onClear()
                                onValueChange(TextFieldValue())
                                text?.value = TextFieldValue()
                            }
                        }
                    }
                }
            }else trailingIcon,
            leadingIcon = if(leadingIcon != null) {
                {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(2.dp),
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = controlColor
                    )
                }
            }else null,
            onValueChange = { output ->
                onValueChange(output)
                text?.value = output
            }
        )
        AnimatedVisibility(suggestText.isNullOrBlank().not()) {
            Text(
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 4.dp
                ),
                text = suggestText ?: "",
                style = LocalTheme.current.styles.regular
            )
        }
        AnimatedVisibility(errorText.isNullOrBlank().not()) {
            Text(
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 4.dp
                ),
                text = errorText ?: "",
                style = LocalTheme.current.styles.regular.copy(
                    color = SharedColors.RED_ERROR,
                    fontSize = 14.sp
                )
            )
        }
    }
}

const val DELAY_BETWEEN_TYPING_SHORT = 200L
