package ui.conversation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_action_emojis
import augmy.composeapp.generated.resources.accessibility_action_keyboard
import augmy.composeapp.generated.resources.accessibility_cancel
import augmy.composeapp.generated.resources.accessibility_message_action_audio
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
import augmy.composeapp.generated.resources.conversation_reply_heading
import augmy.composeapp.generated.resources.conversation_reply_prefix_self
import augmy.composeapp.generated.resources.logo_pdf
import augmy.composeapp.generated.resources.logo_powerpoint
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.MaxModalWidthDp
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_LONG
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.theme.LocalTheme
import base.utils.MediaType
import base.utils.getBitmapFromFile
import base.utils.getMediaType
import data.io.social.network.conversation.ConversationMessageIO
import data.io.social.network.conversation.giphy.GifAsset
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
import ui.conversation.ConversationViewModel
import ui.conversation.components.emoji.MessageEmojiPanel
import ui.conversation.components.gif.MessageGifPanel

/** Horizontal panel for sending and managing a message, and attaching media to it */
@Composable
internal fun BoxScope.SendMessagePanel(
    modifier: Modifier = Modifier,
    keyboardMode: MutableState<Int>,
    replyToMessage: MutableState<ConversationMessageIO?>,
    scrollToMessage: (ConversationMessageIO) -> Unit,
    viewModel: ConversationViewModel
) {
    val screenSize = LocalScreenSize.current
    val density = LocalDensity.current
    val isDesktop = LocalDeviceType.current == WindowWidthSizeClass.Expanded || currentPlatform == PlatformType.Jvm
    val spacing = LocalTheme.current.shapes.betweenItemsSpace / 2
    val imeHeightPadding = WindowInsets.ime.getBottom(density)
    val keyboardController  = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val missingKeyboardHeight = remember { viewModel.keyboardHeight < 2 }
    val isDefaultMode = keyboardMode.value == ConversationKeyboardMode.Default.ordinal

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
    val gifAttached = remember {
        mutableStateOf<GifAsset?>(null)
    }
    val showMoreOptions = rememberSaveable {
        mutableStateOf(messageContent.value.text.isBlank())
    }
    val actionYCoordinate = rememberSaveable {
        mutableStateOf(-1f)
    }

    val bottomPadding = animateFloatAsState(
        targetValue = if (imeHeightPadding >= viewModel.keyboardHeight.div(2) || !isDefaultMode) {
            LocalTheme.current.shapes.betweenItemsSpace.value
        } else {
            with(density) { WindowInsets.navigationBars.getBottom(density).toDp().value }.coerceAtLeast(
                LocalTheme.current.shapes.betweenItemsSpace.value
            )
        },
        label = "bottomPadding",
        animationSpec = tween(durationMillis = 20, easing = LinearEasing)
    )
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
            mediaFiles = mediaAttached,
            anchorMessageId = replyToMessage.value?.id,
            gifAsset = gifAttached.value
        )
        mediaAttached.clear()
        messageContent.value = TextFieldValue()
        viewModel.savedMessage = ""
        replyToMessage.value = null
    }


    DisposableEffect(null) {
        onDispose {
            viewModel.savedMessage = messageContent.value.text
        }
    }

    LaunchedEffect(Unit) {
        if(missingKeyboardHeight) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }else focusRequester.captureFocus()
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

    LaunchedEffect(keyboardMode.value) {
        when(keyboardMode.value) {
            ConversationKeyboardMode.Default.ordinal -> viewModel.additionalBottomPadding.animateTo(0f)
            else -> keyboardController?.hide()
        }
    }

    OnBackHandler(enabled = imeHeightPadding > 0 || !isDefaultMode) {
        when {
            imeHeightPadding > 0 -> keyboardController?.hide()
            !isDefaultMode -> keyboardMode.value = ConversationKeyboardMode.Default.ordinal
        }
    }


    Column(
        modifier = (if(isDesktop) modifier else modifier.height(IntrinsicSize.Min))
            .animateContentSize(),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(LocalTheme.current.shapes.betweenItemsSpace))

        replyToMessage.value?.let { originalMessage ->
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            // reply indication
            Row(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .scalingClickable {
                        scrollToMessage(originalMessage)
                    }
                    .widthIn(max = MaxModalWidthDp.dp)
                    .fillMaxWidth()
                    .background(
                        color = LocalTheme.current.colors.backgroundDark,
                        shape = RoundedCornerShape(
                            topStart = LocalTheme.current.shapes.componentCornerRadius,
                            topEnd = LocalTheme.current.shapes.componentCornerRadius
                        )
                    )
                    .padding(top = 2.dp, bottom = 10.dp, start = 16.dp, end = 8.dp)
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(Res.string.conversation_reply_heading),
                    style = LocalTheme.current.styles.regular
                )
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 6.dp)
                        .weight(1f)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = if(originalMessage.authorPublicId == viewModel.currentUser.value?.publicId) {
                            stringResource(Res.string.conversation_reply_prefix_self)
                        }else originalMessage.user?.displayName?.plus(":") ?: "",
                        style = LocalTheme.current.styles.title.copy(fontSize = 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                        text = originalMessage.content ?: "",
                        style = LocalTheme.current.styles.regular.copy(fontSize = 14.sp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MinimalisticIcon(
                    imageVector = Icons.Outlined.Close,
                    tint = LocalTheme.current.colors.secondary,
                    onTap = {
                        replyToMessage.value = null
                    }
                )
            }
        }

        // media preview
        if(mediaAttached.isNotEmpty()) {
            val mediaListState = rememberLazyListState(
                initialFirstVisibleItemIndex = mediaAttached.lastIndex
            )

            Column(modifier = Modifier.padding(bottom = bottomPadding.value.dp)) {
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

        // message input and emojis if on desktop
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp)
                .padding(bottom = bottomPadding.value.dp)
                .then(if(isDefaultMode) Modifier.imePadding() else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditFieldInput(
                modifier = Modifier
                    .requiredHeight(44.dp)
                    .weight(1f)
                    .padding(start = 12.dp, end = spacing)
                    .onGloballyPositioned {
                        actionYCoordinate.value = it.positionInRoot().y
                    }
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                textValue = messageContent.value,
                trailingIcon = {
                    Crossfade(targetState = !isDefaultMode) { isMedia ->
                        Image(
                            modifier = Modifier.scalingClickable {
                                if(isMedia) {
                                    coroutineScope.launch {
                                        viewModel.additionalBottomPadding.animateTo(0f)
                                    }
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                    if(isDesktop) keyboardMode.value = ConversationKeyboardMode.Default.ordinal
                                }else {
                                    // hack to not close the emoji picker right away
                                    if(viewModel.keyboardHeight > 99) {
                                        viewModel.keyboardHeight += 1
                                        coroutineScope.launch {
                                            delay(200)
                                            viewModel.keyboardHeight -= 1
                                        }
                                    }else viewModel.keyboardHeight = screenSize.height / 3

                                    keyboardController?.hide()
                                    keyboardMode.value = ConversationKeyboardMode.Gif.ordinal
                                }
                            },
                            imageVector = if(isMedia) Icons.Outlined.Keyboard else Icons.Outlined.Mood,
                            contentDescription = stringResource(
                                if(isMedia) {
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
                tint = LocalTheme.current.colors.secondary
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
                Icon(
                    modifier = Modifier
                        .scalingClickable(enabled = !isBlank) {
                            if(!isBlank) sendMessage()
                        }
                        .padding(start = spacing)
                        .size(38.dp)
                        .background(
                            color = LocalTheme.current.colors.brandMainDark,
                            shape = LocalTheme.current.shapes.componentShape
                        )
                        .padding(6.dp),
                    imageVector = if(isBlank) Icons.Outlined.Mic else Icons.AutoMirrored.Outlined.Send,
                    contentDescription = stringResource(
                        if(isBlank) Res.string.accessibility_message_action_audio
                        else Res.string.accessibility_message_action_image
                    ),
                    tint = Color.White
                )
            }
        }

        Crossfade(targetState = keyboardMode.value) { mode ->
            when(mode) {
                ConversationKeyboardMode.Default.ordinal -> {}
                ConversationKeyboardMode.Emoji.ordinal -> {
                    MessageEmojiPanel(
                        visible = viewModel.keyboardHeight > 0f,
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
                            keyboardMode.value = ConversationKeyboardMode.Default.ordinal
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
                ConversationKeyboardMode.Gif.ordinal -> {
                    MessageGifPanel(
                        viewModel = viewModel,
                        onGifSelected = { gif ->
                            gifAttached.value = gif
                        },
                        onDismissRequest = {
                            keyboardMode.value = ConversationKeyboardMode.Default.ordinal
                        }
                    )
                }
            }
        }
    }

    // Has to be outside of message panel in order to lay over everything else
    if(messageContent.value.text.isBlank()) {
        PanelMicrophone(
            modifier = Modifier
                .offset(y = - (screenSize.height.dp - with(density) { actionYCoordinate.value.toDp() } - 44.dp))
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

// IOS does not support unicode special classes, must be done manually
private val REGEX_GRAPHEME_IOS = """
    (?:[\uD800-\uDBFF][\uDC00-\uDFFF][\uFE0F\u200D\u0300-\u036F\u1AB0-\u1AFF\u1DC0-\u1DFF\u20D0-\u20FF\uFE20-\uFE2F]*|[\u0020-\u007E]|[\u00A0-\uFFFF]|\s|.)[\u0300-\u036F\u1AB0-\u1AFF\u1DC0-\u1DFF\u20D0-\u20FF\uFE20-\uFE2F]*
""".trimIndent()

private val REGEX_GRAPHEME = if(currentPlatform == PlatformType.Native) REGEX_GRAPHEME_IOS else """\X"""
