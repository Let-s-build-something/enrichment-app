package ui.conversation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_action_emojis
import augmy.composeapp.generated.resources.accessibility_action_keyboard
import augmy.composeapp.generated.resources.accessibility_cancel
import augmy.composeapp.generated.resources.accessibility_message_action_audio
import augmy.composeapp.generated.resources.accessibility_message_action_file
import augmy.composeapp.generated.resources.accessibility_message_action_image
import augmy.composeapp.generated.resources.accessibility_message_more_options
import augmy.composeapp.generated.resources.account_picture_pick_title
import augmy.composeapp.generated.resources.conversation_attached
import augmy.interactive.shared.ext.contentReceiver
import augmy.interactive.shared.ext.horizontallyDraggable
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.MaxModalWidthDp
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_LONG
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavigationNode
import base.utils.MediaType
import base.utils.getMediaType
import coil3.toUri
import data.io.social.network.conversation.ConversationMessageIO
import data.io.social.network.conversation.giphy.GifAsset
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ui.conversation.ConversationViewModel
import ui.conversation.components.audio.PanelMicrophone
import ui.conversation.components.gif.GifImage

/** Horizontal panel for sending and managing a message, and attaching media to it */
@OptIn(ExperimentalFoundationApi::class)
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
    val navController = LocalNavController.current
    val isDesktop = LocalDeviceType.current == WindowWidthSizeClass.Expanded || currentPlatform == PlatformType.Jvm
    val spacing = LocalTheme.current.shapes.betweenItemsSpace / 2
    val imeHeightPadding = WindowInsets.ime.getBottom(density)
    val keyboardController  = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val isDefaultMode = keyboardMode.value == ConversationKeyboardMode.Default.ordinal

    val keyboardHeight = viewModel.keyboardHeight.collectAsState()
    val savedMessage = viewModel.savedMessage.collectAsState()
    val messageState = remember(savedMessage.value) {
        TextFieldState(
            initialText = savedMessage.value,
            initialSelection = TextRange(savedMessage.value.length)
        )
    }
    val missingKeyboardHeight = remember { keyboardHeight.value < 2 }
    val mediaAttached = remember {
        mutableStateListOf<PlatformFile>()
    }
    val urlsAttached = remember {
        mutableStateListOf<String>()
    }
    val gifAttached = remember {
        mutableStateOf<GifAsset?>(null)
    }
    val showMoreOptions = rememberSaveable {
        mutableStateOf(messageState.text.isBlank())
    }
    val actionYCoordinate = rememberSaveable {
        mutableStateOf(-1f)
    }

    val isContentEmpty = messageState.text.isBlank()
            && mediaAttached.isEmpty()
            && urlsAttached.isEmpty()
            && gifAttached.value == null

    val bottomPadding = animateFloatAsState(
        targetValue = if (imeHeightPadding >= keyboardHeight.value.div(2) || !isDefaultMode) {
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
            content = messageState.text.toString(),
            mediaFiles = mediaAttached.toList(),
            anchorMessage = replyToMessage.value,
            gifAsset = gifAttached.value,
            mediaUrls = urlsAttached
        )
        mediaAttached.clear()
        keyboardMode.value = ConversationKeyboardMode.Default.ordinal
        messageState.clearText()
        viewModel.saveMessage(null)
        replyToMessage.value = null
        gifAttached.value = null
    }


    DisposableEffect(null) {
        onDispose {
            viewModel.saveMessage(messageState.text.toString())
        }
    }

    LaunchedEffect(savedMessage.value) {
        if(missingKeyboardHeight || savedMessage.value.isNotBlank()) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }else focusRequester.captureFocus()
    }

    if(missingKeyboardHeight) {
        LaunchedEffect(imeHeightPadding) {
            if(missingKeyboardHeight) {
                imeHeightPadding.let { imeHeight ->
                    if(imeHeight > keyboardHeight.value) {
                        viewModel.setKeyboardHeight(imeHeight)
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

    LaunchedEffect(messageState.text) {
        if(messageState.text.isNotBlank()) {
            showMoreOptions.value = false
        }
    }

    OnBackHandler(enabled = imeHeightPadding > 0 || !isDefaultMode) {
        when {
            imeHeightPadding > 0 -> keyboardController?.hide()
            !isDefaultMode -> keyboardMode.value = ConversationKeyboardMode.Default.ordinal
        }
    }


    Column(
        modifier = modifier.animateContentSize(),
        verticalArrangement = Arrangement.Top
    ) {
        AnimatedVisibility(gifAttached.value != null) {
            gifAttached.value?.let { gifAsset ->
                Box {
                    GifImage(
                        modifier = Modifier
                            .zIndex(1f)
                            .scalingClickable(scaleInto = .95f) {
                                navController?.navigate(
                                    NavigationNode.MediaDetail(listOf(gifAsset.original ?: ""))
                                )
                            }
                            .height(MEDIA_MAX_HEIGHT_DP.dp)
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(6.dp)),
                        data = gifAsset.fixedWidthSmall ?: "",
                        contentDescription = gifAsset.description,
                        contentScale = ContentScale.FillHeight
                    )
                    MinimalisticIcon(
                        modifier = Modifier
                            .align(Alignment.TopEnd),
                        imageVector = Icons.Outlined.Close,
                        tint = LocalTheme.current.colors.secondary,
                        onTap = {
                            gifAttached.value = null
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(LocalTheme.current.shapes.betweenItemsSpace))

        replyToMessage.value?.let { originalMessage ->
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            ReplyIndication(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .widthIn(max = MaxModalWidthDp.dp)
                    .fillMaxWidth(),
                data = originalMessage.toAnchorMessage(),
                onClick = {
                    scrollToMessage(originalMessage)
                },
                onRemoveRequest = {
                    replyToMessage.value = null
                },
                isCurrentUser = originalMessage.authorPublicId == viewModel.currentUser.value?.publicId,
                removable = true
            )
        }

        // media preview
        if(mediaAttached.isNotEmpty() || urlsAttached.isNotEmpty()) {
            val mediaListState = rememberScrollState(
                initial = mediaAttached.lastIndex + urlsAttached.lastIndex
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

                Row(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .fillMaxWidth()
                        .requiredHeight(MEDIA_MAX_HEIGHT_DP.dp)
                        .horizontalScroll(state = mediaListState)
                        .horizontallyDraggable(state = mediaListState),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Spacer(Modifier)
                    (mediaAttached + arrayOfNulls(urlsAttached.size)).forEachIndexed { index, media ->
                        Box(
                            modifier = Modifier.fillMaxHeight(),
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

                            MediaElement(
                                modifier = Modifier
                                    .requiredHeight(MEDIA_MAX_HEIGHT_DP.dp)
                                    .wrapContentWidth()
                                    .clip(LocalTheme.current.shapes.rectangularActionShape),
                                media = media,
                                url = urlsAttached.getOrNull(index - mediaAttached.lastIndex),
                                contentScale = ContentScale.FillHeight
                            )
                        }
                    }
                    Spacer(Modifier)
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
            CustomTextField(
                modifier = Modifier
                    .requiredHeight(44.dp)
                    .weight(1f)
                    .padding(start = 12.dp, end = spacing)
                    .onGloballyPositioned {
                        actionYCoordinate.value = it.positionInRoot().y
                    }
                    .contentReceiver { uri ->
                        when(getMediaType((uri.toUri().path ?: uri).substringAfterLast("."))) {
                            MediaType.GIF -> gifAttached.value = GifAsset(singleUrl = uri)
                            else -> urlsAttached.add(uri)
                        }
                    }
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                state = messageState,
                onKeyboardAction = {
                    sendMessage()
                },
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
                                    if(keyboardHeight.value > 99) {
                                        viewModel.setKeyboardHeight(keyboardHeight.value + 1)
                                        coroutineScope.launch {
                                            delay(200)
                                            viewModel.setKeyboardHeight(keyboardHeight.value - 1)
                                        }
                                    }else viewModel.setKeyboardHeight(screenSize.height / 3)

                                    keyboardController?.hide()
                                    keyboardMode.value = ConversationKeyboardMode.Emoji.ordinal
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
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = LocalTheme.current.shapes.componentShape
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
            Crossfade(targetState = isContentEmpty) { isBlank ->
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

        // Emojis + GIFs + Stickers
        if(keyboardMode.value != ConversationKeyboardMode.Default.ordinal) {
            MessageMediaPanel(
                mode = keyboardMode,
                viewModel = viewModel,
                showBackSpace = messageState.text.isNotBlank(),
                onGifSelected = { gif ->
                    gifAttached.value = gif
                },
                onEmojiSelected = { emoji ->
                    showMoreOptions.value = false

                    val newContent = buildString {
                        // before selection
                        append(
                            messageState.text.subSequence(
                                0, messageState.selection.start
                            )
                        )
                        // selection
                        append(emoji)
                        // after selection
                        append(
                            messageState.text.subSequence(
                                messageState.selection.end,
                                messageState.text.length
                            )
                        )
                    }
                    messageState.setTextAndPlaceCursorAtEnd(newContent)
                },
                onBackSpace = {
                    showMoreOptions.value = false

                    val isRangeRemoval = messageState.selection.start != messageState.selection.end

                    if(isRangeRemoval) {
                        messageState.setTextAndPlaceCursorAtEnd(
                            buildString {
                                // before selection
                                append(
                                    messageState.text.subSequence(
                                        startIndex = 0,
                                        endIndex = messageState.selection.start
                                    ).toString()
                                )
                                // after selection
                                append(
                                    messageState.text.subSequence(
                                        startIndex = messageState.selection.end,
                                        endIndex = messageState.text.length
                                    )
                                )
                            }
                        )
                    }else {
                        val modifiedPrefix = removeUnicodeCharacter(
                            text = messageState.text.subSequence(
                                0, messageState.selection.start
                            ).toString(),
                            index = messageState.selection.start
                        )

                        val newContent = buildString {
                            // before selection
                            append(modifiedPrefix)
                            // after selection
                            append(
                                messageState.text.subSequence(
                                    messageState.selection.end,
                                    messageState.text.length
                                )
                            )
                        }
                        messageState.setTextAndPlaceCursorAtEnd(newContent)
                    }
                },
                onDismissRequest = {
                    keyboardMode.value = ConversationKeyboardMode.Default.ordinal
                }
            )
        }
    }

    // Has to be outside of message panel in order to lay over everything else
    if(isContentEmpty) {
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

/** Maximum height of a user attached media */
const val MEDIA_MAX_HEIGHT_DP = 250

// IOS does not support unicode special classes, must be done manually
private val REGEX_GRAPHEME_IOS = """
    (?:[\uD800-\uDBFF][\uDC00-\uDFFF][\uFE0F\u200D\u0300-\u036F\u1AB0-\u1AFF\u1DC0-\u1DFF\u20D0-\u20FF\uFE20-\uFE2F]*|[\u0020-\u007E]|[\u00A0-\uFFFF]|\s|.)[\u0300-\u036F\u1AB0-\u1AFF\u1DC0-\u1DFF\u20D0-\u20FF\uFE20-\uFE2F]*
""".trimIndent()

private val REGEX_GRAPHEME = if(currentPlatform == PlatformType.Native) REGEX_GRAPHEME_IOS else """\X"""
