package ui.conversation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_backspace
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
import augmy.interactive.shared.ui.components.input.DELAY_BETWEEN_TYPING_SHORT
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.theme.LocalTheme
import base.theme.Colors
import future_shared_module.ext.scalingClickable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Component displaying emojis with the ability to select any amount of them */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EmojiPicker(
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel,
    onEmojiSelected: (String) -> Unit,
    onBackSpace: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeHeight = viewModel.keyboardHeight
    val imePadding = WindowInsets.ime.getBottom(density)
    val coroutineScope = rememberCoroutineScope()
    val searchCoroutineScope = rememberCoroutineScope()

    val emojis = viewModel.emojis.collectAsState(initial = null)
    val areEmojisFiltered = viewModel.areEmojisFiltered.collectAsState()

    val isFilterFocused = remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = 2)

    DisposableEffect(null) {
        onDispose {
            viewModel.filterEmojis("")
        }
    }

    LaunchedEffect(isFilterFocused.value, imePadding) {
        if(isFilterFocused.value && imePadding > 0) {
            viewModel.additionalBottomPadding.animateTo(with(density) { 100.dp.toPx() })
        }else {
            viewModel.additionalBottomPadding.animateTo(0f)
        }
    }

    LaunchedEffect(imePadding, isFilterFocused.value) {
        delay(100)
        if(imeHeight - imePadding <= 0 && isFilterFocused.value.not()) {
            onDismissRequest()
        }
    }

    LaunchedEffect(gridState.firstVisibleItemScrollOffset) {
        keyboardController?.hide()
    }


    Box(
        modifier = Modifier
            .padding(bottom = with(density) {
                viewModel.additionalBottomPadding.value.toDp()
            })
            .height(with(density) { imeHeight.toDp() })
    ) {
        if(emojis.value != null) {
            Image(
                modifier = Modifier
                    .padding(bottom = 8.dp, end = 12.dp)
                    .zIndex(1f)
                    .scalingClickable {
                        onBackSpace()
                    }
                    .size(height = 42.dp, width = 64.dp)
                    .background(
                        color = LocalTheme.current.colors.brandMainDark,
                        shape = LocalTheme.current.shapes.rectangularActionShape
                    )
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .shadow(elevation = LocalTheme.current.styles.actionElevation)
                    .align(Alignment.BottomEnd),
                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = stringResource(Res.string.accessibility_backspace),
                colorFilter = ColorFilter.tint(color = Colors.GrayLight)
            )
            LazyVerticalGrid(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp)
                    .then(
                        if (imePadding > 0) Modifier else Modifier.animateContentSize()
                    ),
                columns = GridCells.Adaptive(minSize = 38.dp),
                state = gridState
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(16.dp))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EditFieldInput(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .requiredHeight(44.dp)
                                .fillMaxWidth(.65f)
                                .onFocusChanged {
                                    isFilterFocused.value = it.isFocused
                                },
                            value = "",
                            shape = LocalTheme.current.shapes.circularActionShape,
                            hint = stringResource(Res.string.action_filter_emojis),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            ),
                            paddingValues = PaddingValues(start = 16.dp),
                            minHeight = 38.dp,
                            leadingIcon = Icons.Outlined.Search,
                            onValueChange = {
                                searchCoroutineScope.coroutineContext.cancelChildren()
                                searchCoroutineScope.launch {
                                    delay(DELAY_BETWEEN_TYPING_SHORT)
                                    viewModel.filterEmojis(it.text)
                                }
                            },
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(16.dp))
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
                            val tooltipState = rememberBasicTooltipState(isPersistent = true)

                            BasicTooltipBox(
                                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                                tooltip = {
                                    LazyRow(
                                        modifier = Modifier
                                            .background(
                                                color = LocalTheme.current.colors.backgroundLight,
                                                shape = LocalTheme.current.shapes.componentShape
                                            )
                                            .padding(4.dp)
                                    ) {
                                        items(emojiData.emoji) { emoji ->
                                            Text(
                                                modifier = Modifier
                                                    .scalingClickable {
                                                        tooltipState.dismiss()
                                                        onEmojiSelected(emoji)
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
                                state = tooltipState
                            ) {
                                Box(
                                    modifier = Modifier
                                        .animateItem()
                                        .scalingClickable {
                                            if(emojiData.emoji.size > 1) {
                                                coroutineScope.launch {
                                                    tooltipState.show()
                                                }
                                            }else onEmojiSelected(emojiData.emoji.firstOrNull() ?: "")
                                        }
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
}
