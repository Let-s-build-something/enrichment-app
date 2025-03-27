package augmy.interactive.shared.ui.components.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.shared.generated.resources.Res
import augmy.shared.generated.resources.accessibility_clear
import org.jetbrains.compose.resources.stringResource

/**
 * Brand specific customized [BasicTextField] supporting error state via [errorText], [suggestText], [isCorrect], and trailing icon
 */
@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    state: TextFieldState,
    textStyle: TextStyle = LocalTheme.current.styles.title.copy(
        fontSize = 18.sp
    ),
    paddingValues: PaddingValues = PaddingValues(
        start = 16.dp,
        end = 0.dp,
        top = 8.dp,
        bottom = 8.dp
    ),
    colors: TextFieldColors = LocalTheme.current.styles.textFieldColors,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.RevealLastTyped,
    inputTransformation: InputTransformation? = null,
    prefixIcon: ImageVector? = null,
    maxCharacters: Int = -1,
    focusRequester: FocusRequester = remember(state) { FocusRequester() },
    trailingIcon: @Composable (() -> Unit)? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    shape: Shape = LocalTheme.current.shapes.rectangularActionShape,
    errorText: String? = null,
    hint: String? = null,
    suggestText: String? = null,
    isClearable: Boolean = false,
    showBorders: Boolean = true,
    isCorrect: Boolean = false,
    enabled: Boolean = true,
    isFocused: MutableState<Boolean> = remember(state.text) { mutableStateOf(false) }
) {
    val controlColor = if(showBorders) {
        animateColorAsState(
            when {
                errorText != null -> colors.errorTextColor
                isCorrect -> SharedColors.GREEN_CORRECT
                isFocused.value -> colors.focusedTextColor
                !enabled -> colors.disabledTextColor
                else -> colors.unfocusedTextColor
            },
            label = "controlColorChange"
        )
    }else null

    Column(modifier = modifier.animateContentSize()) {
        Row(
            Modifier
                .heightIn(min = 44.dp)
                .then(
                    controlColor?.value?.let {
                        Modifier.border(
                            width = if (isFocused.value) 1.dp else 0.25.dp,
                            color = it,
                            shape = shape
                        )
                    } ?: Modifier
                )
                .clickable(indication = null, interactionSource = null) {
                    focusRequester.requestFocus()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            prefixIcon?.let { vector ->
                Icon(
                    modifier = Modifier.padding(start = 6.dp),
                    imageVector = vector,
                    contentDescription = null,
                    tint = LocalTheme.current.colors.disabled
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .padding(paddingValues),
                contentAlignment = Alignment.CenterStart
            ) {
                if(keyboardOptions.keyboardType == KeyboardType.Password) {
                    BasicSecureTextField(
                        modifier = fieldModifier
                            .onKeyEvent { keyEvent ->
                                if(keyEvent.key == Key.Escape) {
                                    focusRequester.freeFocus()
                                }else false
                            }
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                isFocused.value = it.isFocused
                            },
                        state = state,
                        cursorBrush = Brush.linearGradient(listOf(textStyle.color, textStyle.color)),
                        textObfuscationMode = textObfuscationMode,
                        textStyle = textStyle,
                        keyboardOptions = keyboardOptions,
                        onKeyboardAction = onKeyboardAction
                    )
                }else {
                    BasicTextField(
                        modifier = fieldModifier
                            .onKeyEvent { keyEvent ->
                                if(keyEvent.key == Key.Escape) {
                                    focusRequester.freeFocus()
                                }else false
                            }
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                isFocused.value = it.isFocused
                            },
                        inputTransformation = inputTransformation,
                        state = state,
                        cursorBrush = Brush.linearGradient(listOf(textStyle.color, textStyle.color)),
                        textStyle = textStyle,
                        lineLimits = lineLimits,
                        keyboardOptions = keyboardOptions,
                        onKeyboardAction = onKeyboardAction
                    )
                }
                if(hint != null) {
                    androidx.compose.animation.AnimatedVisibility(state.text.isEmpty()) {
                        Text(
                            text = hint,
                            style = textStyle.copy(
                                color = colors.disabledTextColor
                            )
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if(maxCharacters > 0) {
                    Text(
                        text = "${state.text.length}/$maxCharacters",
                        style = LocalTheme.current.styles.regular.copy(
                            color = if(state.text.length > maxCharacters) {
                                SharedColors.RED_ERROR
                            }else LocalTheme.current.colors.disabled
                        )
                    )
                }
                Crossfade(trailingIcon != null) { showTrailingIcon ->
                    if(showTrailingIcon) {
                        trailingIcon?.invoke()
                    }else if((isClearable && state.text.isNotEmpty()) || maxCharacters > 0) {
                        AnimatedVisibility(enabled && isClearable && state.text.isNotEmpty()) {
                            MinimalisticIcon(
                                contentDescription = stringResource(Res.string.accessibility_clear),
                                imageVector = Icons.Outlined.Clear,
                                tint = LocalTheme.current.colors.secondary
                            ) {
                                state.setTextAndPlaceCursorAtEnd("")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
        }

        if(suggestText.isNullOrBlank().not()) {
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
        if(errorText.isNullOrBlank().not()) {
            Text(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(
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