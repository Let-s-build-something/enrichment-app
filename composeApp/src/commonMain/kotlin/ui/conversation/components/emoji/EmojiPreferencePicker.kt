package ui.conversation.components.emoji

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.emojis_preference_set_heading
import augmy.composeapp.generated.resources.emojis_preference_set_hint
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.theme.LocalTheme
import components.InfoHintBox
import data.io.social.network.conversation.EmojiData
import future_shared_module.ext.scalingClickable
import org.jetbrains.compose.resources.stringResource
import ui.conversation.ConversationViewModel

/**
 * Bottom sheet for displaying a picker and preference modifier of preferred emoji set
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPreferencePicker(
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel,
    onEmojiSelected: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    onDismissRequest: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val selectedIndex = remember {
        mutableStateOf(-1)
    }
    val showHint = remember {
        mutableStateOf(viewModel.showEmojiPreferenceHint)
    }

    val preferredEmojis = viewModel.preferredEmojis.collectAsState()


    SimpleModalBottomSheet(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        scrollEnabled = false
    ) {
        Box {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.emojis_preference_set_heading)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    preferredEmojis.value.forEachIndexed { index, emojiData ->
                        Text(
                            modifier = Modifier
                                .scalingClickable(scaleInto = .7f) {
                                    selectedIndex.value = index
                                }
                                .then(
                                    if(selectedIndex.value == index) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = LocalTheme.current.colors.primary,
                                            shape = CircleShape
                                        )
                                    }else Modifier
                                )
                                .padding(8.dp),
                            text = emojiData.emoji.firstOrNull() ?: "",
                            style = LocalTheme.current.styles.heading
                        )
                    }
                }
                AnimatedVisibility(showHint.value) {
                    InfoHintBox(
                        text = stringResource(Res.string.emojis_preference_set_hint),
                        onDismissRequest = {
                            viewModel.showEmojiPreferenceHint = false
                            showHint.value = false
                        }
                    )
                }
            }
        }
        EmojiPicker(
            viewModel = viewModel,
            onEmojiSelected = { emoji ->
                if(selectedIndex.value != -1) {
                    viewModel.updatePreferredEmojiSet(
                        preferredEmojis.value.toMutableList().apply {
                            set(selectedIndex.value, EmojiData(emoji))
                        }
                    )
                }else onEmojiSelected(emoji)
            }
        )
    }
}