package ui.network.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_invite
import augmy.composeapp.generated.resources.invite_message_explanation
import augmy.composeapp.generated.resources.invite_message_hint
import augmy.composeapp.generated.resources.invite_new_item_button
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.ComponentHeaderButton
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_SHORT
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import components.network.NetworkItemRow
import data.io.user.NetworkItemIO
import org.jetbrains.compose.resources.stringResource

/** Bottom sheet for adding users to a conversation */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun <T>AddToLauncher(
    modifier: Modifier = Modifier,
    key: Any?,
    defaultMessage: String? = null,
    multiSelect: Boolean,
    newItemHint: String? = null,
    isLoading: Boolean = false,
    heading: String,
    items: List<T>?,
    mapToNetworkItem: (T) -> NetworkItemIO,
    onInvite: (items: List<T>, message: String, newName: String?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val checkedItems = remember { mutableStateListOf<T>() }
    val newItemState = if(newItemHint != null) {
        remember(key) {
            TextFieldState(
                initialText = defaultMessage ?: "",
                initialSelection = TextRange(defaultMessage?.length ?: 0)
            )
        }
    }else null

    SimpleModalBottomSheet(
        modifier = modifier,
        scrollEnabled = false,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        onDismissRequest = onDismissRequest
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 6.dp),
            text = heading,
            style = LocalTheme.current.styles.title
        )
        LazyColumn(
            modifier = Modifier
                .padding(top = 8.dp)
                .animateContentSize()
                .fillMaxWidth()
        ) {
            stickyHeader {
                androidx.compose.animation.AnimatedVisibility(
                    visible = checkedItems.isNotEmpty() || newItemState?.text?.isNotBlank() == true,
                    enter = slideInVertically (
                        initialOffsetY = { -it },
                        animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
                    ),
                    exit = slideOutVertically (
                        targetOffsetY = { -it },
                        animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
                    )
                ) {

                    val messageState = remember(key) {
                        TextFieldState(
                            initialText = defaultMessage ?: "",
                            initialSelection = TextRange(defaultMessage?.length ?: 0)
                        )
                    }

                    Row(
                        modifier = Modifier.animateItem(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomTextField(
                            modifier = Modifier
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 6.dp
                                )
                                .background(
                                    LocalTheme.current.colors.backgroundLight,
                                    shape = LocalTheme.current.shapes.componentShape
                                )
                                .weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Send
                            ),
                            suggestText = stringResource(Res.string.invite_message_explanation),
                            hint = stringResource(Res.string.invite_message_hint),
                            state = messageState,
                            onKeyboardAction = {
                                onInvite(
                                    checkedItems.toList(),
                                    messageState.text.toString(),
                                    newItemState?.text?.toString()
                                )
                            },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            shape = LocalTheme.current.shapes.componentShape
                        )
                        BrandHeaderButton(
                            text = stringResource(Res.string.button_invite),
                            isLoading = isLoading
                        ) {
                            onInvite(
                                checkedItems.toList(),
                                messageState.text.toString(),
                                newItemState?.text?.toString()
                            )
                        }
                    }
                }
            }
            newItemState?.let { state ->
                item {
                    val newItem = remember(key) {
                        mutableStateOf(false)
                    }
                    LaunchedEffect(state.text) {
                        if(state.text.isNotBlank()) {
                            checkedItems.clear()
                        }else newItem.value = false
                    }

                    Crossfade(targetState = newItem.value) { isNewItem ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if(isNewItem) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    CustomTextField(
                                        modifier = Modifier
                                            .padding(
                                                horizontal = 8.dp,
                                                vertical = 6.dp
                                            )
                                            .background(
                                                LocalTheme.current.colors.backgroundDark,
                                                shape = LocalTheme.current.shapes.componentShape
                                            )
                                            .weight(1f),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Done
                                        ),
                                        showBorders = false,
                                        hint = newItemHint,
                                        state = state,
                                        lineLimits = TextFieldLineLimits.SingleLine,
                                        shape = LocalTheme.current.shapes.componentShape
                                    )
                                    Checkbox(
                                        checked = state.text.isNotBlank(),
                                        onCheckedChange = { checked ->
                                            if(!checked) {
                                                newItemState.setTextAndPlaceCursorAtEnd("")
                                                newItem.value = false
                                            }
                                        },
                                        colors = LocalTheme.current.styles.checkBoxColorsDefault
                                    )
                                }
                            }else {
                                ComponentHeaderButton(
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth(.7f)
                                        .align(Alignment.Center),
                                    text = stringResource(Res.string.invite_new_item_button),
                                    endImageVector = Icons.Outlined.Add
                                ) {
                                    newItem.value = true
                                }
                            }
                        }
                    }
                }
            }
            items(items = items.orEmpty()) { data ->
                val onCheck = {
                    when {
                        checkedItems.contains(data) -> checkedItems.remove(data)
                        checkedItems.isEmpty() || multiSelect -> checkedItems.add(data)
                        else -> {
                            newItemState?.setTextAndPlaceCursorAtEnd("")
                            checkedItems.remove(data)
                            checkedItems.add(data)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NetworkItemRow(
                        modifier = Modifier
                            .scalingClickable(
                                scaleInto = .95f,
                                onTap = { onCheck() }
                            )
                            .weight(1f),
                        data = mapToNetworkItem(data)
                    )
                    Checkbox(
                        modifier = Modifier.wrapContentWidth(),
                        checked = checkedItems.contains(data),
                        onCheckedChange = { onCheck() },
                        colors = LocalTheme.current.styles.checkBoxColorsDefault
                    )
                }
            }
            item {
                Spacer(Modifier.height(50.dp))
            }
        }
    }
}