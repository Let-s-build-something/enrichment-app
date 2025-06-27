package ui.conversation.components.emoji

import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BasicTooltipDefaults.GlobalMutatorMutex
import androidx.compose.foundation.BasicTooltipState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_emojis_history
import augmy.composeapp.generated.resources.action_filter_emojis
import augmy.composeapp.generated.resources.emojis_activities
import augmy.composeapp.generated.resources.emojis_animals_and_nature
import augmy.composeapp.generated.resources.emojis_component
import augmy.composeapp.generated.resources.emojis_flags
import augmy.composeapp.generated.resources.emojis_food_and_drink
import augmy.composeapp.generated.resources.emojis_objects
import augmy.composeapp.generated.resources.emojis_people_body
import augmy.composeapp.generated.resources.emojis_smileys_and_emotion
import augmy.composeapp.generated.resources.emojis_symbols
import augmy.composeapp.generated.resources.emojis_travel_and_places
import augmy.composeapp.generated.resources.error_general
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.components.input.DELAY_BETWEEN_TYPING_SHORT
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.social.network.conversation.EmojiData
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.compose.resources.stringResource
import ui.conversation.ConversationModel
import ui.conversation.components.emoji.EmojiUseCase.Companion.EMOJIS_HISTORY_GROUP

/** Component displaying emojis with the ability to select one and filter them */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPicker(
    modifier: Modifier = Modifier,
    viewModel: ConversationModel,
    onEmojiSelected: (String) -> Unit,
    isFilterFocused: MutableState<Boolean> = remember { mutableStateOf(false) },
    gridState: LazyGridState = rememberLazyGridState()
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val searchCoroutineScope = rememberCoroutineScope()

    val searchState = remember { TextFieldState() }
    val areEmojisFiltered = viewModel.areEmojisFiltered.collectAsState()
    val emojis = viewModel.emojis.collectAsState(initial = null)

    DisposableEffect(null) {
        onDispose {
            isFilterFocused.value = false
        }
    }

    LaunchedEffect(searchState.text) {
        searchCoroutineScope.launch {
            searchCoroutineScope.coroutineContext.cancelChildren()
            searchCoroutineScope.launch {
                delay(DELAY_BETWEEN_TYPING_SHORT)
                viewModel.filterEmojis(searchState.text)
            }
        }
    }

    if(emojis.value != null) {
        LazyVerticalGrid(
            modifier = modifier,
            columns = GridCells.Adaptive(minSize = 38.dp),
            state = gridState
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(12.dp))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CustomTextField(
                        modifier = Modifier
                            .height(44.dp)
                            .fillMaxWidth(.65f),
                        isFocused = isFilterFocused,
                        shape = LocalTheme.current.shapes.rectangularActionShape,
                        hint = stringResource(Res.string.action_filter_emojis),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        paddingValues = PaddingValues(start = 16.dp),
                        prefixIcon = Icons.Outlined.Search,
                        state = searchState,
                        isClearable = true,
                        colors = LocalTheme.current.styles.textFieldColors.copy(
                            focusedTextColor = LocalTheme.current.colors.disabled,
                            unfocusedTextColor = LocalTheme.current.colors.disabled,
                            focusedContainerColor = LocalTheme.current.colors.backgroundDark,
                            unfocusedContainerColor = LocalTheme.current.colors.backgroundDark
                        )
                    )
                }
            }
            emojis.value?.forEach { category ->
                if(category.second.isNotEmpty()) {
                    if(!areEmojisFiltered.value) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                                text = stringResource(when(category.first) {
                                    "smileys-emotion" -> Res.string.emojis_smileys_and_emotion
                                    "people-body" -> Res.string.emojis_people_body
                                    "component" -> Res.string.emojis_component
                                    "animals-nature" -> Res.string.emojis_animals_and_nature
                                    "activities" -> Res.string.emojis_activities
                                    "objects" -> Res.string.emojis_objects
                                    EMOJIS_HISTORY_GROUP -> Res.string.accessibility_emojis_history
                                    "food-drink" -> Res.string.emojis_food_and_drink
                                    "symbols" -> Res.string.emojis_symbols
                                    "travel-places" -> Res.string.emojis_travel_and_places
                                    "flags" -> Res.string.emojis_flags
                                    else -> Res.string.error_general
                                }),
                                style = LocalTheme.current.styles.subheading
                            )
                        }
                    }
                    items(
                        items = category.second,
                        key = { it.name }
                    ) { emojiData ->
                        EmojiImpl(
                            emojiData = emojiData,
                            onEmojiSelected = { content, name ->
                                viewModel.noteEmojiSelection(EmojiData(mutableListOf(content), name))
                                onEmojiSelected(content)
                            },
                            coroutineScope = coroutineScope
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(
                    50.dp + with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
                ))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
private fun LazyGridItemScope.EmojiImpl(
    emojiData: EmojiData,
    onEmojiSelected: (content: String, name: String) -> Unit,
    coroutineScope: CoroutineScope,
    gridState: LazyGridState = rememberLazyGridState()
) {
    val state = remember(emojiData.name) {
        object: BasicTooltipState {
            override var isVisible by mutableStateOf(false)
            override val isPersistent: Boolean = true
            private val mutatorMutex: MutatorMutex = GlobalMutatorMutex

            private var job: (CancellableContinuation<Unit>)? = null

            override suspend fun show(mutatePriority: MutatePriority) {
                val cancellableShow: suspend () -> Unit = {
                    suspendCancellableCoroutine { continuation ->
                        isVisible = true
                        job = continuation
                    }
                }
                mutatorMutex.mutate(mutatePriority) {
                    try { cancellableShow() } finally { isVisible = false }
                }
            }
            override fun dismiss() {}
            override fun onDispose() {
                isVisible = false
                job?.cancel()
            }
        }
    }

    LaunchedEffect(gridState.firstVisibleItemScrollOffset) {
        if(state.isVisible) state.onDispose()
    }

    BasicTooltipBox(
        focusable = false,
        enableUserInput = false,
        state = state,
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            LazyRow(
                modifier = Modifier
                    .background(
                        color = LocalTheme.current.colors.component,
                        shape = LocalTheme.current.shapes.componentShape
                    )
                    .padding(4.dp)
            ) {
                itemsIndexed(
                    emojiData.emoji,
                    key = { _, emoji -> emoji  }
                ) { index, emoji ->
                    Text(
                        modifier = Modifier
                            .scalingClickable {
                                onEmojiSelected(emoji, "${emojiData.name}_$index")
                                coroutineScope.launch {
                                    state.onDispose()
                                }
                            }
                            .size(46.dp)
                            .padding(4.dp)
                            .animateItem(),
                        text = emoji,
                        style = LocalTheme.current.styles.heading.copy(
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        },
    ) { Box(Modifier) }

    Box(
        modifier = Modifier
            .scalingClickable {
                if(emojiData.emoji.size > 1) {
                    coroutineScope.launch {
                        state.show(MutatePriority.PreventUserInput)
                    }
                }else onEmojiSelected(emojiData.emoji.firstOrNull() ?: "", emojiData.name)
            }
            .animateItem()
    ) {
        Text(
            modifier = Modifier
                .size(46.dp)
                .padding(4.dp),
            text = emojiData.emoji.firstOrNull() ?: "",
            style = LocalTheme.current.styles.heading.copy(
                textAlign = TextAlign.Center
            )
        )
        if(emojiData.emoji.size > 1) {
            Icon(
                modifier = Modifier
                    .rotate(-45f)
                    .size(16.dp)
                    .align(Alignment.BottomEnd),
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = LocalTheme.current.colors.secondary
            )
        }
    }
}
