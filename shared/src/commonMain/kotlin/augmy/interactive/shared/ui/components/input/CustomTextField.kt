package augmy.interactive.shared.ui.components.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors

/**
 * Brand specific customized [BasicTextField] supporting error state via [errorText], [suggestText], [isCorrect], and trailing icon
 */
@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
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
    showBorders: Boolean = true,
    colors: TextFieldColors = LocalTheme.current.styles.textFieldColors,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    shape: Shape = LocalTheme.current.shapes.rectangularActionShape,
    errorText: String? = null,
    hint: String? = null,
    suggestText: String? = null,
    isCorrect: Boolean = false,
    enabled: Boolean = true
) {
    val focusRequester = remember(state) { FocusRequester() }
    val isFocused = remember(state.text) { mutableStateOf(false) }
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

    Column(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Min)
            .animateContentSize()
    ) {
        Row(
            Modifier
                .fillMaxSize()
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
            Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .padding(paddingValues),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            isFocused.value = it.isFocused
                        },
                    state = state,
                    cursorBrush = Brush.linearGradient(listOf(textStyle.color, textStyle.color)),
                    textStyle = textStyle,
                    lineLimits = lineLimits,
                    keyboardOptions = keyboardOptions,
                    onKeyboardAction = onKeyboardAction
                )
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
            trailingIcon?.invoke()
            Spacer(Modifier.width(16.dp))
        }

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