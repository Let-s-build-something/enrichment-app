package ui.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_action_emojis
import augmy.composeapp.generated.resources.accessibility_action_keyboard
import augmy.composeapp.generated.resources.accessibility_cancel
import augmy.composeapp.generated.resources.accessibility_message_action_file
import augmy.composeapp.generated.resources.accessibility_message_action_image
import augmy.composeapp.generated.resources.accessibility_message_audio
import augmy.composeapp.generated.resources.accessibility_message_file
import augmy.composeapp.generated.resources.accessibility_message_image
import augmy.composeapp.generated.resources.accessibility_message_more_options
import augmy.composeapp.generated.resources.accessibility_message_pdf
import augmy.composeapp.generated.resources.accessibility_message_presentation
import augmy.composeapp.generated.resources.accessibility_message_text
import augmy.composeapp.generated.resources.account_picture_pick_title
import augmy.composeapp.generated.resources.conversation_attached
import augmy.composeapp.generated.resources.logo_pdf
import augmy.composeapp.generated.resources.logo_powerpoint
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_LONG
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.theme.LocalTheme
import base.theme.Colors
import base.utils.MediaType
import base.utils.getBitmapFromFile
import base.utils.getMediaType
import future_shared_module.ext.scalingClickable
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.extension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Horizontal panel for sending and managing a message, and attaching media to it */
@Composable
internal fun BoxScope.SendMessagePanel(
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel
) {
    val screenSize = LocalScreenSize.current
    val density = LocalDensity.current
    val spacing = LocalTheme.current.shapes.betweenItemsSpace / 2
    val imeHeightPadding = WindowInsets.ime.getBottom(density)
    val keyboardController  = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val missingKeyboardHeight = remember { viewModel.keyboardHeight == 0 }

    val messageContent = remember {
        mutableStateOf(
            TextFieldValue(
                viewModel.savedMessage,
                TextRange(viewModel.savedMessage.length)
            )
        )
    }
    val mediaAttached = remember {
        mutableStateListOf<PlatformFile>()
    }
    val showMoreOptions = rememberSaveable {
        mutableStateOf(messageContent.value.text.isBlank())
    }
    val isEmojiPickerVisible = rememberSaveable {
        mutableStateOf(false)
    }

    val showMoreIconRotation = animateFloatAsState(
        targetValue = if(showMoreOptions.value) 0f else 135f,
        label = "showMoreIconRotation",
        animationSpec = tween(durationMillis = DEFAULT_ANIMATION_LENGTH_LONG)
    )
    val launcherImageVideo = rememberFilePickerLauncher(
        type = PickerType.ImageAndVideo,
        mode = PickerMode.Multiple(maxItems = MAX_ITEMS_SELECTED),
        title = stringResource(Res.string.account_picture_pick_title)
    ) { files ->
        if(!files.isNullOrEmpty()) {
            coroutineScope.launch(Dispatchers.Default) {
                mediaAttached.addAll(
                    index = 0,
                    files.filter { newFile ->
                        mediaAttached.none { it.name == newFile.name }
                    }
                )
            }
        }
    }
    val launcherFile = rememberFilePickerLauncher(
        type = PickerType.File(),
        mode = PickerMode.Multiple(maxItems = MAX_ITEMS_SELECTED),
        title = stringResource(Res.string.account_picture_pick_title)
    ) { files ->
        if(!files.isNullOrEmpty()) {
            coroutineScope.launch(Dispatchers.Default) {
                mediaAttached.addAll(
                    index = 0,
                    files.filter { newFile ->
                        mediaAttached.none { it.name == newFile.name }
                    }
                )
            }
        }
    }

    val sendMessage = {
        viewModel.sendMessage(
            content = messageContent.value.text,
            mediaFiles = mediaAttached
        )
        mediaAttached.clear()
        messageContent.value = TextFieldValue()
        viewModel.savedMessage = ""
    }


    DisposableEffect(null) {
        onDispose {
            viewModel.savedMessage = messageContent.value.text
        }
    }

    LaunchedEffect(Unit) {
        if(missingKeyboardHeight) {
            focusRequester.requestFocus()
        }
    }

    if(missingKeyboardHeight) {
        LaunchedEffect(imeHeightPadding) {
            if(missingKeyboardHeight) {
                imeHeightPadding.let { imeHeight ->
                    if(imeHeight > viewModel.keyboardHeight) {
                        viewModel.keyboardHeight = imeHeight
                    }
                }
            }
        }
    }

    LaunchedEffect(isEmojiPickerVisible.value) {
        if(isEmojiPickerVisible.value) {
            keyboardController?.hide()
        }else {
            viewModel.additionalBottomPadding.animateTo(0f)
            keyboardController?.show()
        }
    }

    OnBackHandler(enabled = imeHeightPadding > 0 || isEmojiPickerVisible.value) {
        when {
            imeHeightPadding > 0 -> keyboardController?.hide()
            isEmojiPickerVisible.value -> isEmojiPickerVisible.value = false
        }
    }


    Column(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .animateContentSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Spacer(Modifier.height(LocalTheme.current.shapes.betweenItemsSpace))

        if(mediaAttached.isNotEmpty()) {
            val mediaListState = rememberLazyListState(
                initialFirstVisibleItemIndex = mediaAttached.lastIndex
            )

            Column(
                modifier = Modifier
                    .then(if(!isEmojiPickerVisible.value) Modifier.imePadding() else Modifier)
            ) {
                Text(
                    modifier = Modifier.padding(
                        bottom = 8.dp,
                        start = LocalTheme.current.shapes.betweenItemsSpace * 2
                    ),
                    text = stringResource(Res.string.conversation_attached),
                    style = LocalTheme.current.styles.regular
                )

                val previewHeight = screenSize.height.div(7).coerceAtMost(MEDIA_MAX_HEIGHT_DP)
                LazyRow(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .fillMaxWidth()
                        .height(previewHeight.dp)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                coroutineScope.launch {
                                    mediaListState.scrollBy(-delta)
                                }
                            }
                        ),
                    reverseLayout = true,
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    state = mediaListState
                ) {
                    item {
                        Spacer(Modifier)
                    }
                    itemsIndexed(
                        items = mediaAttached,
                        key = { _, media -> media.name }
                    ) { index, media ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(min = previewHeight.times(.75).dp)
                                .animateItem(),
                            contentAlignment = Alignment.Center
                        ) {
                            MinimalisticIcon(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .zIndex(1f)
                                    .background(
                                        color = LocalTheme.current.colors.backgroundDark,
                                        shape = LocalTheme.current.shapes.componentShape
                                    ),
                                tint = LocalTheme.current.colors.secondary,
                                imageVector = Icons.Outlined.Close,
                                onTap = {
                                    if(index < mediaAttached.size) {
                                        mediaAttached.removeAt(index)
                                    }
                                }
                            )

                            val contentPreviewModifier = Modifier
                                .widthIn(max = previewHeight.times(1.25).dp)
                                .fillMaxHeight()
                                .background(
                                    color = LocalTheme.current.colors.brandMainDark,
                                    shape = LocalTheme.current.shapes.componentShape
                                )
                                .padding(vertical = 3.dp, horizontal = 4.dp)
                                .clip(LocalTheme.current.shapes.rectangularActionShape)

                            // content preview
                            when(val mediaType = getMediaType(media.extension)) {
                                MediaType.IMAGE -> {
                                    val bitmap = remember {
                                        mutableStateOf<ImageBitmap?>(null)
                                    }

                                    LaunchedEffect(Unit) {
                                        bitmap.value = getBitmapFromFile(media)
                                    }

                                    bitmap.value?.let { value ->
                                        Image(
                                            modifier = contentPreviewModifier,
                                            bitmap = value,
                                            contentDescription = stringResource(Res.string.accessibility_message_image)
                                        )
                                    }
                                }
                                MediaType.VIDEO -> {

                                }
                                else -> {
                                    Column(
                                        modifier = Modifier.width(IntrinsicSize.Min),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val iconModifier = Modifier.size(previewHeight.div(2).dp)
                                        when(mediaType) {
                                            MediaType.PDF -> {
                                                Image(
                                                    modifier = iconModifier,
                                                    painter = painterResource(Res.drawable.logo_pdf),
                                                    contentDescription = stringResource(Res.string.accessibility_message_pdf)
                                                )
                                            }
                                            MediaType.AUDIO -> {
                                                Icon(
                                                    modifier = iconModifier,
                                                    imageVector = Icons.Outlined.GraphicEq,
                                                    tint = LocalTheme.current.colors.secondary,
                                                    contentDescription = stringResource(Res.string.accessibility_message_audio)
                                                )
                                            }
                                            MediaType.TEXT -> {
                                                Icon(
                                                    modifier = iconModifier,
                                                    imageVector = Icons.Outlined.Description,
                                                    tint = LocalTheme.current.colors.secondary,
                                                    contentDescription = stringResource(Res.string.accessibility_message_text)
                                                )
                                            }
                                            MediaType.PRESENTATION -> {
                                                Image(
                                                    modifier = iconModifier,
                                                    painter = painterResource(Res.drawable.logo_powerpoint),
                                                    contentDescription = stringResource(Res.string.accessibility_message_presentation)
                                                )
                                            }
                                            else -> {
                                                Icon(
                                                    modifier = iconModifier,
                                                    imageVector = Icons.Outlined.FilePresent,
                                                    tint = LocalTheme.current.colors.secondary,
                                                    contentDescription = stringResource(Res.string.accessibility_message_file)
                                                )
                                            }
                                        }
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            text = media.name,
                                            style = LocalTheme.current.styles.regular.copy(
                                                textAlign = TextAlign.Center
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp)
                .then(if(!isEmojiPickerVisible.value) Modifier.imePadding() else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditFieldInput(
                modifier = Modifier
                    .requiredHeight(44.dp)
                    .weight(1f)
                    .padding(end = spacing)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                textValue = messageContent.value,
                trailingIcon = {
                    Crossfade(targetState = isEmojiPickerVisible.value) { isEmoji ->
                        Image(
                            modifier = Modifier.scalingClickable {
                                if(isEmoji) {
                                    coroutineScope.launch {
                                        viewModel.additionalBottomPadding.animateTo(0f)
                                    }
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }else {
                                    keyboardController?.hide()
                                    isEmojiPickerVisible.value = true

                                    // hack to not close the emoji picker right away
                                    viewModel.keyboardHeight += 1
                                    coroutineScope.launch {
                                        delay(200)
                                        viewModel.keyboardHeight -= 1
                                    }
                                }
                            },
                            imageVector = if(isEmoji) Icons.Outlined.Keyboard else Icons.Outlined.Mood,
                            contentDescription = stringResource(
                                if(isEmoji) {
                                    Res.string.accessibility_action_keyboard
                                }else Res.string.accessibility_action_emojis
                            ),
                            colorFilter = ColorFilter.tint(LocalTheme.current.colors.secondary)
                        )
                    }
                },
                minHeight = 44.dp,
                shape = LocalTheme.current.shapes.componentShape,
                paddingValues = PaddingValues(start = 16.dp),
                keyboardActions = KeyboardActions(
                    onSend = {
                        sendMessage()
                    }
                ),
                value = viewModel.savedMessage,
                onValueChange = {
                    showMoreOptions.value = false
                    messageContent.value = it
                }
            )

            Icon(
                modifier = Modifier
                    .scalingClickable {
                        showMoreOptions.value = !showMoreOptions.value
                    }
                    .size(38.dp)
                    .padding(6.dp)
                    .rotate(showMoreIconRotation.value),
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(
                    if(showMoreOptions.value) Res.string.accessibility_cancel
                    else Res.string.accessibility_message_more_options
                ),
                tint = Colors.GrayLight
            )
            AnimatedVisibility(showMoreOptions.value) {
                Row(
                    modifier = Modifier.padding(start = spacing),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Icon(
                        modifier = Modifier
                            .scalingClickable {
                                launcherFile.launch()
                            }
                            .size(38.dp)
                            .background(
                                color = LocalTheme.current.colors.brandMainDark,
                                shape = LocalTheme.current.shapes.componentShape
                            )
                            .padding(6.dp),
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = stringResource(Res.string.accessibility_message_action_file),
                        tint = Color.White
                    )
                    Icon(
                        modifier = Modifier
                            .scalingClickable {
                                launcherImageVideo.launch()
                            }
                            .size(38.dp)
                            .background(
                                color = LocalTheme.current.colors.brandMainDark,
                                shape = LocalTheme.current.shapes.componentShape
                            )
                            .padding(6.dp),
                        imageVector = Icons.Outlined.Image,
                        contentDescription = stringResource(Res.string.accessibility_message_action_image),
                        tint = Color.White
                    )
                }
            }

            // space for the microphone action
            Crossfade(targetState = messageContent.value.text.isBlank()) { isBlank ->
                if(isBlank) {
                    Spacer(Modifier.width(38.dp + spacing))
                }else {
                    Icon(
                        modifier = Modifier
                            .scalingClickable {
                                sendMessage()
                            }
                            .padding(start = spacing)
                            .size(38.dp)
                            .background(
                                color = LocalTheme.current.colors.brandMainDark,
                                shape = LocalTheme.current.shapes.componentShape
                            )
                            .padding(6.dp),
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = stringResource(Res.string.accessibility_message_action_image),
                        tint = Color.White
                    )
                }
            }
        }

        if(isEmojiPickerVisible.value && viewModel.keyboardHeight > 0f) {
            EmojiPicker(
                viewModel = viewModel,
                onEmojiSelected = { emoji ->
                    showMoreOptions.value = false

                    val newContent = buildString {
                        // before selection
                        append(
                            messageContent.value.text.subSequence(
                                0, messageContent.value.selection.start
                            )
                        )
                        // selection
                        append(emoji)
                        // after selection
                        append(
                            messageContent.value.text.subSequence(
                                messageContent.value.selection.end,
                                messageContent.value.text.length
                            )
                        )
                    }
                    messageContent.value = TextFieldValue(
                        text = newContent,
                        selection = TextRange(
                            messageContent.value.selection.start + emoji.length.coerceAtMost(newContent.length)
                        )
                    )
                },
                onDismissRequest = {
                    isEmojiPickerVisible.value = false
                },
                onBackSpace = {
                    showMoreOptions.value = false

                    val isRangeRemoval = messageContent.value.selection.start != messageContent.value.selection.end

                    if(isRangeRemoval) {
                        messageContent.value = TextFieldValue(
                            text = buildString {
                                // before selection
                                append(
                                    messageContent.value.text.subSequence(
                                        startIndex = 0,
                                        endIndex = messageContent.value.selection.start
                                    ).toString()
                                )
                                // after selection
                                append(
                                    messageContent.value.text.subSequence(
                                        startIndex = messageContent.value.selection.end,
                                        endIndex = messageContent.value.text.length
                                    )
                                )
                            },
                            selection = TextRange(messageContent.value.selection.start)
                        )
                    }else {
                        val modifiedPrefix = removeUnicodeCharacter(
                            text = messageContent.value.text.subSequence(
                                0, messageContent.value.selection.start
                            ).toString(),
                            index = messageContent.value.selection.start
                        )

                        val newContent = buildString {
                            // before selection
                            append(modifiedPrefix)
                            // after selection
                            append(
                                messageContent.value.text.subSequence(
                                    messageContent.value.selection.end,
                                    messageContent.value.text.length
                                )
                            )
                        }
                        messageContent.value = TextFieldValue(
                            text = newContent,
                            selection = TextRange(modifiedPrefix.length)
                        )
                    }
                }
            )
        }
    }

    // Has to be outside of message panel in order to lay over everything else
    if(messageContent.value.text.isBlank()) {
        PanelMicrophone(
            modifier = Modifier
                .then(
                    if(isEmojiPickerVisible.value) {
                        Modifier.padding(
                            bottom = with(density) { viewModel.keyboardHeight.plus(
                                viewModel.additionalBottomPadding.value
                            ) .toDp() }
                        )
                    }else Modifier.imePadding()
                )
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .align(Alignment.BottomEnd),
            onSaveRequest = { byteArray ->
                viewModel.sendAudioMessage(byteArray)
            }
        )
    }
}

private fun removeUnicodeCharacter(text: String, index: Int): String {
    val prefix = text.substring(0, index)
    val suffix = REGEX_GRAPHEME.toRegex().findAll(prefix).lastOrNull()?.value ?: ""


    return prefix.removeSuffix(suffix) + text.substring(index)
}


/** Maximum amount of files, images, and videos to be selected and sent within a singular message */
private const val MAX_ITEMS_SELECTED = 20

private const val MEDIA_MAX_HEIGHT_DP = 250

private const val REGEX_GRAPHEME = """\X"""
