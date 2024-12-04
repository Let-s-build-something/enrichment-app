package ui.conversation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
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
import augmy.composeapp.generated.resources.logo_pdf
import augmy.composeapp.generated.resources.logo_powerpoint
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_LONG
import augmy.interactive.shared.ui.components.MinimalisticFilledIcon
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.theme.LocalTheme
import base.MediaType
import base.getBitmapFromFile
import base.getMediaType
import base.theme.Colors
import future_shared_module.ext.scalingClickable
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.extension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Horizontal panel for sending and managing a message, and attaching media to it */
@Composable
internal fun SendMessagePanel(
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel
) {
    val screenSize = LocalScreenSize.current
    val spacing = LocalTheme.current.shapes.betweenItemsSpace / 2
    val content = remember {
        mutableStateOf(viewModel.savedMessage)
    }
    val coroutineScope = rememberCoroutineScope()
    val showMoreOptions = rememberSaveable {
        mutableStateOf(content.value.isNotBlank())
    }
    val mediaAttached = remember {
        mutableStateListOf<PlatformFile>()
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


    DisposableEffect(null) {
        onDispose {
            viewModel.savedMessage = content.value
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

            Column {
                Text(
                    modifier = Modifier.padding(
                        bottom = 4.dp,
                        start = LocalTheme.current.shapes.betweenItemsSpace * 2
                    ),
                    text = stringResource(Res.string.conversation_attached),
                    style = LocalTheme.current.styles.regular
                )

                val previewHeight = screenSize.height.div(5).coerceAtMost(MEDIA_MAX_HEIGHT_DP)
                LazyRow(
                    modifier = Modifier
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
                            MinimalisticFilledIcon(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .zIndex(1f),
                                tint = LocalTheme.current.colors.secondary,
                                background = LocalTheme.current.colors.brandMainDark,
                                imageVector = Icons.Outlined.Close,
                                onTap = {
                                    if(index < mediaAttached.size) {
                                        mediaAttached.removeAt(index)
                                    }
                                }
                            )
                            val contentPreviewModifier = Modifier
                                .fillMaxHeight()
                                .background(
                                    color = LocalTheme.current.colors.brandMainDark,
                                    shape = LocalTheme.current.shapes.componentShape
                                )
                                .padding(vertical = 6.dp, horizontal = 8.dp)
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
                //.background(color = LocalTheme.current.colors.backgroundLight)
                .padding(start = 12.dp, end = 10.dp, top = 6.dp)
                .weight(1f, fill = true),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditFieldInput(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = spacing),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                minHeight = 44.dp,
                shape = RoundedCornerShape(LocalTheme.current.shapes.screenCornerRadius),
                paddingValues = PaddingValues(start = 16.dp),
                keyboardActions = KeyboardActions(
                    onSend = {
                        viewModel.sendMessage(
                            content = content.value,
                            mediaFiles = mediaAttached
                        )
                        mediaAttached.clear()
                        content.value = ""
                        viewModel.savedMessage = ""
                    }
                ),
                value = viewModel.savedMessage,
                onValueChange = {
                    content.value = it
                    showMoreOptions.value = false
                }
            )

            Icon(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = LocalTheme.current.colors.brandMainDark,
                        shape = LocalTheme.current.shapes.circularActionShape
                    )
                    .padding(6.dp)
                    .scalingClickable {
                        showMoreOptions.value = !showMoreOptions.value
                    }
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
                            .size(38.dp)
                            .background(
                                color = LocalTheme.current.colors.brandMainDark,
                                shape = LocalTheme.current.shapes.circularActionShape
                            )
                            .padding(6.dp)
                            .scalingClickable {
                                launcherFile.launch()
                            },
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = stringResource(Res.string.accessibility_message_action_file),
                        tint = Color.White
                    )
                    Icon(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = LocalTheme.current.colors.brandMainDark,
                                shape = LocalTheme.current.shapes.circularActionShape
                            )
                            .padding(6.dp)
                            .scalingClickable {
                                launcherImageVideo.launch()
                            },
                        imageVector = Icons.Outlined.Image,
                        contentDescription = stringResource(Res.string.accessibility_message_action_image),
                        tint = Color.White
                    )
                    Icon(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = LocalTheme.current.colors.brandMainDark,
                                shape = LocalTheme.current.shapes.circularActionShape
                            )
                            .padding(6.dp)
                            .scalingClickable {
                                // TODO in-app audio recorder
                            },
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = stringResource(Res.string.accessibility_message_action_audio),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/** Maximum amount of files, images, and videos to be selected and sent within a singular message */
private const val MAX_ITEMS_SELECTED = 20

private const val MEDIA_MAX_HEIGHT_DP = 250
