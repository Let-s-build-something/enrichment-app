package com.squadris.squadris.compose.components.chips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squadris.squadris.compose.components.MinimalisticIcon
import com.squadris.squadris.compose.components.input.CustomTextField
import com.squadris.squadris.compose.theme.Colors
import com.squadris.squadris.compose.theme.LocalTheme
import com.squadris.squadris.compose.theme.SharedColors

private const val PERCENTAGE_OF_SCREEN_WIDTH = 0.3f

/** Chip for searching text */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchChip(
    modifier: Modifier = Modifier,
    imageVector: ImageVector = Icons.Outlined.Search,
    maxHeight: TextUnit = 32.sp,
    enabled: Boolean = true,
    text: String,
    isChecked: MutableState<Boolean> = mutableStateOf(false),
    isFieldError: MutableState<Boolean> = mutableStateOf(false),
    extraContent: @Composable RowScope.() -> Unit = {},
    onSearchOutput: (String) -> Unit,
    onSearchRequest: (String) -> Unit = onSearchOutput,
    onClick: () -> Unit
) {
    val isImeVisible = WindowInsets.isImeVisible
    val localDensity = LocalDensity.current
    val configuration = LocalConfiguration.current
    val focusManager = LocalFocusManager.current

    val focusRequester = remember {
        FocusRequester()
    }

    val isFocused = remember { mutableStateOf(false) }
    val fieldText = remember(text) {
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }

    LaunchedEffect(isChecked.value) {
        if(isChecked.value) {
            focusRequester.requestFocus()
        }
    }
    LaunchedEffect(isImeVisible) {
        if(isImeVisible.not() && fieldText.value.text.isEmpty()) {
            isChecked.value = false
        }
    }

    Row(
        modifier = modifier
            .padding(vertical = 8.dp, horizontal = if (isChecked.value) 8.dp else 4.dp)
            .height(with(localDensity) { maxHeight.toDp() })
            .widthIn(min = with(localDensity) { maxHeight.toDp() })
            .background(
                color = LocalTheme.current.colors.brandMain,
                shape = LocalTheme.current.shapes.chipShape
            )
            .focusRequester(focusRequester)
            .onFocusEvent { state ->
                if (fieldText.value.text.isEmpty() && isChecked.value && state.hasFocus.not() && isFocused.value) {
                    isChecked.value = false
                } else {
                    isFocused.value = state.hasFocus
                }
            }
            .clip(LocalTheme.current.shapes.chipShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple()
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(18.dp),
            imageVector = imageVector,
            tint = LocalTheme.current.colors.tetrial,
            contentDescription = null
        )
        Crossfade(
            modifier = Modifier.animateContentSize(),
            targetState = isChecked.value,
            label = "crossFadeSearchChip"
        ) { isInEdit ->
            if(isInEdit) {
                val controlColor by animateColorAsState(
                    when {
                        isFieldError.value -> SharedColors.RED_ERROR
                        isFocused.value -> LocalTheme.current.colors.secondary
                        else -> LocalTheme.current.colors.brandMain
                    },
                    label = "controlColorChange"
                )

                CustomTextField(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .widthIn(
                            min = configuration.screenWidthDp.times(
                                PERCENTAGE_OF_SCREEN_WIDTH
                            ).dp
                        )
                        .border(
                            if (isFocused.value) 1.dp else 0.25.dp,
                            controlColor,
                            LocalTheme.current.shapes.componentShape
                        ),
                    value = fieldText.value,
                    paddingValues = PaddingValues(start = 8.dp),
                    minLines = 1,
                    maxLines = 1,
                    textStyle = TextStyle(
                        color = LocalTheme.current.colors.primary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Start
                    ),
                    singleLine = true,
                    colors = LocalTheme.current.styles.textFieldColors,
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearchRequest(fieldText.value.text)
                            focusManager.clearFocus()
                        }
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    isError = isFieldError.value,
                    shape = LocalTheme.current.shapes.circularActionShape,
                    trailingIcon = {
                        MinimalisticIcon(
                            contentDescription = "Clear",
                            imageVector = Icons.Outlined.Clear,
                            tint = LocalTheme.current.colors.secondary
                        ) {
                            onSearchOutput("")
                            fieldText.value = TextFieldValue()
                            isChecked.value = false
                        }
                    },
                    onValueChange = { output ->
                        fieldText.value = output
                        onSearchOutput(output.text)
                        isFieldError.value = false
                    }
                )
            }else {
                AnimatedVisibility(visible = fieldText.value.text.isNotEmpty()) {
                    Text(
                        text = fieldText.value.text,
                        style = TextStyle(
                            color = LocalTheme.current.colors.primary,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        extraContent()
    }
}